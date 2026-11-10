package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;
import java.util.function.Function;

/**
 * Operação sobre um campo opcional: preservar, remover ou substituir.
 *
 * @param <T> tipo do valor aceito pelo campo
 */
public interface AlteracaoCampo<T> {

  /**
   * @param preservado resultado mantido quando o campo foi omitido
   * @param informado operação executada somente para remoção ou substituição
   * @param <R> tipo do resultado da operação
   * @return resultado preservado ou produzido pela operação informada
   */
  <R> R aplicar(R preservado, Function<Optional<T>, R> informado);

  /** @return se a operação foi solicitada, inclusive para remoção do valor */
  default boolean informada() {
    return this.aplicar(false, valor -> true);
  }

  /** @return novo valor na substituição; ausência na preservação ou remoção */
  default Optional<T> valor() {
    return this.aplicar(Optional.empty(), Function.identity());
  }
}
