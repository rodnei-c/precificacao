package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;
import java.util.Objects;

/** Cobra um percentual do saldo, sem arredondar o resultado. */
public final class TaxaPercentual implements TaxaPagamento {

  private final BigDecimal percentual;

  /** @param percentual valor em pontos percentuais, por exemplo 2 para 2% */
  public TaxaPercentual(BigDecimal percentual) {
    this.percentual = Objects.requireNonNull(percentual);
  }

  @Override
  public BigDecimal sobre(BigDecimal saldo) {
    return saldo.multiply(this.percentual).movePointLeft(2);
  }
}