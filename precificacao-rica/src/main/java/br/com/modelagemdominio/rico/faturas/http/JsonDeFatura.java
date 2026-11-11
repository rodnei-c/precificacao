package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

final class JsonDeFatura {

  private final JsonMapper json;

  JsonDeFatura() {
    this.json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
      .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();
  }

  PedidoFatura pedido(CorpoJson corpo) throws IOException {
    return this.json.readValue(corpo.bytes(), PedidoFatura.class);
  }

  RespostasFatura respostas() {
    return new RespostasFatura(this.json);
  }
}
