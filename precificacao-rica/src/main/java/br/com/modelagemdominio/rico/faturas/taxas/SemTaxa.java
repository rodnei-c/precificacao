package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

/** Forma de pagamento sem cobrança adicional. */
public final class SemTaxa implements TaxaPagamento {

  @Override
  public BigDecimal sobre(BigDecimal saldo) {
    return BigDecimal.ZERO;
  }
}