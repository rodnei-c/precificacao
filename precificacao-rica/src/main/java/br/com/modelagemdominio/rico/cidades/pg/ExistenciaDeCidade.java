package br.com.modelagemdominio.rico.cidades.pg;

import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import java.sql.Connection;
import java.sql.SQLException;

/** Consulta a referência da cidade na conexão do chamador. */
public final class ExistenciaDeCidade {

  private final Connection conexao;

  public ExistenciaDeCidade(Connection conexao) {
    this.conexao = conexao;
  }

  public boolean existe(CodigoCidade codigo) throws SQLException {
    try (var consulta = this.conexao.prepareStatement("SELECT 1 FROM public.cidade WHERE codigo = ?")) {
      consulta.setLong(1, codigo.valor());
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }
}
