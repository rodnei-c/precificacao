package br.com.precificacao.anemico.models;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;

import java.math.BigDecimal;

public class Produto {

    private Long codigo;
    private String nome;
    private BigDecimal custo;
    private BigDecimal precoVenda;
    private BigDecimal percentual;
    private ModalidadePrecificacao modalidadePrecificacao;

    public Produto() {
    }

    public Produto(Long codigo, String nome, BigDecimal custo, BigDecimal precoVenda,
                   BigDecimal percentual, ModalidadePrecificacao modalidadePrecificacao) {
        this.codigo = codigo;
        this.nome = nome;
        this.custo = custo;
        this.precoVenda = precoVenda;
        this.percentual = percentual;
        this.modalidadePrecificacao = modalidadePrecificacao;
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getCusto() {
        return custo;
    }

    public void setCusto(BigDecimal custo) {
        this.custo = custo;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public void setPercentual(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public ModalidadePrecificacao getModalidadePrecificacao() {
        return modalidadePrecificacao;
    }

    public void setModalidadePrecificacao(ModalidadePrecificacao modalidadePrecificacao) {
        this.modalidadePrecificacao = modalidadePrecificacao;
    }
}
