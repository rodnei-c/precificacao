package br.com.modelagemdominio.rico.cidades;

import java.util.Objects;

public record CidadeConstante(CodigoCidade identidade, NomeCidade nome, UnidadeFederativa uf) implements Cidade {

  public CidadeConstante(long codigo, NomeCidade nome, UnidadeFederativa uf) {
    this(new CodigoCidade(codigo), nome, uf);
  }

  public CidadeConstante {
    Objects.requireNonNull(identidade);
    Objects.requireNonNull(nome);
    Objects.requireNonNull(uf);
  }

  @Override
  public long codigo() {
    return this.identidade.valor();
  }
}
