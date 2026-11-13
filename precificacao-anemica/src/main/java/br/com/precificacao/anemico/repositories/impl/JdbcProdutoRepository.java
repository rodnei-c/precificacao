package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Executa as operações SQL de produtos, sem cálculos ou validações de negócio.
 */
@Repository
public class JdbcProdutoRepository implements ProdutoRepository {

  private static final String COLUNAS =
    "codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo";
  private static final String CONSULTA_POR_CODIGO = "SELECT " + COLUNAS + " FROM produto WHERE codigo = ?";

  private final JdbcTemplate jdbcTemplate;

  /**
   * @param jdbcTemplate acesso à conexão e aos comandos parametrizados
   */
  public JdbcProdutoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Insere o produto ou atualiza o registro identificado pelo código.
   *
   * @param produto dados já validados pelo serviço
   * @return registro persistido com código gerado, quando novo
   */
  @Override
  public Produto salvar(Produto produto) {
    if (produto.getCodigo() == null) {
      return jdbcTemplate.queryForObject(
        "INSERT INTO produto (nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo)"
          + " VALUES (?, ?, ?, ?, ?, ?) RETURNING " + COLUNAS,
        (resultado, linha) -> mapear(resultado), produto.getNome(), produto.getCusto(), produto.getPrecoVenda(),
        produto.getPercentual(), produto.getModalidadePrecificacao().name(), produto.getCodigoCategoria()
      );
    }
    return jdbcTemplate.queryForObject(
      "UPDATE produto SET nome = ?, custo = ?, preco_venda = ?, percentual = ?,"
        + " modalidade_precificacao = ?, categoria_codigo = ? WHERE codigo = ? RETURNING " + COLUNAS,
      (resultado, linha) -> mapear(resultado), produto.getNome(), produto.getCusto(), produto.getPrecoVenda(),
      produto.getPercentual(), produto.getModalidadePrecificacao().name(), produto.getCodigoCategoria(),
      produto.getCodigo()
    );
  }

  /**
   * @param codigo identificação procurada
   * @return registro encontrado ou resultado vazio
   */
  @Override
  public Optional<Produto> buscarPorCodigo(Long codigo) {
    return jdbcTemplate.query(
      CONSULTA_POR_CODIGO, (resultado, linha) -> mapear(resultado), codigo
    ).stream().findFirst();
  }

  @Override
  public Optional<Produto> buscarPorCodigoParaAtualizacao(Long codigo) {
    return jdbcTemplate.query(
      CONSULTA_POR_CODIGO + " FOR UPDATE", (resultado, linha) -> mapear(resultado), codigo
    ).stream().findFirst();
  }

  /**
   * @return produtos ordenados pelo código
   */
  @Override
  public List<Produto> listar(String nome, ModalidadePrecificacao modalidade, Long categoria) {
    StringBuilder sql = new StringBuilder("SELECT ").append(COLUNAS).append(" FROM produto WHERE TRUE");
    List<Object> parametros = new ArrayList<>();
    if (nome != null) {
      sql.append(" AND STRPOS(LOWER(nome), LOWER(?)) > 0");
      parametros.add(nome);
    }
    if (modalidade != null) {
      sql.append(" AND modalidade_precificacao = ?");
      parametros.add(modalidade.name());
    }
    if (categoria != null) {
      sql.append(" AND categoria_codigo = ?");
      parametros.add(categoria);
    }
    sql.append(" ORDER BY codigo");
    return jdbcTemplate.query(sql.toString(), (resultado, linha) -> mapear(resultado), parametros.toArray());
  }

  static Produto mapear(ResultSet resultado) throws SQLException {
    Produto produto = new Produto(
      resultado.getLong("codigo"), resultado.getString("nome"),
      resultado.getBigDecimal("custo"), resultado.getBigDecimal("preco_venda"),
      resultado.getBigDecimal("percentual"),
      ModalidadePrecificacao.valueOf(resultado.getString("modalidade_precificacao"))
    );
    produto.setCodigoCategoria(resultado.getObject("categoria_codigo", Long.class));
    return produto;
  }
}
