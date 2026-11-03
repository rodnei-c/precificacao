package br.com.modelagemdominio.rico.faturas.descontos;

import java.math.BigDecimal;

/** Regra para determinar o abatimento sobre a soma dos itens de uma fatura. */
public interface Desconto {

  /**
   * @param subtotal soma não negativa dos itens, sem arredondamento
   * @return abatimento aplicado, sem arredondamento e limitado ao subtotal
   */
  BigDecimal valorAplicado(BigDecimal subtotal);
}
