package br.com.modelagemdominio.rico.faturas.descontos;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import java.math.BigDecimal;
import java.util.Optional;

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

  /**
   * @param subtotal soma inicial dos itens, sem arredondamento
   * @throws FaturaInvalida quando o valor contratado superar o subtotal inicial
   */
  @Override
  public void validarSubtotalInicial(BigDecimal subtotal) {
    if (this.valorOriginal.compareTo(subtotal) > 0) {
      throw new FaturaInvalida("O desconto fixo não pode ultrapassar o subtotal inicial da fatura.");
    }
  }

  /** @return representação do valor original, mesmo quando o abatimento tiver sido limitado */
  @Override
  public Optional<DescontoContratado> registro() {
    return Optional.of(new DescontoContratado(TipoDesconto.VALOR_FIXO, this.valorOriginal));
  }
}
