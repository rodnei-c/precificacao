package br.com.modelagemdominio.rico.categorias;

import java.util.List;

public interface Categorias {

    Categoria cadastrar(NomeCategoria nome) throws FalhaCategoria;

    Categoria renomear(CodigoCategoria codigo, NomeCategoria nome) throws FalhaCategoria;

    List<Categoria> listar() throws FalhaCategoria;

    boolean existe(CodigoCategoria codigo) throws FalhaCategoria;
}
