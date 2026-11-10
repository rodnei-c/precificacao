package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Substituição solicitada com um novo valor; remoção usa CampoRemovido explicitamente.
 *
 * @param <T> tipo do novo valor
 */
public final class CampoSubstituido<T> implements AlteracaoCampo<T> {

  private final T novoValor;

  /** @param novoValor valor não nulo que substitui o anterior */
  public CampoSubstituido(T novoValor) {
    this.novoValor = Objects.requireNonNull(novoValor);
  }

  @Override
  public <R> R aplicar(R preservado, Function<Optional<T>, R> informado) {
    return informado.apply(Optional.of(this.novoValor));
  }
}
