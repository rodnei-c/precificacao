package br.com.modelagemdominio.rico.cidades;

import br.com.modelagemdominio.rico.numeros.InteiroExato;
import java.math.BigDecimal;

/** Identificação positiva de cidade, com conversão decimal exata por composição. */
public record CodigoCidade(long valor) {

  private static final String INVALIDO = "Informe um código de cidade inteiro positivo válido.";

  /**
   * @param valor código decimal que represente um inteiro positivo no limite de long
   * @throws IllegalArgumentException quando faltar código ou a conversão exata não for válida
   */
  public CodigoCidade(BigDecimal valor) {
    this(new InteiroExato(valor, INVALIDO).valor());
  }

  /**
   * @param valor identificação positiva da cidade
   * @throws IllegalArgumentException quando o código não for positivo
   */
  public CodigoCidade {
    if (valor <= 0) {
      throw new IllegalArgumentException(INVALIDO);
    }
  }
}
