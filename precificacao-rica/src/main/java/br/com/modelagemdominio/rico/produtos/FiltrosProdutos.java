package br.com.modelagemdominio.rico.produtos;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import java.util.Objects;
import java.util.Optional;

/** Filtros de produto; o trecho do nome é normalizado e valores em branco não restringem a busca. */
public record FiltrosProdutos(
  Optional<String> nome,
  Optional<ModalidadePrecificacao> modalidade,
  Optional<CodigoCategoria> categoria
) {

  /**
   * @param nome trecho opcional, sem espaços nas extremidades
   * @param modalidade modalidade opcional já interpretada pela borda de entrada
   * @param categoria referência opcional com código validado
   */
  public FiltrosProdutos {
    Objects.requireNonNull(nome, "O filtro de nome deve usar Optional.");
    Objects.requireNonNull(modalidade, "O filtro de modalidade deve usar Optional.");
    Objects.requireNonNull(categoria, "O filtro de categoria deve usar Optional.");
    nome = nome.map(String::strip).filter(valor -> !valor.isEmpty());
  }

  public FiltrosProdutos() {
    this(Optional.empty(), Optional.empty(), Optional.empty());
  }
}
