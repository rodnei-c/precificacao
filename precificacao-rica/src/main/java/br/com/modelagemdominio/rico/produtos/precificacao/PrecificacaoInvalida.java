package br.com.modelagemdominio.rico.produtos.precificacao;

public final class PrecificacaoInvalida extends Exception {

    public PrecificacaoInvalida(String mensagem) {
        super(mensagem);
    }
}
