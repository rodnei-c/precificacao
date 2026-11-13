package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.dtos.FaturaResumo;
import br.com.precificacao.anemico.enums.FormaPagamento;
import br.com.precificacao.anemico.enums.TipoDesconto;
import br.com.precificacao.anemico.models.Fatura;
import br.com.precificacao.anemico.models.ItemFatura;
import br.com.precificacao.anemico.repositories.FaturaRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFaturaRepository implements FaturaRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcFaturaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Fatura salvar(Fatura fatura) {
    TipoDesconto tipoDesconto = fatura.getTipoDesconto();
    FormaPagamento formaPagamento = fatura.getFormaPagamento();
    Fatura gravada = jdbcTemplate.queryForObject(
      """
      INSERT INTO fatura (tipo_desconto, valor_desconto, cliente_codigo, forma_pagamento)
      VALUES (?, ?, ?, ?) RETURNING id, data_criacao
      """,
      (resultado, linha) -> new Fatura(
        resultado.getLong("id"), resultado.getObject("data_criacao", LocalDateTime.class),
        fatura.getItens(), tipoDesconto, fatura.getValorDesconto(), fatura.getCodigoCliente(), formaPagamento
      ),
      tipoDesconto == null ? null : tipoDesconto.name(), fatura.getValorDesconto(), fatura.getCodigoCliente(),
      formaPagamento == null ? null : formaPagamento.name()
    );

    List<Object[]> parametros = new ArrayList<>();
    for (ItemFatura item : gravada.getItens()) {
      parametros.add(new Object[] {
        gravada.getId(), item.getProduto().getCodigo(), item.getQuantidade(), parametros.size()
      });
    }
    jdbcTemplate.batchUpdate(
      "INSERT INTO item_fatura (fatura_id, produto_codigo, quantidade, posicao) VALUES (?, ?, ?, ?)",
      parametros
    );
    return gravada;
  }

  @Override
  public Optional<Fatura> buscarPorId(Long id) {
    return jdbcTemplate.query("""
      SELECT f.id, f.data_criacao, f.tipo_desconto, f.valor_desconto, f.cliente_codigo, f.forma_pagamento,
             i.quantidade, p.codigo, p.nome, p.custo, p.preco_venda,
             p.percentual, p.modalidade_precificacao, p.categoria_codigo
      FROM fatura f
      JOIN item_fatura i ON i.fatura_id = f.id
      JOIN produto p ON p.codigo = i.produto_codigo
      WHERE f.id = ?
      ORDER BY i.posicao
      """, resultado -> {
        if (!resultado.next()) {
          return Optional.empty();
        }
        String tipo = resultado.getString("tipo_desconto");
        List<ItemFatura> itens = new ArrayList<>();
        Fatura fatura = new Fatura(
          resultado.getLong("id"), resultado.getObject("data_criacao", LocalDateTime.class),
          itens, tipo == null ? null : TipoDesconto.valueOf(tipo),
          resultado.getBigDecimal("valor_desconto"), resultado.getObject("cliente_codigo", Long.class),
          mapearFormaPagamento(resultado)
        );
        do {
          itens.add(new ItemFatura(
            JdbcProdutoRepository.mapear(resultado), resultado.getBigDecimal("quantidade")
          ));
        } while (resultado.next());
        return Optional.of(fatura);
      }, id);
  }

  @Override
  public List<FaturaResumo> listar(LocalDateTime inicioInclusivo, LocalDateTime fimExclusivo, Long codigoCliente) {
    StringBuilder sql = new StringBuilder("""
      SELECT f.id, f.data_criacao, f.cliente_codigo, f.forma_pagamento, COUNT(i.fatura_id) AS quantidade_itens
      FROM fatura f
      LEFT JOIN item_fatura i ON i.fatura_id = f.id
      WHERE TRUE
      """);
    List<Object> parametros = new ArrayList<>();
    if (inicioInclusivo != null) {
      sql.append(" AND f.data_criacao >= ?");
      parametros.add(inicioInclusivo);
    }
    if (fimExclusivo != null) {
      sql.append(" AND f.data_criacao < ?");
      parametros.add(fimExclusivo);
    }
    if (codigoCliente != null) {
      sql.append(" AND f.cliente_codigo = ?");
      parametros.add(codigoCliente);
    }
    sql.append(" GROUP BY f.id, f.data_criacao, f.cliente_codigo, f.forma_pagamento"
      + " ORDER BY f.data_criacao DESC, f.id DESC");

    return jdbcTemplate.query(sql.toString(), (resultado, linha) -> new FaturaResumo(
      resultado.getLong("id"), resultado.getObject("data_criacao", LocalDateTime.class),
      resultado.getLong("quantidade_itens"), resultado.getObject("cliente_codigo", Long.class),
      mapearFormaPagamento(resultado)
    ), parametros.toArray());
  }

  private FormaPagamento mapearFormaPagamento(ResultSet resultado) throws SQLException {
    String forma = resultado.getString("forma_pagamento");
    return forma == null ? null : FormaPagamento.valueOf(forma);
  }
}
