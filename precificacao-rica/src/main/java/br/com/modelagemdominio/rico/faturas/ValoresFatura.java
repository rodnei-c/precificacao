package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Resultado imutável de um cálculo, utilizável sem identidade ou data de criação. */
public final class ValoresFatura {

  private final List<ValoresItemFatura> itens;
  private final Desconto desconto;
  private final TaxaPagamento taxa;

  ValoresFatura(List<ValoresItemFatura> itens, Desconto desconto, TaxaPagamento taxa) {
    if (taxa == null) {
      throw new FaturaInvalida("Informe a taxa de pagamento ou sua ausência.");
    }
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

  /** @return taxa calculada sobre o saldo após o desconto, sem arredondamento */
  public BigDecimal taxaPagamento() {
    return this.taxa.valor(this.saldo());
  }

  /** @return saldo e taxa somados, arredondados somente aqui para duas casas */
  public BigDecimal total() {
    BigDecimal saldo = this.saldo();
    return saldo.add(this.taxa.valor(saldo)).setScale(2, RoundingMode.HALF_EVEN);
  }

  private BigDecimal saldo() {
    return this.subtotal().subtract(this.descontoAplicado());
  }
}
