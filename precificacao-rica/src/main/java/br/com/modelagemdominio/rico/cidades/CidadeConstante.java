package br.com.modelagemdominio.rico.cidades;

public record CidadeConstante(long codigo, NomeCidade nome, UnidadeFederativa uf) implements Cidade {
}
