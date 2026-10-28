package br.com.precificacao.anemico.models;

import br.com.precificacao.anemico.enums.TipoDesconto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Fatura {

    private Long id;
    private LocalDateTime dataCriacao;
    private List<ItemFatura> itens = new ArrayList<>();
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;

    public Fatura() {
    }

    public Fatura(Long id, LocalDateTime dataCriacao, List<ItemFatura> itens,
                  TipoDesconto tipoDesconto, BigDecimal valorDesconto) {
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.itens = itens;
        this.tipoDesconto = tipoDesconto;
        this.valorDesconto = valorDesconto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public List<ItemFatura> getItens() {
        return itens;
    }

    public void setItens(List<ItemFatura> itens) {
        this.itens = itens;
    }

    public TipoDesconto getTipoDesconto() {
        return tipoDesconto;
    }

    public void setTipoDesconto(TipoDesconto tipoDesconto) {
        this.tipoDesconto = tipoDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }
}
