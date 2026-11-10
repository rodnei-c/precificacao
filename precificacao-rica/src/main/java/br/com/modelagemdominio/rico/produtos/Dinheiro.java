package br.com.modelagemdominio.rico.produtos;

import java.math.BigDecimal;

/** Valor monetário positivo usado no custo e no preço de venda de um produto. */
public record Dinheiro(BigDecimal valor) {

  private static final BigDecimal LIMITE = new BigDecimal("99999999999999999.99");

  /** @param valor representação decimal exata, sem conversão por ponto flutuante */
  public Dinheiro(String valor) {
    this(valor == null ? null : new BigDecimal(valor));
  }

  /** @param valor quantidade inteira de unidades monetárias */
  public Dinheiro(long valor) {
    this(BigDecimal.valueOf(valor));
  }

  /**
   * @param valor quantia positiva, com até duas casas e 17 dígitos inteiros
   * @throws ValorInvalido quando a quantia não respeitar os limites do produto
   */
  public Dinheiro {
    if (valor == null) {
      throw new ValorInvalido("Informe o %s.", "valor monetário");
    }
    if (valor.signum() <= 0 || valor.compareTo(LIMITE) > 0) {
      throw new ValorInvalido("O %s deve ser maior que zero e caber em 17 dígitos inteiros.", "valor monetário");
    }
    if (valor.stripTrailingZeros().scale() > 2) {
      throw new ValorInvalido("Informe o %s com até duas casas decimais.", "valor monetário");
    }
    valor = valor.setScale(2);
  }

  /**
   * @param outro outra quantia válida
   * @return ordem numérica entre as quantias
   */
  public int comparar(Dinheiro outro) {
    return this.valor.compareTo(outro.valor);
  }
}
