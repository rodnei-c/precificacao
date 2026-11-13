package br.com.modelagemdominio.rico.categorias.http;

import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import java.io.IOException;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import tools.jackson.databind.json.JsonMapper;

public final class TkListaCategorias implements TkRegex {

    private final Categorias categorias;
    private final RespostasCategoria respostas;

    public TkListaCategorias(Categorias categorias) {
        this(categorias, new RespostasCategoria(new JsonMapper()));
    }

    TkListaCategorias(Categorias categorias, RespostasCategoria respostas) {
        this.categorias = categorias;
        this.respostas = respostas;
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        try {
            return this.respostas.lista(this.categorias.listar());
        } catch (FalhaCategoria falha) {
            return this.respostas.falha(falha, requisicao.matcher().group());
        }
    }
}
