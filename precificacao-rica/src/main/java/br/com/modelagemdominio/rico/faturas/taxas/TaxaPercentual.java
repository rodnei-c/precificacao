package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;
import java.util.Objects;

public final class TaxaPercentual implements TaxaPagamento {
  private final BigDecimal percentual;

  public TaxaPercentual(BigDecimal percentual) {
    this.percentual = Objects.requireNonNull(percentual);
    if (percentual.signum() < 0) {
      throw new IllegalArgumentException("A taxa percentual não pode ser negativa.");
    }
  }

  @Override
  public BigDecimal valorSobre(BigDecimal saldo) {
    return saldo.multiply(this.percentual).movePointLeft(2);
  }
}
