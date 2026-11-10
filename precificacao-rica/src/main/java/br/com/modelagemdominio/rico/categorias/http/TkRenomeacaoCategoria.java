package br.com.modelagemdominio.rico.categorias.http;

import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public final class TkRenomeacaoCategoria implements TkRegex {

    private final Categorias categorias;
    private final RespostasCategoria respostas;

    public TkRenomeacaoCategoria(Categorias categorias) {
        this.categorias = categorias;
        this.respostas = new RespostasCategoria(new JsonMapper());
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        CorpoJson corpo = new CorpoJson(requisicao);
        if (!corpo.temTipoJson()) {
            return new RsWithStatus(415);
        }
        String caminho = requisicao.matcher().group();
        CodigoCategoria codigo;
        NomeCategoria nome;
        try {
            codigo = new CodigoCategoria(Long.parseLong(requisicao.matcher().group(1)));
            nome = NomeCategoriaJson.ler(corpo);
        } catch (JacksonException | IOException | NumberFormatException falha) {
            return this.respostas.formatoInvalido(caminho);
        } catch (IllegalArgumentException falha) {
            return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
        }
        try {
            return this.respostas.categoria(this.categorias.renomear(codigo, nome));
        } catch (FalhaCategoria falha) {
            return this.respostas.falha(falha, caminho);
        }
    }
}
