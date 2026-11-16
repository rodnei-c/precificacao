package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

final class TaxaBoleto implements TaxaPagamento {

  @Override
  public BigDecimal calcular(BigDecimal saldo) {
    return saldo.signum() <= 0 ? BigDecimal.ZERO : new BigDecimal("5.00");
  }
}
