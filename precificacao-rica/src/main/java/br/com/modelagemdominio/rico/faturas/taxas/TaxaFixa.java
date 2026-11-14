package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;
import java.util.Objects;

/** Cobra um valor fixo por simulação. */
public final class TaxaFixa implements TaxaPagamento {

  private final BigDecimal valor;

  public TaxaFixa(BigDecimal valor) {
    this.valor = Objects.requireNonNull(valor);
  }

  @Override
  public BigDecimal sobre(BigDecimal saldo) {
    return this.valor;
  }
}