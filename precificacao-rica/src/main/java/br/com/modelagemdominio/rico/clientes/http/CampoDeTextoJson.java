package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.clientes.AlteracaoCampo;
import br.com.modelagemdominio.rico.clientes.CampoPreservado;
import br.com.modelagemdominio.rico.clientes.CampoRemovido;
import br.com.modelagemdominio.rico.clientes.CampoSubstituido;
import java.util.Optional;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;

/** Interpreta a presença e o valor de um campo textual no PATCH do cliente. */
final class CampoDeTextoJson {

  private final JsonNode campo;

  CampoDeTextoJson(JsonNode campo) {
    this.campo = campo;
  }

  boolean formatoAceito() {
    return this.campo.isMissingNode() || this.campo.isNull() || this.campo.isString();
  }

  <T> Optional<T> alteracaoDeValorObrigatorio(Function<String, T> interpretar) {
    if (this.campo.isMissingNode()) {
      return Optional.empty();
    }
    return Optional.of(interpretar.apply(this.campo.isNull() ? null : this.campo.asString()));
  }

  <T> AlteracaoCampo<T> alteracaoOpcional(Function<String, T> interpretar) {
    if (this.campo.isMissingNode()) {
      return new CampoPreservado<>();
    }
    if (this.campo.isNull()) {
      return new CampoRemovido<>();
    }
    return new CampoSubstituido<>(interpretar.apply(this.campo.asString()));
  }
}
