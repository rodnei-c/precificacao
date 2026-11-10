package br.com.modelagemdominio.rico.faturas.descontos;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import java.math.BigDecimal;
import java.util.Optional;

/** Desconto percentual sobre a soma dos itens, sem valor mínimo de fatura. */
public final class DescontoPercentual implements Desconto {

  private final BigDecimal percentual;

  /**
   * @param percentual pontos percentuais entre zero e cem, com até quatro casas decimais
   * @throws FaturaInvalida quando o percentual for ausente, inválido ou exceder a precisão
   */
  public DescontoPercentual(BigDecimal percentual) {
    if (percentual == null || percentual.signum() < 0
      || percentual.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new FaturaInvalida("O desconto percentual deve estar entre zero e 100%.");
    }
    if (percentual.stripTrailingZeros().scale() > 4) {
      throw new FaturaInvalida("Informe o desconto percentual com até quatro casas decimais.");
    }
    this.percentual = percentual;
  }

  /** @return percentual contratado em pontos percentuais */
  public BigDecimal percentual() {
    return this.percentual;
  }

  /**
   * @param subtotal soma não negativa dos itens
   * @return abatimento calculado sem arredondamento intermediário
   */
  @Override
  public BigDecimal valorAplicado(BigDecimal subtotal) {
    return subtotal.multiply(this.percentual).movePointLeft(2);
  }

  /** @return representação do percentual contratado, inclusive quando for zero */
  @Override
  public Optional<DescontoContratado> registro() {
    return Optional.of(new DescontoContratado(TipoDesconto.PERCENTUAL, this.percentual));
  }
}
