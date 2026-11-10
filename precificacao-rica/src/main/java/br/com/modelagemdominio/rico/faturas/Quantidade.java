package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

/** Quantidade positiva de um item, com até 15 dígitos inteiros e quatro casas decimais. */
public final class Quantidade {

  private static final BigDecimal LIMITE = new BigDecimal("999999999999999.9999");

  private final BigDecimal valor;

  /**
   * @param valor quantidade inteira ou fracionária
   * @throws FaturaInvalida quando a quantidade não for positiva ou exceder a precisão
   */
  public Quantidade(BigDecimal valor) {
    if (valor == null || valor.signum() <= 0) {
      throw new FaturaInvalida("A quantidade deve ser maior que zero.");
    }
    if (valor.stripTrailingZeros().scale() > 4) {
      throw new FaturaInvalida("Informe a quantidade com até quatro casas decimais.");
    }
    if (valor.compareTo(LIMITE) > 0) {
      throw new FaturaInvalida("A quantidade deve caber em 15 dígitos inteiros.");
    }
    this.valor = valor;
  }

  /** @return quantidade informada, sem arredondamento */
  public BigDecimal valor() {
    return this.valor;
  }
}
