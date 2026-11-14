package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoFixo;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoPercentual;
import br.com.modelagemdominio.rico.faturas.descontos.SemDesconto;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.produtos.precificacao.AcrescimoSobreCusto;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.Precificacoes;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculoFaturaTest {

  @Test
  void constroiSemConsultarProdutosEProtegeComposicao() {
    var item = item(new PgProdutosConstantes(new DataSourceSemConexao()), 1, "1");
    var recebidos = new ArrayList<>(List.of(item));
    var calculo = assertDoesNotThrow(() -> new CalculoFatura(recebidos, new SemDesconto()));

    recebidos.clear();
    assertEquals(List.of(item), calculo.itens());
    assertThrows(UnsupportedOperationException.class, () -> calculo.itens().clear());
  }

  @Test
  void rejeitaComposicaoInvalidaAntesDeConsultarProdutos() {
    var produtos = new PgProdutosConstantes(new DataSourceSemConexao());
    var item = item(produtos, 1, "1");
    assertThrows(FaturaInvalida.class, () -> new CalculoFatura(null, new SemDesconto()));
    assertThrows(FaturaInvalida.class, () -> new CalculoFatura(List.of(), new SemDesconto()));
    assertThrows(FaturaInvalida.class, () -> new CalculoFatura(Arrays.asList(item, null), new SemDesconto()));
    assertThrows(FaturaInvalida.class, () -> new CalculoFatura(List.of(item), null));
    assertThrows(FaturaInvalida.class, () -> new CalculoFatura(
      List.of(item, item(produtos, 1, "0.5")), new SemDesconto()));
  }

  @Test
  void mantemPrecisaoDoItemEDoDescontoAteOTotalFinal() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "1.23");
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "0.1234")),
      new DescontoPercentual(new BigDecimal("12.3456")));

    ValoresFatura valores = calculo.preparar();

    assertEquals(new BigDecimal("0.151782"), valores.subtotal());
    assertEquals(new BigDecimal("0.018738398592"), valores.descontoAplicado());
    assertEquals(new BigDecimal("0.13"), valores.total());
    assertEquals(new BigDecimal("1.23"), valores.itens().get(0).precoVenda());
    assertEquals(new BigDecimal("0.151782"), valores.itens().get(0).subtotal());
    assertEquals(1, produtos.consultas);
    assertThrows(UnsupportedOperationException.class, () -> valores.itens().clear());

    produtos.preco(1, "2.00");
    assertEquals(new BigDecimal("0.13"), valores.total());
    assertEquals(1, produtos.consultas, "Acessar o resultado não consulta novamente o produto.");
    assertEquals(new BigDecimal("0.246800"), calculo.calcular().subtotal());
    assertEquals(2, produtos.consultas);
  }

  @Test
  void somaTodosOsItensAntesDeArredondar() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "1.01");
    produtos.preco(2, "1.05");
    var calculo = new CalculoFatura(List.of(item(produtos, 2, "0.5"), item(produtos, 1, "0.5")),
      new SemDesconto());

    ValoresFatura valores = calculo.preparar();

    assertEquals(new BigDecimal("1.030"), valores.subtotal());
    assertEquals(new BigDecimal("1.03"), valores.total());
    assertEquals(List.of(2L, 1L), valores.itens().stream().map(ValoresItemFatura::codigoProduto).toList());
    assertEquals(2, produtos.consultas);
  }

  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
    "PIX;0;90.00",
    "DINHEIRO;0;90.00",
    "CARTAO_DEBITO;0;90.00",
    "BOLETO;5.00;95.00",
    "CARTAO_CREDITO;1.80;91.80"
  })
  void aplicaTaxaDaFormaDePagamentoSobreOSaldo(
    FormaPagamento formaPagamento, BigDecimal taxaEsperada, BigDecimal totalEsperado
  ) throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "100.00");
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "1")),
      new DescontoFixo(new BigDecimal("10.00")));

    ValoresFatura valores = calculo.preparar(formaPagamento.taxa());

    assertEquals(new BigDecimal("100.00"), valores.subtotal());
    assertEquals(new BigDecimal("10.00"), valores.descontoAplicado());
    assertEquals(0, taxaEsperada.compareTo(valores.taxaPagamento()));
    assertEquals(totalEsperado, valores.total());
  }

  @ParameterizedTest
  @EnumSource(value = FormaPagamento.class, names = {"BOLETO", "CARTAO_CREDITO"})
  void zeraTaxaETotalQuandoODescontoConsomeTodoOSubtotal(FormaPagamento formaPagamento) throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "100.00");
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "1")),
      new DescontoFixo(new BigDecimal("100.00")));

    ValoresFatura valores = calculo.preparar(formaPagamento.taxa());

    assertEquals(BigDecimal.ZERO, valores.taxaPagamento());
    assertEquals(new BigDecimal("0.00"), valores.total());
  }

  @Test
  void mantemTotalSemTaxaForaDaSimulacao() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "100.00");
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "1")),
      new DescontoFixo(new BigDecimal("10.00")));

    ValoresFatura valores = calculo.preparar();

    assertEquals(BigDecimal.ZERO, valores.taxaPagamento());
    assertEquals(new BigDecimal("90.00"), valores.total());
  }

  @ParameterizedTest
  @CsvSource({"1.01, 0.50", "1.03, 0.52"})
  void aplicaArredondamentoParSomenteNoTotal(String preco, BigDecimal total) throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, preco);

    assertEquals(total, new CalculoFatura(List.of(item(produtos, 1, "0.5")), new SemDesconto())
      .preparar().total());
  }

  @Test
  void comparaDescontoFixoComSubtotalExatoNaPreparacao() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "1.23");
    var itens = List.of(item(produtos, 1, "0.1234"));
    var excedente = new CalculoFatura(itens, new DescontoFixo(new BigDecimal("0.16")));
    var permitido = new CalculoFatura(itens, new DescontoFixo(new BigDecimal("0.15")));

    assertThrows(FaturaInvalida.class, excedente::preparar);
    assertEquals(new BigDecimal("0.00"), permitido.preparar().total());
    assertEquals(new BigDecimal("0.00"), excedente.calcular().total(),
      "A consulta posterior limita o abatimento; a preparação rejeita excesso inicial.");
  }

  @Test
  void recalculaDescontoFixoSemPerderOriginalOuModificarLeituraAnterior() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "100.00");
    var desconto = new DescontoFixo(new BigDecimal("30.00"));
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "1")), desconto);
    ValoresFatura original = calculo.preparar();

    produtos.preco(1, "12.34");
    ValoresFatura reduzidos = calculo.calcular();
    assertEquals(new BigDecimal("12.34"), reduzidos.descontoAplicado());
    assertEquals(new BigDecimal("0.00"), reduzidos.total());
    produtos.preco(1, "50.00");
    ValoresFatura recuperados = calculo.calcular();
    assertEquals(new BigDecimal("30.00"), recuperados.descontoAplicado());
    assertEquals(new BigDecimal("20.00"), recuperados.total());
    assertEquals(new BigDecimal("70.00"), original.total());
    assertEquals(new BigDecimal("30.00"), desconto.registro().orElseThrow().valor());
    assertEquals(3, produtos.consultas);
  }

  @Test
  void aceitaTotalZeroComDescontoIntegral() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "1.23");
    var itens = List.of(item(produtos, 1, "2"));

    assertEquals(new BigDecimal("0.00"),
      new CalculoFatura(itens, new DescontoPercentual(new BigDecimal("100"))).preparar().total());
    assertEquals(new BigDecimal("0.00"),
      new CalculoFatura(itens, new DescontoFixo(new BigDecimal("2.46"))).preparar().total());
  }

  @ParameterizedTest
  @EnumSource(value = FalhaProduto.Motivo.class, names = {"PRODUTO_AUSENTE", "PERSISTENCIA_INDISPONIVEL"})
  void distingueProdutoAusenteDeFalhaDeLeituraEPreservaCausa(FalhaProduto.Motivo motivo) {
    var produtos = new ProdutosDeTeste();
    produtos.falha = new FalhaProduto(motivo);
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "1")), new SemDesconto());

    FalhaFatura falha = assertThrows(FalhaFatura.class, calculo::preparar);

    assertEquals(motivo == FalhaProduto.Motivo.PRODUTO_AUSENTE
      ? FalhaFatura.Motivo.PRODUTO_AUSENTE : FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha.motivo());
    assertSame(produtos.falha, falha.getCause());
  }

  @Test
  void contratoDaFaturaCompoeCalculoEVinculosOpcionais() throws Exception {
    var produtos = new ProdutosDeTeste();
    produtos.preco(1, "1.23");
    var calculo = new CalculoFatura(List.of(item(produtos, 1, "2")), new SemDesconto());
    Fatura fatura = new FaturaDeTeste(42, LocalDateTime.of(2026, 11, 10, 12, 0), calculo,
      new VinculosFatura(Optional.of(7L), Optional.of(FormaPagamento.PIX)));

    assertEquals(new BigDecimal("2.46"), fatura.subtotal());
    assertEquals(BigDecimal.ZERO, fatura.descontoAplicado());
    assertEquals(new BigDecimal("2.46"), fatura.total());
    assertEquals(Optional.of(7L), fatura.vinculos().codigoCliente());
    assertEquals(Optional.of(FormaPagamento.PIX), fatura.vinculos().formaPagamento());
    assertTrue(new VinculosFatura().codigoCliente().isEmpty());
    assertTrue(new VinculosFatura().formaPagamento().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void rejeitaVinculoComCodigoInvalido(long codigo) {
    assertThrows(FaturaInvalida.class,
      () -> new VinculosFatura(Optional.of(codigo), Optional.of(FormaPagamento.BOLETO)));
  }

  private static ItemFatura item(Produtos produtos, long codigo, String quantidade) {
    return new ItemFatura(produtos, codigo, new Quantidade(new BigDecimal(quantidade)));
  }

  private record FaturaDeTeste(long codigo, LocalDateTime dataCriacao, CalculoFatura calculo, VinculosFatura vinculos)
    implements Fatura {

    @Override
    public List<ItemFatura> itens() {
      return this.calculo.itens();
    }

    @Override
    public Desconto desconto() {
      return this.calculo.desconto();
    }
  }

  private static final class ProdutosDeTeste implements Produtos {

    private final Map<Long, Produto> registros = new HashMap<>();
    private int consultas;
    private FalhaProduto falha;

    void preco(long codigo, String preco) throws Exception {
      Precificacao precificacao = new Precificacoes(new AcrescimoSobreCusto())
        .comCustoEVenda(new BigDecimal("0.01"), new BigDecimal(preco)).precificacao();
      this.registros.put(codigo, new ProdutoDeTeste(codigo, precificacao));
    }

    @Override
    public Produto produto(long codigo) throws FalhaProduto {
      this.consultas++;
      if (this.falha != null) {
        throw this.falha;
      }
      Produto produto = this.registros.get(codigo);
      if (produto == null) {
        throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
      }
      return produto;
    }

    @Override
    public List<Produto> listar(FiltrosProdutos filtros) {
      throw new AssertionError("O cálculo deve consultar somente os produtos dos itens.");
    }

    @Override
    public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao, Optional<CodigoCategoria> categoria) {
      throw new AssertionError("O cálculo não deve cadastrar produtos.");
    }

    @Override
    public Produto vincularCategoria(long codigo, CodigoCategoria categoria) {
      throw new AssertionError("O cálculo não deve alterar a categoria.");
    }

    @Override
    public Produto alterarPrecificacao(long codigo, ValoresParaPrecificar valores) {
      throw new AssertionError("O cálculo não deve alterar os preços.");
    }
  }

  private record ProdutoDeTeste(long codigo, Precificacao precificacao) implements Produto {

    @Override
    public String nome() {
      return "Produto " + this.codigo;
    }

    @Override
    public Optional<CodigoCategoria> codigoCategoria() {
      return Optional.empty();
    }
  }
}
