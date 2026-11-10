package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongFunction;
import javax.sql.DataSource;

public final class PgProdutosConstantes implements Produtos, ProdutosNaConexao {

  private final DataSource banco;
  private final LongFunction<Produto> produtoPorCodigo;
  private final Produtos origem;

  public PgProdutosConstantes(DataSource banco) {
    this(banco, codigo -> new PgProduto(banco, codigo));
  }

  public PgProdutosConstantes(DataSource banco, LongFunction<Produto> produtoPorCodigo) {
    this(banco, produtoPorCodigo, new PgProdutos(banco, produtoPorCodigo));
  }

  public PgProdutosConstantes(DataSource banco, LongFunction<Produto> produtoPorCodigo, Produtos origem) {
    this.banco = banco;
    this.origem = origem;
    this.produtoPorCodigo = produtoPorCodigo;
  }

  @Override
  public Produto produto(long codigo) throws FalhaProduto {
    if (codigo <= 0) {
      throw new FalhaProduto(FalhaProduto.Motivo.CODIGO_INVALIDO);
    }
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement("""
        SELECT codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
        FROM public.produto WHERE codigo = ?
        """)) {
      consulta.setLong(1, codigo);
      try (ResultSet resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
      }
    } catch (SQLException falha) {
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  /**
   * Carrega e protege os produtos na transação do chamador, sem fechar sua conexão.
   *
   * @param conexao conexão da operação composta
   * @param codigos códigos positivos e distintos dos itens
   * @return produtos materializados em uma única consulta
   * @throws SQLException quando a consulta falhar
   * @throws FalhaProduto quando algum produto não existir
   */
  @Override
  public Map<Long, Produto> produtos(Connection conexao, List<Long> codigos) throws SQLException, FalhaProduto {
    return this.produtos(conexao, codigos, true);
  }

  /**
   * @param conexao conexão da leitura, pertencente ao chamador
   * @param codigos códigos positivos e distintos dos itens
   * @return produtos materializados em conjunto, sem bloquear linhas
   * @throws SQLException quando a consulta falhar
   * @throws FalhaProduto quando algum produto não existir
   */
  @Override
  public Map<Long, Produto> produtosParaConsulta(Connection conexao, List<Long> codigos)
    throws SQLException, FalhaProduto {
    return this.produtos(conexao, codigos, false);
  }

  private Map<Long, Produto> produtos(Connection conexao, List<Long> codigos, boolean proteger)
    throws SQLException, FalhaProduto {
    String bloqueio = proteger ? " FOR SHARE" : "";
    var parametros = conexao.createArrayOf("bigint", codigos.toArray(Long[]::new));
    try (var consulta = conexao.prepareStatement("""
      SELECT codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
      FROM public.produto WHERE codigo = ANY (?) ORDER BY codigo
      """ + bloqueio)) {
      consulta.setArray(1, parametros);
      var produtos = new LinkedHashMap<Long, Produto>();
      try (var resultado = consulta.executeQuery()) {
        while (resultado.next()) {
          Produto produto = this.daLinha(resultado);
          produtos.put(produto.codigo(), produto);
        }
      }
      if (produtos.size() != codigos.size()) {
        throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
      }
      return Map.copyOf(produtos);
    } finally {
      parametros.free();
    }
  }

  /**
   * @param resultado linha com as colunas do produto, pertencente ao chamador
   * @return produto materializado, sem novas consultas
   * @throws SQLException quando uma coluna não puder ser lida
   */
  public Produto daLinha(ResultSet resultado) throws SQLException {
    return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
  }

  @Override
  public List<Produto> listar(FiltrosProdutos filtros) throws FalhaProduto {
    return new ConsultaSqlDeProdutos(this.banco).listar(
      "codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo",
      filtros, resultado -> new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto()
    );
  }

  @Override
  public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao,
              Optional<CodigoCategoria> codigoCategoria) throws FalhaProduto {
    return this.origem.cadastrar(nome, precificacao, codigoCategoria);
  }

  @Override
  public Produto vincularCategoria(long codigo, CodigoCategoria codigoCategoria) throws FalhaProduto {
    return this.origem.vincularCategoria(codigo, codigoCategoria);
  }

  @Override
  public Produto alterarPrecificacao(long codigo, ValoresParaPrecificar valores)
    throws FalhaProduto, PrecificacaoInvalida {
    return this.origem.alterarPrecificacao(codigo, valores);
  }
}
