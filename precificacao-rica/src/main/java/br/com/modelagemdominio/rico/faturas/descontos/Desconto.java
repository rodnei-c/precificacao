package br.com.modelagemdominio.rico.faturas.descontos;

import java.math.BigDecimal;
import java.util.Optional;

/** Regra para determinar o abatimento sobre a soma dos itens de uma fatura. */
public interface Desconto {

  /**
   * @param subtotal soma não negativa dos itens, sem arredondamento
   * @return abatimento aplicado, sem arredondamento e limitado ao subtotal
   */
  BigDecimal valorAplicado(BigDecimal subtotal);

  /**
   * @param subtotal soma inicial dos itens, sem arredondamento
   * @throws br.com.modelagemdominio.rico.faturas.FaturaInvalida quando o desconto inicial não for aplicável
   */
  default void validarSubtotalInicial(BigDecimal subtotal) {
    // Apenas o desconto fixo exige uma comparação adicional com o subtotal inicial.
  }

  /** @return tipo e valor contratado, ou ausência de desconto para gravação */
  Optional<DescontoContratado> registro();
}
