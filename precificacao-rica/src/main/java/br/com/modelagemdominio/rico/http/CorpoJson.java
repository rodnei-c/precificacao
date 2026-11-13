package br.com.modelagemdominio.rico.http;

import java.io.IOException;
import java.util.Locale;
import org.takes.Request;
import org.takes.rq.RqChunk;
import org.takes.rq.RqHeaders;
import org.takes.rq.RqLengthAware;

public final class CorpoJson {

    private final Request requisicao;

    public CorpoJson(Request requisicao) {
        this.requisicao = requisicao;
    }

    public boolean temTipoJson() throws IOException {
        String tipo = new RqHeaders.Smart(this.requisicao).single("content-type", "")
            .split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        return tipo.equals("application/json") || (tipo.startsWith("application/") && tipo.endsWith("+json"));
    }

    public byte[] bytes() throws IOException {
        RqHeaders.Smart cabecalhos = new RqHeaders.Smart(this.requisicao);
        if ("chunked".equalsIgnoreCase(cabecalhos.single("transfer-encoding", ""))) {
            return new RqChunk(this.requisicao).body().readAllBytes();
        }
        if (!cabecalhos.single("content-length", "").isEmpty()) {
            return new RqLengthAware(this.requisicao).body().readAllBytes();
        }
        throw new IOException("A requisição não informa como ler o corpo.");
    }
}
