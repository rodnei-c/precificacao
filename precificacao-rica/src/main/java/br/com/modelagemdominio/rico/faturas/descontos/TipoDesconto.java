package br.com.modelagemdominio.rico.faturas.descontos;

import java.math.BigDecimal;

/** Identificação dos descontos armazenáveis, sem fórmulas fora das estratégias. */
public enum TipoDesconto {
  PERCENTUAL,
  VALOR_FIXO;

  /**
   * @param valor valor original registrado para este tipo
   * @return estratégia com suas próprias validações
   */
  public Desconto desconto(BigDecimal valor) {
    return switch (this) {
      case PERCENTUAL -> new DescontoPercentual(valor);
      case VALOR_FIXO -> new DescontoFixo(valor);
    };
  }
}
