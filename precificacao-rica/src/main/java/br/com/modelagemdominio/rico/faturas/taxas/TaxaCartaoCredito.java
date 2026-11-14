package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

/** Taxa percentual do cartão de crédito sobre o saldo após o desconto. */
public final class TaxaCartaoCredito implements TaxaPagamento {

  private static final BigDecimal PERCENTUAL = new BigDecimal("0.02");

  /** @param saldo saldo após o desconto, sem arredondamento */
  @Override
  public BigDecimal valor(BigDecimal saldo) {
    return saldo.signum() <= 0 ? BigDecimal.ZERO : saldo.multiply(PERCENTUAL);
  }
}
