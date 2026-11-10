package br.com.modelagemdominio.rico.produtos;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import java.util.Optional;

public interface Produto {

    long codigo();

    String nome() throws FalhaProduto;

    Precificacao precificacao() throws FalhaProduto;

    Optional<CodigoCategoria> codigoCategoria() throws FalhaProduto;
}
