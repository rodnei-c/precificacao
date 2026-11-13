package br.com.modelagemdominio.rico.clientes.pg;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/** Desfaz a transação e conserva uma eventual falha de rollback junto da falha original. */
final class ReversaoDaAlteracaoCliente {

  private final Connection conexao;
  private final Logger registro;

  ReversaoDaAlteracaoCliente(Connection conexao, Logger registro) {
    this.conexao = conexao;
    this.registro = registro;
  }

  void desfazer(Throwable falha) {
    try {
      this.conexao.rollback();
    } catch (SQLException falhaAoDesfazer) {
      this.registro.severe("Falha ao desfazer alteração do cliente (SQLState: " + falhaAoDesfazer.getSQLState() + ").");
      if (falhaAoDesfazer != falha) {
        falha.addSuppressed(falhaAoDesfazer);
      }
    }
  }
}
