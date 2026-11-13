package br.com.modelagemdominio.rico.clientes.pg;

import java.sql.Connection;
import java.sql.SQLException;

/** Consulta a referência do cliente na conexão do chamador. */
final class ExistenciaDeCliente {

  private final Connection conexao;

  ExistenciaDeCliente(Connection conexao) {
    this.conexao = conexao;
  }

  boolean existe(long codigo, boolean proteger) throws SQLException {
    String bloqueio = proteger ? " FOR KEY SHARE" : "";
    try (var consulta = this.conexao.prepareStatement("SELECT 1 FROM public.cliente WHERE codigo = ?" + bloqueio)) {
      consulta.setLong(1, codigo);
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }
}
