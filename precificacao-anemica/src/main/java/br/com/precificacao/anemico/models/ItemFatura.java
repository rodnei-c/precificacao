package br.com.precificacao.anemico.models;

import java.math.BigDecimal;

public class ItemFatura {

    private Produto produto;
    private BigDecimal quantidade;

    public ItemFatura() {
    }

    public ItemFatura(Produto produto, BigDecimal quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
