package br.com.modelagemdominio.rico.infraestrutura.banco;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Nome de banco aceito no endereço JDBC e no comando de criação. */
record NomeBancoPostgres(String valor) {

  NomeBancoPostgres {
    if (valor == null || valor.isBlank() || valor.indexOf('\0') >= 0
      || valor.getBytes(StandardCharsets.UTF_8).length > 63) {
      throw new FalhaInicializacaoBanco("Informe um nome de banco não vazio com até 63 bytes.");
    }
  }

  String codificado() {
    return URLEncoder.encode(this.valor, StandardCharsets.UTF_8).replace("+", "%20");
  }

  String identificadorSql() {
    return "\"" + this.valor.replace("\"", "\"\"") + "\"";
  }
}
