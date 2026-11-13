package br.com.modelagemdominio.rico.produtos;

import java.util.Optional;

/** Condição de validade que identifica a regra violada, quando houver. */
interface EspecificacaoDePrecificacao {

  Optional<String> violacao();

  default EspecificacaoDePrecificacao e(EspecificacaoDePrecificacao outra) {
    return () -> this.violacao().or(outra::violacao);
  }

  default void exigir() {
    this.violacao().ifPresent(mensagem -> {
      throw new IllegalArgumentException(mensagem);
    });
  }
}
