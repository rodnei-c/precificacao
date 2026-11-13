package br.com.modelagemdominio.rico.faturas.descontos;

import java.math.BigDecimal;
import java.util.Optional;

/** Representa uma fatura sem abatimento, evitando um desconto nulo. */
public final class SemDesconto implements Desconto {

  /**
   * @param subtotal soma não negativa dos itens
   * @return zero
   */
  @Override
  public BigDecimal valorAplicado(BigDecimal subtotal) {
    return BigDecimal.ZERO;
  }

  /** @return ausência de tipo e valor; distingue uma fatura sem desconto de desconto zero */
  @Override
  public Optional<DescontoContratado> registro() {
    return Optional.empty();
  }
}
