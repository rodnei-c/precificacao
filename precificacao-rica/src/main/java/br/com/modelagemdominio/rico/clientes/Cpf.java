package br.com.modelagemdominio.rico.clientes;

import java.util.regex.Pattern;

public record Cpf(String valor) {

  private static final Pattern FORMATO = Pattern.compile("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}");
  private static final Pattern REPETIDO = Pattern.compile("([0-9])\\1{10}");
  private static final String INVALIDO = "Informe um CPF válido.";

  public Cpf {
    valor = new TextoObrigatorio("CPF", valor, 14).valor();
    if (!FORMATO.matcher(valor).matches()) {
      throw new IllegalArgumentException(INVALIDO);
    }
    valor = valor.replace(".", "").replace("-", "");
    if (REPETIDO.matcher(valor).matches() || !new DigitosVerificadoresCpf(valor).validos()) {
      throw new IllegalArgumentException(INVALIDO);
    }
  }
}
