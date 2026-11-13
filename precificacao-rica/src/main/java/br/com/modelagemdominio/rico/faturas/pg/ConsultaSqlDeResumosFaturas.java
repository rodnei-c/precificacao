package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.ResumoFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/** Consulta agregada de cabeçalho e contagem de itens, sem buscar produtos ou preços. */
final class ConsultaSqlDeResumosFaturas {

  private final DataSource banco;

  ConsultaSqlDeResumosFaturas(DataSource banco) {
    this.banco = banco;
  }

  List<ResumoFatura> listar(FiltrosFaturas filtros) throws SQLException {
    var sql = new StringBuilder("""
      SELECT f.id, f.data_criacao, f.cliente_codigo, f.forma_pagamento,
        COUNT(i.produto_codigo) AS quantidade_itens
      FROM public.fatura f LEFT JOIN public.item_fatura i ON i.fatura_id = f.id
      WHERE TRUE
      """);
    var datas = new ArrayList<LocalDateTime>(2);
    filtros.inicioInclusivo().ifPresent(data -> {
      sql.append(" AND f.data_criacao >= ?");
      datas.add(data);
    });
    filtros.fimExclusivo().ifPresent(data -> {
      sql.append(" AND f.data_criacao < ?");
      datas.add(data);
    });
    if (filtros.codigoCliente().isPresent()) {
      sql.append(" AND f.cliente_codigo = ?");
    }
    sql.append(" GROUP BY f.id, f.data_criacao, f.cliente_codigo, f.forma_pagamento"
      + " ORDER BY f.data_criacao DESC, f.id DESC");
    try (var conexao = this.banco.getConnection(); var consulta = conexao.prepareStatement(sql.toString())) {
      int indice = 1;
      for (LocalDateTime data : datas) {
        consulta.setObject(indice++, data);
      }
      if (filtros.codigoCliente().isPresent()) {
        consulta.setLong(indice, filtros.codigoCliente().orElseThrow());
      }
      var resumos = new ArrayList<ResumoFatura>();
      try (var resultado = consulta.executeQuery()) {
        while (resultado.next()) {
          var vinculos = new VinculosFatura(Optional.ofNullable(resultado.getObject("cliente_codigo", Long.class)),
            Optional.ofNullable(resultado.getString("forma_pagamento")).map(FormaPagamento::valueOf));
          resumos.add(new ResumoFatura(resultado.getLong("id"), resultado.getObject("data_criacao", LocalDateTime.class),
            resultado.getLong("quantidade_itens"), vinculos));
        }
      }
      return List.copyOf(resumos);
    }
  }
}
