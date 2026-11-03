package br.com.modelagemdominio.rico.faturas.descontos;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import java.math.BigDecimal;

/** Desconto que preserva o valor contratado e limita o abatimento ao subtotal atual. */
public final class DescontoFixo implements Desconto {

  private final BigDecimal valorOriginal;

  /**
   * @param valorOriginal valor monetário não negativo, com até duas casas decimais
   * @throws FaturaInvalida quando o valor for ausente, negativo ou exceder a precisão
   */
  public DescontoFixo(BigDecimal valorOriginal) {
    if (valorOriginal == null || valorOriginal.signum() < 0) {
      throw new FaturaInvalida("O desconto fixo deve ser não negativo.");
    }
    if (valorOriginal.stripTrailingZeros().scale() > 2) {
      throw new FaturaInvalida("Informe o desconto fixo com até duas casas decimais.");
    }
    this.valorOriginal = valorOriginal;
  }

  /** @return valor contratado, preservado mesmo quando os preços diminuírem */
  public BigDecimal valorOriginal() {
    return this.valorOriginal;
  }

  /**
   * @param subtotal soma não negativa dos itens
   * @return o menor valor entre o desconto original e o subtotal atual
   */
  @Override
  public BigDecimal valorAplicado(BigDecimal subtotal) {
    return this.valorOriginal.min(subtotal);
  }
}
