package br.com.modelagemdominio.rico.categorias.http;

import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class NomeCategoriaJson {

    private static final JsonMapper JSON = JsonMapper.builder().enable(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
    ).build();

    private final CorpoJson corpo;

    NomeCategoriaJson(CorpoJson corpo) {
        this.corpo = corpo;
    }

    NomeCategoria nome() throws IOException {
        Entrada entrada = JSON.readValue(this.corpo.bytes(), Entrada.class);
        if (entrada == null) {
            throw new IOException("O corpo deve conter um objeto JSON.");
        }
        return new NomeCategoria(entrada.nome());
    }

    private record Entrada(String nome) {
    }
}
