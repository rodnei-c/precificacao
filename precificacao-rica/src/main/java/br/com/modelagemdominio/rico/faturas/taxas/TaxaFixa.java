package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;
import java.util.Objects;

public final class TaxaFixa implements TaxaPagamento {
  private final BigDecimal valor;

  public TaxaFixa(BigDecimal valor) {
    this.valor = Objects.requireNonNull(valor);
    if (valor.signum() < 0) {
      throw new IllegalArgumentException("A taxa fixa não pode ser negativa.");
    }
  }

  @Override
  public BigDecimal valorSobre(BigDecimal saldo) {
    return saldo.signum() == 0 ? BigDecimal.ZERO : this.valor;
  }
}
