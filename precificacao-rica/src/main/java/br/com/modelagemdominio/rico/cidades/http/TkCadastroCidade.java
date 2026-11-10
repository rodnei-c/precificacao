package br.com.modelagemdominio.rico.cidades.http;

import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TkCadastroCidade implements TkRegex {

    private final Cidades cidades;
    private final JsonMapper json;
    private final RespostasCidade respostas;

    public TkCadastroCidade(Cidades cidades) {
        this.cidades = cidades;
        this.json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        this.respostas = new RespostasCidade(this.json);
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        CorpoJson corpo = new CorpoJson(requisicao);
        if (!corpo.temTipoJson()) {
            return new RsWithStatus(415);
        }
        String caminho = requisicao.matcher().group();
        Cadastro entrada;
        try {
            entrada = this.json.readValue(corpo.bytes(), Cadastro.class);
        } catch (JacksonException | IOException falha) {
            return this.respostas.formatoInvalido(caminho);
        }
        if (entrada == null) {
            return this.respostas.formatoInvalido(caminho);
        }
        return this.cadastrar(entrada, caminho);
    }

    private Response cadastrar(Cadastro entrada, String caminho) {
        NomeCidade nome;
        UnidadeFederativa uf;
        try {
            nome = new NomeCidade(entrada.nome());
            uf = UnidadeFederativa.daSigla(entrada.uf());
        } catch (IllegalArgumentException falha) {
            return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
        }
        try {
            return this.respostas.cadastrada(this.cidades.cadastrar(nome, uf));
        } catch (FalhaCidade falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private record Cadastro(String nome, String uf) {
    }
}
