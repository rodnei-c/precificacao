package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.Precificacao;

public final class PrecificacaoParaCadastro {

    private final Precificacao precificacao;

    PrecificacaoParaCadastro(Precificacao precificacao) {
        this.precificacao = precificacao;
    }

    public Precificacao precificacao() {
        return this.precificacao;
    }
}
