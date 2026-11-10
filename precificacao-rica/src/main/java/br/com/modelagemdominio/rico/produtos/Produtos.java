package br.com.modelagemdominio.rico.produtos;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.util.List;
import java.util.Optional;

public interface Produtos {

    Produto produto(long codigo) throws FalhaProduto;

    default List<Produto> listar() throws FalhaProduto {
        return this.listar(new FiltrosProdutos());
    }

    List<Produto> listar(FiltrosProdutos filtros) throws FalhaProduto;

    default Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao) throws FalhaProduto {
        return this.cadastrar(nome, precificacao, Optional.empty());
    }

    Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao,
                      Optional<CodigoCategoria> codigoCategoria) throws FalhaProduto;

    Produto vincularCategoria(long codigo, CodigoCategoria codigoCategoria) throws FalhaProduto;

    Produto alterarPrecificacao(long codigo, ValoresParaPrecificar valores) throws FalhaProduto, PrecificacaoInvalida;
}
