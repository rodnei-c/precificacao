package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;
import java.util.Objects;

/** Decorador: não cobra nada quando não há saldo a pagar. */
public final class TaxaSomenteComSaldo implements TaxaPagamento {

  private final TaxaPagamento taxa;

  public TaxaSomenteComSaldo(TaxaPagamento taxa) {
    this.taxa = Objects.requireNonNull(taxa);
  }

  @Override
  public BigDecimal sobre(BigDecimal saldo) {
    return saldo.signum() <= 0 ? BigDecimal.ZERO : this.taxa.sobre(saldo);
  }
}