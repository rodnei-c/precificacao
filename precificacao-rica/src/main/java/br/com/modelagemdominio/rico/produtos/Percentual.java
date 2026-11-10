package br.com.modelagemdominio.rico.produtos;

import java.math.BigDecimal;

/** Percentual não negativo de precificação, sem impor o limite específico da margem. */
public record Percentual(BigDecimal valor) {

  private static final BigDecimal LIMITE = new BigDecimal("999999999999999.9999");

  /** @param valor pontos percentuais em representação decimal exata */
  public Percentual(String valor) {
    this(valor == null ? null : new BigDecimal(valor));
  }

  /** @param valor pontos percentuais inteiros */
  public Percentual(long valor) {
    this(BigDecimal.valueOf(valor));
  }

  /**
   * @param valor pontos percentuais com até quatro casas e 15 dígitos inteiros
   * @throws ValorInvalido quando o percentual for ausente, negativo ou exceder os limites
   */
  public Percentual {
    if (valor == null) {
      throw new ValorInvalido("Informe o %s.", "percentual");
    }
    if (valor.signum() < 0 || valor.compareTo(LIMITE) > 0) {
      throw new ValorInvalido("O %s deve ser não negativo e caber em 15 dígitos inteiros.", "percentual");
    }
    if (valor.stripTrailingZeros().scale() > 4) {
      throw new ValorInvalido("Informe o %s com até quatro casas decimais.", "percentual");
    }
    valor = valor.setScale(4);
  }

  /** @return proporção decimal exata, sem arredondamento intermediário */
  public BigDecimal fracao() {
    return this.valor.movePointLeft(2);
  }

  /**
   * @param outro outro percentual válido
   * @return ordem numérica entre os percentuais
   */
  public int comparar(Percentual outro) {
    return this.valor.compareTo(outro.valor);
  }
}
