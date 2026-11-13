package br.com.modelagemdominio.rico.faturas.descontos;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import java.math.BigDecimal;

/** Tipo e valor original, sem armazenar o abatimento limitado pelo subtotal atual. */
public record DescontoContratado(TipoDesconto tipo, BigDecimal valor) {

  /**
   * @param tipo tipo registrado
   * @param valor valor original validado pela estratégia correspondente
   * @throws FaturaInvalida quando faltar tipo ou o valor não respeitar as regras do desconto
   */
  public DescontoContratado {
    if (tipo == null) {
      throw new FaturaInvalida("Informe o tipo do desconto.");
    }
    tipo.desconto(valor);
  }

  /** @return comportamento reconstituído com o valor original */
  public Desconto desconto() {
    return this.tipo.desconto(this.valor);
  }
}
