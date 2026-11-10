package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;
import java.util.function.Function;

/**
 * Remoção solicitada explicitamente; aplica a alteração com ausência de valor.
 *
 * @param <T> tipo do campo removido
 */
public final class CampoRemovido<T> implements AlteracaoCampo<T> {

  @Override
  public <R> R aplicar(R preservado, Function<Optional<T>, R> informado) {
    return informado.apply(Optional.empty());
  }
}
