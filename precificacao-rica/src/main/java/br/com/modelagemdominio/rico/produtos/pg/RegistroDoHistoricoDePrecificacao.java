package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.Precificacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Registra os valores anteriores e gravados na transação do chamador. */
final class RegistroDoHistoricoDePrecificacao {

  private final Connection conexao;

  RegistroDoHistoricoDePrecificacao(Connection conexao) {
    this.conexao = conexao;
  }

  void registrar(long codigo, Precificacao anterior, Precificacao nova) throws SQLException {
    try (PreparedStatement insercao = this.conexao.prepareStatement("""
      INSERT INTO public.historico_precificacao
        (produto_codigo, custo_anterior, preco_venda_anterior, percentual_anterior,
        custo_novo, preco_venda_novo, percentual_novo)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """)) {
      insercao.setLong(1, codigo);
      new ParametrosSqlDaPrecificacao(anterior).informar(insercao, 2);
      new ParametrosSqlDaPrecificacao(nova).informar(insercao, 5);
      if (insercao.executeUpdate() != 1) {
        throw new SQLException("A alteração não gerou o registro de histórico.");
      }
    }
  }
}
