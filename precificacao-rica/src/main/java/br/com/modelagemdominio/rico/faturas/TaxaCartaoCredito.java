package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

final class TaxaCartaoCredito implements TaxaPagamento {

  @Override
  public BigDecimal calcular(BigDecimal saldo) {
    return saldo.signum() <= 0 ? BigDecimal.ZERO : saldo.multiply(BigDecimal.valueOf(2)).movePointLeft(2);
  }
}
