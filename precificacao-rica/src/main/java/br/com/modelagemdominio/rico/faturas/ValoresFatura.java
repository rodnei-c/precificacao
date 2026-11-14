package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.taxas.SemTaxa;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Resultado imutável de um cálculo, utilizável sem identidade ou data de criação. */
public final class ValoresFatura {

  private final List<ValoresItemFatura> itens;
  private final Desconto desconto;
  private final TaxaPagamento taxa;

  ValoresFatura(List<ValoresItemFatura> itens, Desconto desconto) {
    this(itens, desconto, new SemTaxa());
  }

  ValoresFatura(List<ValoresItemFatura> itens, Desconto desconto, TaxaPagamento taxa) {
    this.itens = List.copyOf(itens);
    this.desconto = desconto;
    this.taxa = taxa;
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

  /** @return subtotal menos desconto aplicado, sem arredondamento */
  public BigDecimal saldo() {
    return this.subtotal().subtract(this.descontoAplicado());
  }

  /** @return taxa da forma de pagamento sobre o saldo, sem arredondamento */
  public BigDecimal taxaPagamento() {
    return this.taxa.sobre(this.saldo());
  }

  /** @return saldo mais taxa, arredondado somente aqui para duas casas */
  public BigDecimal total() {
    return this.saldo().add(this.taxaPagamento()).setScale(2, RoundingMode.HALF_EVEN);
  }
}