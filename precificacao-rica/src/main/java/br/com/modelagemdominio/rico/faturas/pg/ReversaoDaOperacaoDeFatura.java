package br.com.modelagemdominio.rico.faturas.pg;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/** Desfaz a operação e conserva a falha original se o rollback também falhar. */
final class ReversaoDaOperacaoDeFatura {

  private final Connection conexao;
  private final Logger registro;

  ReversaoDaOperacaoDeFatura(Connection conexao, Logger registro) {
    this.conexao = conexao;
    this.registro = registro;
  }

  void desfazer(Throwable falha) {
    try {
      this.conexao.rollback();
    } catch (SQLException falhaAoDesfazer) {
      this.registro.severe("Falha ao desfazer operação de fatura (SQLState: " + falhaAoDesfazer.getSQLState() + ").");
      if (falhaAoDesfazer != falha) {
        falha.addSuppressed(falhaAoDesfazer);
      }
    }
  }
}
