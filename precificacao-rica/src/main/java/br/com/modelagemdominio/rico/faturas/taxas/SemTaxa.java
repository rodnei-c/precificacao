package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

/** Formas de pagamento sem acréscimo, inclusive quando a forma não é informada. */
public final class SemTaxa implements TaxaPagamento {

  /** @param saldo saldo após o desconto, sem arredondamento */
  @Override
  public BigDecimal valor(BigDecimal saldo) {
    return BigDecimal.ZERO;
  }
}
