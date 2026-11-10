package br.com.modelagemdominio.rico.cidades.http;

import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import java.io.IOException;
import java.util.Optional;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.misc.Href;
import org.takes.rq.RqHref;
import tools.jackson.databind.json.JsonMapper;

public final class TkListaCidades implements TkRegex {

    private final Cidades cidades;
    private final RespostasCidade respostas;

    public TkListaCidades(Cidades cidades) {
        this.cidades = cidades;
        this.respostas = new RespostasCidade(new JsonMapper());
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        String caminho = requisicao.matcher().group();
        FiltrosCidades filtros;
        try {
            var endereco = new RqHref.Base(requisicao).href();
            filtros = new FiltrosCidades(
                parametro(endereco, "nome").map(NomeCidade::new),
                parametro(endereco, "uf").map(UnidadeFederativa::daSigla)
            );
        } catch (IllegalArgumentException falha) {
            return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
        }
        try {
            return this.respostas.lista(this.cidades.listar(filtros));
        } catch (FalhaCidade falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private static Optional<String> parametro(Href endereco, String nome) {
        var valores = endereco.param(nome).iterator();
        if (!valores.hasNext()) {
            return Optional.empty();
        }
        return Optional.of(valores.next()).filter(valor -> !valor.isBlank());
    }
}
