package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

final class SemTaxaPagamento implements TaxaPagamento {

  @Override
  public BigDecimal calcular(BigDecimal saldo) {
    return BigDecimal.ZERO;
  }
}
