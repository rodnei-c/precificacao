package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;
import java.util.function.Function;

/**
 * Campo omitido no pedido; conserva o resultado recebido sem executar a alteração.
 *
 * @param <T> tipo do campo preservado
 */
public final class CampoPreservado<T> implements AlteracaoCampo<T> {

  @Override
  public <R> R aplicar(R preservado, Function<Optional<T>, R> informado) {
    return preservado;
  }
}
