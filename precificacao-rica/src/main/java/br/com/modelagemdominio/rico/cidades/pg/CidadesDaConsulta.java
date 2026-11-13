package br.com.modelagemdominio.rico.cidades.pg;

import br.com.modelagemdominio.rico.cidades.Cidade;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class CidadesDaConsulta {

  private final PreparedStatement consulta;

  CidadesDaConsulta(PreparedStatement consulta) {
    this.consulta = consulta;
  }

  List<Cidade> cidades() throws SQLException {
    try (ResultSet resultado = this.consulta.executeQuery()) {
      List<Cidade> cidades = new ArrayList<>();
      while (resultado.next()) {
        cidades.add(new ResultadoSqlDeCidade(resultado).cidade());
      }
      return List.copyOf(cidades);
    }
  }
}
