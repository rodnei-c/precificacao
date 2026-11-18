package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPagamento;
import br.com.modelagemdominio.rico.faturas.taxas.SemTaxaPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** Resultado imutável de um cálculo, utilizável sem identidade ou data de criação. */
public final class ValoresFatura {

  private final List<ValoresItemFatura> itens;
  private final Desconto desconto;
  private final TaxaPagamento taxa;

  ValoresFatura(List<ValoresItemFatura> itens, Desconto desconto) {
    this(itens, desconto, new SemTaxaPagamento());
  }

  private ValoresFatura(List<ValoresItemFatura> itens, Desconto desconto, TaxaPagamento taxa) {
    this.itens = List.copyOf(itens);
    this.desconto = Objects.requireNonNull(desconto);
    this.taxa = Objects.requireNonNull(taxa);
  }

  public ValoresFatura comTaxa(TaxaPagamento taxa) {
    return new ValoresFatura(this.itens, this.desconto, taxa);
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

  public BigDecimal taxaPagamento() {
    return this.taxa.valorSobre(this.subtotal().subtract(this.descontoAplicado()));
  }

  public BigDecimal total() {
    return this.subtotal().subtract(this.descontoAplicado()).add(this.taxaPagamento())
      .setScale(2, RoundingMode.HALF_EVEN);
  }
}
