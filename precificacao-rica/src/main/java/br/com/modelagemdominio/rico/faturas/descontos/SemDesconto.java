package br.com.modelagemdominio.rico.faturas.descontos;

import java.math.BigDecimal;

/** Representa uma fatura sem abatimento, evitando um desconto nulo. */
public final class SemDesconto implements Desconto {

  /**
   * @param subtotal soma não negativa dos itens
   * @return zero
   */
  @Override
  public BigDecimal valorAplicado(BigDecimal subtotal) {
    return BigDecimal.ZERO;
  }
}
