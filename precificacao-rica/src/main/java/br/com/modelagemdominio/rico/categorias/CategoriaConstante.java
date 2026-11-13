package br.com.modelagemdominio.rico.categorias;

import java.util.Objects;

public record CategoriaConstante(CodigoCategoria identidade, NomeCategoria nome) implements Categoria {

  public CategoriaConstante(long codigo, NomeCategoria nome) {
    this(new CodigoCategoria(codigo), nome);
  }

  public CategoriaConstante {
    Objects.requireNonNull(identidade);
    Objects.requireNonNull(nome);
  }

  @Override
  public long codigo() {
    return this.identidade.valor();
  }
}
