package br.com.modelagemdominio.rico.produtos;

public interface Produto {

    long codigo();

    String nome() throws FalhaProduto;

    Precificacao precificacao() throws FalhaProduto;
}
