package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroFaturaRequisicao;
import br.com.precificacao.anemico.enums.FormaPagamento;
import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.enums.TipoDesconto;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Fatura;
import br.com.precificacao.anemico.models.ItemFatura;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import br.com.precificacao.anemico.repositories.FaturaRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.impl.FaturaServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Confere precisão, arredondamento e validações das faturas antes da gravação. */
class FaturaServiceTest {

  private final FaturaRepository faturas = mock(FaturaRepository.class);
  private final ProdutoRepository produtos = mock(ProdutoRepository.class);
  private final FaturaService servico = new FaturaServiceImpl(
    faturas, produtos, mock(ClienteRepository.class)
  );

  @Test
  void somaOsItensSemArredondarSeusSubtotais() {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.01")));
    when(produtos.buscarPorCodigo(2L)).thenReturn(Optional.of(produto(2L, "1.05")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "0.5"), item(2L, "0.5")), null, null, null
    );

    var valores = servico.simular(requisicao);

    conferirValor("0.505", valores.itens().get(0).subtotal());
    conferirValor("0.525", valores.itens().get(1).subtotal());
    conferirValor("1.030", valores.subtotal());
    assertEquals(new BigDecimal("1.03"), valores.total());
  }

  @Test
  void mantemPrecisaoDaQuantidadeEDoDescontoPercentualComQuatroCasas() {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.23")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "0.1234")),
      new CadastroFaturaRequisicao.Desconto(TipoDesconto.PERCENTUAL, new BigDecimal("12.3456")),
      null, null
    );

    var valores = servico.simular(requisicao);

    conferirValor("0.151782", valores.itens().get(0).subtotal());
    conferirValor("0.151782", valores.subtotal());
    conferirValor("0.018738398592", valores.descontoAplicado());
    assertEquals(new BigDecimal("0.13"), valores.total());
  }

  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
    "PIX;0;90.00",
    "DINHEIRO;0;90.00",
    "CARTAO_DEBITO;0;90.00",
    "BOLETO;5.00;95.00",
    "CARTAO_CREDITO;1.8000;91.80"
  })
  void aplicaTaxaDaFormaDePagamentoNaSimulacao(
    FormaPagamento formaPagamento, String taxaEsperada, String totalEsperado
  ) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "100.00")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "1")),
      new CadastroFaturaRequisicao.Desconto(TipoDesconto.VALOR_FIXO, new BigDecimal("10.00")),
      null, formaPagamento
    );

    var valores = servico.simular(requisicao);

    conferirValor("100.00", valores.subtotal());
    conferirValor("10.00", valores.descontoAplicado());
    conferirValor(taxaEsperada, valores.taxaPagamento());
    assertEquals(new BigDecimal(totalEsperado), valores.total());
  }

  @Test
  void naoAplicaTaxaQuandoAFormaDePagamentoNaoEInformada() {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "100.00")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "1")),
      new CadastroFaturaRequisicao.Desconto(TipoDesconto.VALOR_FIXO, new BigDecimal("10.00")),
      null, null
    );

    var valores = servico.simular(requisicao);

    conferirValor("0", valores.taxaPagamento());
    assertEquals(new BigDecimal("90.00"), valores.total());
  }

  @ParameterizedTest
  @CsvSource({"BOLETO", "CARTAO_CREDITO"})
  void zeraTaxaETotalQuandoODescontoConsomeTodoOSubtotal(FormaPagamento formaPagamento) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "100.00")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "1")),
      new CadastroFaturaRequisicao.Desconto(TipoDesconto.VALOR_FIXO, new BigDecimal("100.00")),
      null, formaPagamento
    );

    var valores = servico.simular(requisicao);

    conferirValor("0", valores.taxaPagamento());
    assertEquals(new BigDecimal("0.00"), valores.total());
  }

  @ParameterizedTest
  @CsvSource({
    "0.4999, 0.504899, 0.50",
    "0.5000, 0.505000, 0.50",
    "0.5001, 0.505101, 0.51",
    "1.5000, 1.515000, 1.52"
  })
  void arredondaSomenteOTotalPeloCriterioPar(
    String quantidade, String subtotalEsperado, String totalEsperado
  ) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.01")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, quantidade)), null, null, null
    );

    var valores = servico.simular(requisicao);

    conferirValor(subtotalEsperado, valores.subtotal());
    assertEquals(new BigDecimal(totalEsperado), valores.total());
  }

  @Test
  void preservaPrecisaoAoSubtrairELimitarDescontoFixo() {
    Fatura original = faturaComDescontoFixo("1.01");
    Fatura reduzida = faturaComDescontoFixo("0.01");
    when(faturas.buscarPorId(1L)).thenReturn(Optional.of(original)).thenReturn(Optional.of(reduzida));

    var valoresOriginais = servico.buscarPorCodigo(1L);
    var valoresReduzidos = servico.buscarPorCodigo(1L);

    conferirValor("0.505", valoresOriginais.subtotal());
    conferirValor("0.01", valoresOriginais.descontoAplicado());
    assertEquals(new BigDecimal("0.50"), valoresOriginais.total());
    conferirValor("0.005", valoresReduzidos.subtotal());
    conferirValor("0.005", valoresReduzidos.descontoAplicado());
    assertEquals(new BigDecimal("0.00"), valoresReduzidos.total());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "-0.0001", "0.00001", "1000000000000000"})
  void rejeitaQuantidadeInvalidaAntesDeTentarGravar(String quantidade) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.23")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, quantidade)), null, null, null
    );

    assertThrows(ValidacaoException.class, () -> servico.cadastrar(requisicao));

    verifyNoInteractions(faturas);
  }

  @ParameterizedTest
  @CsvSource({
    "0.0001, 0.000123, 0.00",
    "999999999999999.9999, 1229999999999999.999877, 1230000000000000.00",
    "1.2345000, 1.518435, 1.52"
  })
  void aceitaLimitesDaQuantidadeEZerosExcedentes(
    String quantidade, String subtotalEsperado, String totalEsperado
  ) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.23")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, quantidade)), null, null, null
    );

    var valores = servico.simular(requisicao);

    conferirValor(subtotalEsperado, valores.subtotal());
    assertEquals(new BigDecimal(totalEsperado), valores.total());
  }

  @ParameterizedTest
  @CsvSource(delimiter = ';', nullValues = "-", value = {
    "-;0.10",
    "PERCENTUAL;-",
    "PERCENTUAL;-0.0001",
    "PERCENTUAL;100.0001",
    "PERCENTUAL;0.00001",
    "VALOR_FIXO;-0.01",
    "VALOR_FIXO;1.24",
    "VALOR_FIXO;1.001"
  })
  void rejeitaDescontoInvalidoAntesDeTentarGravar(TipoDesconto tipo, BigDecimal valor) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.23")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "1")), new CadastroFaturaRequisicao.Desconto(tipo, valor), null, null
    );

    assertThrows(ValidacaoException.class, () -> servico.cadastrar(requisicao));

    verifyNoInteractions(faturas);
  }

  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
    "PERCENTUAL;0;1.23",
    "PERCENTUAL;100.00000;0.00",
    "VALOR_FIXO;0;1.23",
    "VALOR_FIXO;1.230;0.00"
  })
  void aceitaDescontosNosLimitesIncluindoZerosExcedentes(
    TipoDesconto tipo, BigDecimal valor, String totalEsperado
  ) {
    when(produtos.buscarPorCodigo(1L)).thenReturn(Optional.of(produto(1L, "1.23")));
    var requisicao = new CadastroFaturaRequisicao(
      List.of(item(1L, "1")), new CadastroFaturaRequisicao.Desconto(tipo, valor), null, null
    );

    var valores = servico.simular(requisicao);

    conferirValor("1.23", valores.subtotal());
    assertEquals(new BigDecimal(totalEsperado), valores.total());
  }

  private Produto produto(long codigo, String preco) {
    BigDecimal valor = new BigDecimal(preco);
    return new Produto(codigo, "Produto de teste", valor, valor, BigDecimal.ZERO,
      ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO);
  }

  private CadastroFaturaRequisicao.Item item(long codigo, String quantidade) {
    return new CadastroFaturaRequisicao.Item(
      BigDecimal.valueOf(codigo), quantidade == null ? null : new BigDecimal(quantidade)
    );
  }

  private Fatura faturaComDescontoFixo(String preco) {
    return new Fatura(1L, LocalDateTime.of(2026, 1, 1, 12, 0),
      List.of(new ItemFatura(produto(1L, preco), new BigDecimal("0.5"))),
      TipoDesconto.VALOR_FIXO, new BigDecimal("0.01"), null, null);
  }

  private void conferirValor(String esperado, BigDecimal atual) {
    assertEquals(0, new BigDecimal(esperado).compareTo(atual),
      () -> "Valor esperado: " + esperado + "; valor obtido: " + atual);
  }
}
