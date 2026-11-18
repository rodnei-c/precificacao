package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

public final class SemTaxaPagamento implements TaxaPagamento {
  @Override
  public BigDecimal valorSobre(BigDecimal saldo) {
    return BigDecimal.ZERO;
  }
}
