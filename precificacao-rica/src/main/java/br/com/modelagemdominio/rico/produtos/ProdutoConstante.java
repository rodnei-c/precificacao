package br.com.modelagemdominio.rico.produtos;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import java.util.Optional;

public final class ProdutoConstante implements Produto {

    private final Produto origem;
    private final String nome;
    private final Precificacao precificacao;

    private final Optional<CodigoCategoria> codigoCategoria;

    public ProdutoConstante(Produto origem, String nome, Precificacao precificacao,
                            Optional<CodigoCategoria> codigoCategoria) {
        this.origem = origem;
        this.nome = nome;
        this.precificacao = precificacao;
        this.codigoCategoria = codigoCategoria;
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

    @Override
    public Optional<CodigoCategoria> codigoCategoria() {
        return this.codigoCategoria;
    }
}
