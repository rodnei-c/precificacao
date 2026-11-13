package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.FaturaConstante;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoContratado;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoReconstituido;
import br.com.modelagemdominio.rico.faturas.descontos.TipoDesconto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Cabeçalho materializado que compõe a fatura com seus itens, sem conservar o cursor SQL. */
record CabecalhoFatura(long codigo, LocalDateTime data, Desconto desconto, VinculosFatura vinculos) {

  /**
   * @param resultado cursor posicionado na linha do cabeçalho
   * @throws SQLException quando não for possível ler as colunas
   */
  CabecalhoFatura(ResultSet resultado) throws SQLException {
    this(resultado.getLong("id"), resultado.getObject("data_criacao", LocalDateTime.class),
      resultado.getString("tipo_desconto") == null ? new DescontoReconstituido()
        : new DescontoReconstituido(new DescontoContratado(
          TipoDesconto.valueOf(resultado.getString("tipo_desconto")), resultado.getBigDecimal("valor_desconto"))),
      new VinculosFatura(
        Optional.ofNullable(resultado.getObject("cliente_codigo", Long.class)),
        Optional.ofNullable(resultado.getString("forma_pagamento")).map(FormaPagamento::valueOf)));
  }

  /** @param itens itens da mesma consulta usados para compor a fatura */
  Fatura fatura(List<ItemFatura> itens) {
    return new FaturaConstante(this.codigo, this.data, new CalculoFatura(itens, this.desconto), this.vinculos);
  }
}
