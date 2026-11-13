package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

/** Traduz as violações de vínculo conhecidas, inclusive em exceções de gravação em lote. */
final class FalhaNaGravacaoDeFatura {

  private final SQLException causa;
  private final Logger registro;

  FalhaNaGravacaoDeFatura(SQLException causa, Logger registro) {
    this.causa = causa;
    this.registro = registro;
  }

  FalhaFatura traduzida() {
    for (SQLException falha = this.causa; falha != null; falha = falha.getNextException()) {
      String restricao = this.restricaoDeChaveEstrangeira(falha);
      if ("fatura_cliente_codigo_fkey".equals(restricao)) {
        return new FalhaFatura(FalhaFatura.Motivo.CLIENTE_AUSENTE, this.causa);
      }
      if ("item_fatura_produto_codigo_fkey".equals(restricao)) {
        return new FalhaFatura(FalhaFatura.Motivo.PRODUTO_AUSENTE, this.causa);
      }
    }
    this.registro.severe("Falha ao cadastrar fatura (SQLState: " + this.causa.getSQLState() + ").");
    return new FalhaFatura(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, this.causa);
  }

  private String restricaoDeChaveEstrangeira(SQLException falha) {
    if (falha instanceof PSQLException postgres && PSQLState.FOREIGN_KEY_VIOLATION.getState().equals(falha.getSQLState())) {
      var detalhes = postgres.getServerErrorMessage();
      return detalhes == null ? null : detalhes.getConstraint();
    }
    return null;
  }
}
