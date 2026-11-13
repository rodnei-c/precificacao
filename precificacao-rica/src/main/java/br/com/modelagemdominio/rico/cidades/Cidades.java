package br.com.modelagemdominio.rico.cidades;

import java.util.List;
import java.util.Optional;

public interface Cidades {

    Cidade cadastrar(NomeCidade nome, UnidadeFederativa uf) throws FalhaCidade;

    default List<Cidade> listar() throws FalhaCidade {
        return this.listar(Optional.empty());
    }

    default List<Cidade> listar(Optional<NomeCidade> nome) throws FalhaCidade {
        return this.listar(new FiltrosCidades(nome, Optional.empty()));
    }

    List<Cidade> listar(FiltrosCidades filtros) throws FalhaCidade;

    boolean existe(CodigoCidade codigo) throws FalhaCidade;
}
