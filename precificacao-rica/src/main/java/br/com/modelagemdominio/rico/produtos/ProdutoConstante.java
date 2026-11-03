package br.com.modelagemdominio.rico.produtos;

public final class ProdutoConstante implements Produto {

    private final Produto origem;
    private final String nome;
    private final Precificacao precificacao;

    public ProdutoConstante(Produto origem, String nome, Precificacao precificacao) {
        this.origem = origem;
        this.nome = nome;
        this.precificacao = precificacao;
    }

    @Override
    public long codigo() {
        return this.origem.codigo();
    }

    @Override
    public String nome() {
        return this.nome;
    }

    @Override
    public Precificacao precificacao() {
        return this.precificacao;
    }
}
