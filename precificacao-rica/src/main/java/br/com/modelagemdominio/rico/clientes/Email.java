package br.com.modelagemdominio.rico.clientes;

import java.util.regex.Pattern;

public record Email(String valor) {

  private static final Pattern FORMATO = Pattern.compile("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+");

  public Email {
    valor = new TextoObrigatorio("email", valor, 254).valor();
    if (!FORMATO.matcher(valor).matches()) {
      throw new IllegalArgumentException("Informe um e-mail válido.");
    }
  }
}
