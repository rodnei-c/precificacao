package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

/** Valores de uma leitura do item, sem acesso à coleção de produtos durante seu uso. */
public final class ValoresItemFatura {

  private final long codigoProduto;
  private final Quantidade quantidade;
  private final BigDecimal precoVenda;

  ValoresItemFatura(ItemFatura item, BigDecimal precoVenda) {
    this.codigoProduto = item.codigoProduto();
    this.quantidade = item.quantidade();
    this.precoVenda = precoVenda;
  }

  /** @return código do produto desta leitura */
  public long codigoProduto() {
    return this.codigoProduto;
  }

  /** @return quantidade validada do item */
  public Quantidade quantidade() {
    return this.quantidade;
  }

  /** @return preço observado durante o cálculo */
  public BigDecimal precoVenda() {
    return this.precoVenda;
  }

  /** @return preço multiplicado pela quantidade, sem arredondamento */
  public BigDecimal subtotal() {
    return this.precoVenda.multiply(this.quantidade.valor());
  }
}
