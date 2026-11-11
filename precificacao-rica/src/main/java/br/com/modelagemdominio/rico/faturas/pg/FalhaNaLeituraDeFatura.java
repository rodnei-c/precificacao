package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import java.sql.SQLException;
import java.util.logging.Logger;

/** Traduz falhas técnicas de consulta ou materialização de faturas. */
final class FalhaNaLeituraDeFatura {

  private final Exception causa;
  private final Logger registro;

  FalhaNaLeituraDeFatura(Exception causa) {
    this(causa, Logger.getLogger(PgFaturas.class.getName()));
  }

  FalhaNaLeituraDeFatura(Exception causa, Logger registro) {
    this.causa = causa;
    this.registro = registro;
  }

  FalhaFatura traduzida() {
    String estado = this.causa instanceof SQLException sql ? sql.getSQLState() : "não aplicável";
    this.registro.severe(
      "Falha ao ler fatura (" + this.causa.getClass().getSimpleName() + ", SQLState: " + estado + ").");
    return new FalhaFatura(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, this.causa);
  }
}
