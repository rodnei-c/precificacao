package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

/** Regra para calcular a taxa incidente sobre o saldo de uma fatura. */
public interface TaxaPagamento {

  /**
   * @param saldo subtotal menos o desconto aplicado, sem arredondamento
   * @return taxa calculada, sem arredondamento
   */
  BigDecimal valor(BigDecimal saldo);
}
