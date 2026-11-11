package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoContratado;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/** Grava o cabeçalho na conexão da transação e devolve o código gerado. */
final class CadastroDoCabecalhoDaFatura {

  private final Connection conexao;

  CadastroDoCabecalhoDaFatura(Connection conexao) {
    this.conexao = conexao;
  }

  long cadastrar(CalculoFatura calculo, VinculosFatura vinculos) throws SQLException {
    try (var cadastro = this.conexao.prepareStatement("""
      INSERT INTO public.fatura (tipo_desconto, valor_desconto, cliente_codigo, forma_pagamento)
      VALUES (?, ?, ?, ?) RETURNING id
      """)) {
      var desconto = calculo.desconto().registro();
      cadastro.setString(1, desconto.map(DescontoContratado::tipo).map(Enum::name).orElse(null));
      cadastro.setBigDecimal(2, desconto.map(DescontoContratado::valor).orElse(null));
      cadastro.setObject(3, vinculos.codigoCliente().orElse(null), Types.BIGINT);
      cadastro.setString(4, vinculos.formaPagamento().map(FormaPagamento::name).orElse(null));
      try (var resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("O cadastro não retornou o código da fatura.");
        }
        return resultado.getLong("id");
      }
    }
  }
}
