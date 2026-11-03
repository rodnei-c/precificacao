package br.com.modelagemdominio.rico.produtos;

import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;

public interface Produtos {

    Produto produto(long codigo) throws FalhaProduto;

    Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao) throws FalhaProduto;
}
