package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

/** Traduz falhas de gravação do produto de acordo com a operação executada. */
final class FalhaNaGravacaoProduto {

  private final SQLException causa;
  private final boolean comCategoria;
  private final Logger registro;

  FalhaNaGravacaoProduto(SQLException causa, boolean comCategoria, Logger registro) {
    this.causa = Objects.requireNonNull(causa);
    this.comCategoria = comCategoria;
    this.registro = Objects.requireNonNull(registro);
  }

  FalhaProduto traduzida() {
    if (this.categoriaAusente()) {
      return new FalhaProduto(FalhaProduto.Motivo.CATEGORIA_AUSENTE, this.causa);
    }
    this.registro.severe("Falha ao gravar produto (SQLState: " + this.causa.getSQLState() + ").");
    return new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, this.causa);
  }

  private boolean categoriaAusente() {
    return this.comCategoria && this.causa instanceof PSQLException postgres
      && PSQLState.FOREIGN_KEY_VIOLATION.getState().equals(postgres.getSQLState())
      && postgres.getServerErrorMessage() != null
      && "produto_categoria_codigo_fkey".equals(postgres.getServerErrorMessage().getConstraint());
  }
}
