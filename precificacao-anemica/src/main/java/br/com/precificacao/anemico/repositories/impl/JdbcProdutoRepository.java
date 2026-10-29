package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    "codigo, nome, custo, preco_venda, percentual, modalidade_precificacao";

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
        "INSERT INTO produto (nome, custo, preco_venda, percentual, modalidade_precificacao)"
          + " VALUES (?, ?, ?, ?, ?) RETURNING " + COLUNAS,
        this::mapear, produto.getNome(), produto.getCusto(), produto.getPrecoVenda(),
        produto.getPercentual(), produto.getModalidadePrecificacao().name()
      );
    }
    return jdbcTemplate.queryForObject(
      "UPDATE produto SET nome = ?, custo = ?, preco_venda = ?, percentual = ?,"
        + " modalidade_precificacao = ? WHERE codigo = ? RETURNING " + COLUNAS,
      this::mapear, produto.getNome(), produto.getCusto(), produto.getPrecoVenda(),
      produto.getPercentual(), produto.getModalidadePrecificacao().name(), produto.getCodigo()
    );
  }

  /**
   * @param codigo identificação procurada
   * @return registro encontrado ou resultado vazio
   */
  @Override
  public Optional<Produto> buscarPorCodigo(Long codigo) {
    return jdbcTemplate.query(
      "SELECT " + COLUNAS + " FROM produto WHERE codigo = ?", this::mapear, codigo
    ).stream().findFirst();
  }

  /**
   * @return produtos ordenados pelo código
   */
  @Override
  public List<Produto> listar() {
    return jdbcTemplate.query("SELECT " + COLUNAS + " FROM produto ORDER BY codigo", this::mapear);
  }

  private Produto mapear(ResultSet resultado, int linha) throws SQLException {
    return new Produto(
      resultado.getLong("codigo"), resultado.getString("nome"),
      resultado.getBigDecimal("custo"), resultado.getBigDecimal("preco_venda"),
      resultado.getBigDecimal("percentual"),
      ModalidadePrecificacao.valueOf(resultado.getString("modalidade_precificacao"))
    );
  }
}
