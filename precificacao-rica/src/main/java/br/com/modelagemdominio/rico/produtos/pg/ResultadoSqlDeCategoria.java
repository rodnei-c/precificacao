package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Interpreta o vínculo opcional de categoria do produto, sem conservar o cursor SQL. */
final class ResultadoSqlDeCategoria {

  private final Optional<Long> referencia;

  /**
   * @param resultado cursor posicionado na linha que contém categoria_codigo
   * @throws SQLException quando não for possível ler a coluna
   */
  ResultadoSqlDeCategoria(ResultSet resultado) throws SQLException {
    this(Optional.ofNullable(resultado.getObject("categoria_codigo", Long.class)));
  }

  /** @param referencia código lido ou ausência explícita de vínculo */
  ResultadoSqlDeCategoria(Optional<Long> referencia) {
    this.referencia = Objects.requireNonNull(referencia);
  }

  /** @return código validado pelo domínio, ou ausência de categoria */
  Optional<CodigoCategoria> codigo() {
    return this.referencia.map(CodigoCategoria::new);
  }
}
