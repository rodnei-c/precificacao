package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

/** Taxa fixa por simulação de boleto, dispensada quando o saldo é zero. */
public final class TaxaBoleto implements TaxaPagamento {

  private static final BigDecimal VALOR = new BigDecimal("5.00");

  /** @param saldo saldo após o desconto, sem arredondamento */
  @Override
  public BigDecimal valor(BigDecimal saldo) {
    return saldo.signum() <= 0 ? BigDecimal.ZERO : VALOR;
  }
}
