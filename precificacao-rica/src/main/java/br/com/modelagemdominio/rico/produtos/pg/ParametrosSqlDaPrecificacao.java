package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.Precificacao;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Preenche os três parâmetros consecutivos dos valores de uma precificação. */
record ParametrosSqlDaPrecificacao(Precificacao precificacao) {

  void informar(PreparedStatement comando, int inicio) throws SQLException {
    comando.setBigDecimal(inicio, this.precificacao.custo().valor());
    comando.setBigDecimal(inicio + 1, this.precificacao.precoVenda().valor());
    comando.setBigDecimal(inicio + 2, this.precificacao.percentual().valor());
  }
}
