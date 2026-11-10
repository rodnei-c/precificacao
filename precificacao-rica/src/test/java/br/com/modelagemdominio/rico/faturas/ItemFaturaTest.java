package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.produtos.precificacao.AcrescimoSobreCusto;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.Precificacoes;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemFaturaTest {

  @Test
  void constroiItemSemConsultarOBanco() {
    ItemFatura item = assertDoesNotThrow(() -> new ItemFatura(
      new PgProdutosConstantes(new DataSourceSemConexao()), 42, new Quantidade(BigDecimal.ONE)
    ));

    assertEquals(42, item.codigoProduto());
    assertEquals(BigDecimal.ONE, item.quantidade().valor());
  }

  @Test
  void mantemPrecisaoDoSubtotalComQuantidadeFracionaria() throws Exception {
    ProdutosDeTeste produtos = new ProdutosDeTeste();
    Produto produto = produtos.cadastrar(new NomeProduto("Produto"), precificacao("1.23"));
    ItemFatura item = new ItemFatura(
      produtos, produto.codigo(), new Quantidade(new BigDecimal("0.1234"))
    );

    assertEquals(new BigDecimal("0.151782"), item.subtotal());
  }

  @Test
  void consultaNovamenteOPrecoMesmoQuandoAColecaoDevolveObjetosImutaveis() throws Exception {
    ProdutosDeTeste produtos = new ProdutosDeTeste();
    Produto produto = produtos.cadastrar(new NomeProduto("Produto"), precificacao("120.00"));
    ItemFatura item = new ItemFatura(
      produtos, produto.codigo(), new Quantidade(new BigDecimal("1.5"))
    );

    assertEquals(new BigDecimal("180.000"), item.subtotal());
    produtos.alterarPrecificacao(produto.codigo(), valoresParaPreco("80.00"));
    assertEquals(new BigDecimal("120.000"), item.subtotal());
    produtos.alterarPrecificacao(produto.codigo(), valoresParaPreco("140.00"));
    assertEquals(new BigDecimal("210.000"), item.subtotal());
  }

  @Test
  void propagaFalhaQuandoOProdutoNaoExiste() {
    ItemFatura item = new ItemFatura(
      new ProdutosDeTeste(), 1, new Quantidade(BigDecimal.ONE)
    );

    FalhaProduto falha = assertThrows(FalhaProduto.class, item::subtotal);

    assertEquals(FalhaProduto.Motivo.PRODUTO_AUSENTE, falha.motivo());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void rejeitaCodigoNaoPositivo(long codigo) {
    assertThrows(FaturaInvalida.class,
      () -> new ItemFatura(new ProdutosDeTeste(), codigo, new Quantidade(BigDecimal.ONE)));
  }

  @Test
  void rejeitaColecaoOuQuantidadeAusente() {
    assertThrows(FaturaInvalida.class,
      () -> new ItemFatura(null, 1, new Quantidade(BigDecimal.ONE)));
    assertThrows(FaturaInvalida.class, () -> new ItemFatura(new ProdutosDeTeste(), 1, null));
  }

  private PrecificacaoParaCadastro precificacao(String venda) throws PrecificacaoInvalida {
    return new Precificacoes(new AcrescimoSobreCusto())
      .comCustoEVenda(BigDecimal.ONE, new BigDecimal(venda));
  }

  private ValoresParaPrecificar valoresParaPreco(String venda) {
    return new ValoresParaPrecificar(BigDecimal.ONE, new BigDecimal(venda), null);
  }

  private static final class ProdutosDeTeste implements Produtos {

    private final Map<Long, ProdutoDeTeste> registros = new HashMap<>();

    @Override
    public Produto produto(long codigo) throws FalhaProduto {
      Produto produto = this.registros.get(codigo);
      if (produto == null) {
        throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
      }
      return produto;
    }

    @Override
    public List<Produto> listar(FiltrosProdutos filtros) {
      return this.registros.values().stream()
        .filter(produto -> filtros.nome().map(nome -> produto.nome().toLowerCase(Locale.ROOT)
          .contains(nome.toLowerCase(Locale.ROOT))).orElse(true))
        .filter(produto -> filtros.modalidade().map(produto.precificacao().modalidade()::equals).orElse(true))
        .filter(produto -> filtros.categoria().map(categoria -> produto.codigoCategoria().equals(Optional.of(categoria)))
          .orElse(true))
        .sorted(Comparator.comparingLong(Produto::codigo))
        .map(Produto.class::cast)
        .toList();
    }

    @Override
    public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao,
                             Optional<CodigoCategoria> codigoCategoria) {
      long codigo = this.registros.size() + 1L;
      ProdutoDeTeste produto = new ProdutoDeTeste(codigo, nome.valor(), precificacao.precificacao(), codigoCategoria);
      this.registros.put(codigo, produto);
      return produto;
    }

    @Override
    public Produto vincularCategoria(long codigo, CodigoCategoria codigoCategoria) throws FalhaProduto {
      Produto anterior = produto(codigo);
      ProdutoDeTeste vinculado = new ProdutoDeTeste(
        codigo, anterior.nome(), anterior.precificacao(), Optional.of(codigoCategoria)
      );
      this.registros.put(codigo, vinculado);
      return vinculado;
    }

    @Override
    public Produto alterarPrecificacao(long codigo, ValoresParaPrecificar valores)
        throws FalhaProduto, PrecificacaoInvalida {
      Produto anterior = produto(codigo);
      Precificacao precificacao = valores.precificacao(anterior.precificacao().modalidade()).precificacao();
      var atualizado = new ProdutoDeTeste(codigo, anterior.nome(), precificacao, anterior.codigoCategoria());
      this.registros.put(codigo, atualizado);
      return atualizado;
    }
  }

  private record ProdutoDeTeste(long codigo, String nome, Precificacao precificacao,
                                Optional<CodigoCategoria> codigoCategoria) implements Produto {
  }
}
