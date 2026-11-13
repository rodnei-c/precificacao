package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.models.AlteracaoPrecificacao;
import br.com.precificacao.anemico.repositories.HistoricoPrecificacaoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcHistoricoPrecificacaoRepository implements HistoricoPrecificacaoRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcHistoricoPrecificacaoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void registrar(Long codigoProduto, AlteracaoPrecificacao.Valores anteriores,
                        AlteracaoPrecificacao.Valores novos) {
    jdbcTemplate.update("""
      INSERT INTO historico_precificacao (
        produto_codigo, custo_anterior, preco_venda_anterior, percentual_anterior,
        custo_novo, preco_venda_novo, percentual_novo
      ) VALUES (?, ?, ?, ?, ?, ?, ?)
      """,
      codigoProduto, anteriores.custo(), anteriores.precoVenda(), anteriores.percentual(),
      novos.custo(), novos.precoVenda(), novos.percentual()
    );
  }

  @Override
  public List<AlteracaoPrecificacao> listarPorProduto(Long codigoProduto) {
    return jdbcTemplate.query("""
      SELECT codigo, produto_codigo, data_alteracao, custo_anterior, preco_venda_anterior,
             percentual_anterior, custo_novo, preco_venda_novo, percentual_novo
      FROM historico_precificacao
      WHERE produto_codigo = ?
      ORDER BY codigo DESC
      """, (resultado, linha) -> new AlteracaoPrecificacao(
        resultado.getLong("codigo"), resultado.getLong("produto_codigo"),
        resultado.getObject("data_alteracao", LocalDateTime.class),
        new AlteracaoPrecificacao.Valores(
          resultado.getBigDecimal("custo_anterior"), resultado.getBigDecimal("preco_venda_anterior"),
          resultado.getBigDecimal("percentual_anterior")
        ),
        new AlteracaoPrecificacao.Valores(
          resultado.getBigDecimal("custo_novo"), resultado.getBigDecimal("preco_venda_novo"),
          resultado.getBigDecimal("percentual_novo")
        )
      ), codigoProduto);
  }
}
