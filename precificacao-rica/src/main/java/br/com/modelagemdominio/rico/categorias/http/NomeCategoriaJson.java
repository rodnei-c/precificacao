package br.com.modelagemdominio.rico.categorias.http;

import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

record NomeCategoriaJson(String nome) {

    private static final JsonMapper JSON = JsonMapper.builder().enable(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
    ).build();

    static NomeCategoria ler(CorpoJson corpo) throws IOException {
        NomeCategoriaJson entrada = JSON.readValue(corpo.bytes(), NomeCategoriaJson.class);
        if (entrada == null) {
            throw new IOException("O corpo deve conter um objeto JSON.");
        }
        return new NomeCategoria(entrada.nome());
    }
}
