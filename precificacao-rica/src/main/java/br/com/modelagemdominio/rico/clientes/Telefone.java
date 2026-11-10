package br.com.modelagemdominio.rico.clientes;

import java.util.regex.Pattern;

public record Telefone(String valor) {

  private static final Pattern FORMATO = Pattern.compile(
    "(?:\\+55 *)?(?:\\([0-9]{2}\\)|[0-9]{2}) *9?[0-9]{4}[- ]?[0-9]{4}"
  );

  public Telefone {
    valor = new TextoObrigatorio("telefone", valor, 30).valor();
    if (!FORMATO.matcher(valor).matches()) {
      throw new IllegalArgumentException("Informe um telefone fixo ou celular no formato brasileiro, com DDD.");
    }
  }
}
