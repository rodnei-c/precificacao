package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Resultado imutável de um cálculo, utilizável sem identidade ou data de criação. */
public final class ValoresFatura {

  private final List<ValoresItemFatura> itens;
  private final Desconto desconto;

  ValoresFatura(List<ValoresItemFatura> itens, Desconto desconto) {
    this.itens = List.copyOf(itens);
    this.desconto = desconto;
  }

  /** @return itens calculados na ordem recebida, em uma lista imutável */
  public List<ValoresItemFatura> itens() {
    return this.itens;
  }

  /** @return soma exata dos subtotais dos itens desta leitura */
  public BigDecimal subtotal() {
    return this.itens.stream().map(ValoresItemFatura::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** @return abatimento sem arredondamento, limitado ao subtotal desta leitura */
  public BigDecimal descontoAplicado() {
    return this.desconto.valorAplicado(this.subtotal());
  }

  /** @return saldo após o desconto, arredondado somente aqui para duas casas */
  public BigDecimal total() {
    return this.subtotal().subtract(this.descontoAplicado()).setScale(2, RoundingMode.HALF_EVEN);
  }
}
