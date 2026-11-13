package br.com.modelagemdominio.rico.http;

import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

public final class ProblemasHttp {

    private final JsonMapper json;

    public ProblemasHttp(JsonMapper json) {
        this.json = json;
    }

    public Response resposta(int status, String titulo, String detalhe, String caminho) {
        return new RsWithStatus(new RsWithType(
            new RsWithBody(this.json.writeValueAsBytes(new Problema(titulo, status, detalhe, caminho))),
            "application/problem+json"
        ), status);
    }

    private record Problema(String title, int status, String detail, String instance) {
    }
}
