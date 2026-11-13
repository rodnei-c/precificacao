package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.http.CorpoJson;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.io.IOException;
import java.math.BigDecimal;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TkVinculoCategoria implements TkRegex {

    private final Produtos produtos;
    private final JsonMapper json;
    private final RespostasProduto respostas;

    public TkVinculoCategoria(Produtos produtos) {
        this(produtos, JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build());
    }

    TkVinculoCategoria(Produtos produtos, JsonMapper json) {
        this.produtos = produtos;
        this.json = json;
        this.respostas = new RespostasProduto(json);
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        CorpoJson corpo = new CorpoJson(requisicao);
        if (!corpo.temTipoJson()) {
            return new RsWithStatus(415);
        }

        String caminho = requisicao.matcher().group();
        long codigo;
        Alteracao entrada;
        try {
            codigo = Long.parseLong(requisicao.matcher().group(1));
            entrada = this.json.readValue(corpo.bytes(), Alteracao.class);
        } catch (NumberFormatException | JacksonException | IOException falha) {
            return this.respostas.formatoInvalido(caminho);
        }
        if (entrada == null) {
            return this.respostas.formatoInvalido(caminho);
        }
        return this.vincular(codigo, entrada, caminho);
    }

    private Response vincular(long codigo, Alteracao entrada, String caminho) {
        try {
            var categoria = new CodigoCategoria(entrada.codigoCategoria());
            return this.respostas.produto(this.produtos.vincularCategoria(codigo, categoria));
        } catch (IllegalArgumentException falha) {
            return this.respostas.problema(400, "Dados inválidos", falha.getMessage(), caminho);
        } catch (FalhaProduto falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private record Alteracao(BigDecimal codigoCategoria) {
    }
}
