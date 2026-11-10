package br.com.modelagemdominio.rico.categorias.pg;

import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

/** Classifica a falha da renomeação, conservando a causa e o registro de indisponibilidade. */
final class FalhaNaRenomeacaoCategoria {

  private final SQLException causa;
  private final Logger registro;

  /** @param causa falha SQL da renomeação, registrada no mesmo logger de PgCategorias */
  FalhaNaRenomeacaoCategoria(SQLException causa) {
    this(causa, Logger.getLogger(PgCategorias.class.getName()));
  }

  /**
   * @param causa falha SQL original
   * @param registro destino do log das falhas de persistência
   */
  FalhaNaRenomeacaoCategoria(SQLException causa, Logger registro) {
    this.causa = Objects.requireNonNull(causa);
    this.registro = Objects.requireNonNull(registro);
  }

  /** @return falha de categoria correspondente, mantendo a exceção SQL como causa */
  FalhaCategoria traduzida() {
    if (this.causa instanceof PSQLException postgres
      && PSQLState.UNIQUE_VIOLATION.getState().equals(postgres.getSQLState())
      && postgres.getServerErrorMessage() != null
      && "ux_categoria_nome".equals(postgres.getServerErrorMessage().getConstraint())) {
      return new FalhaCategoria(FalhaCategoria.Motivo.NOME_DUPLICADO, this.causa);
    }
    this.registro.severe("Falha ao renomear categoria (SQLState: " + this.causa.getSQLState() + ").");
    return new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, this.causa);
  }
}
