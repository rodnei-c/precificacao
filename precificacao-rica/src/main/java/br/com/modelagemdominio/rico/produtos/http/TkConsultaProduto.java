package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import tools.jackson.databind.json.JsonMapper;

public final class TkConsultaProduto implements TkRegex {

    private final Produtos produtos;
    private final RespostasProduto respostas;

    public TkConsultaProduto(Produtos produtos) {
        this(produtos, new JsonMapper());
    }

    public TkConsultaProduto(Produtos produtos, JsonMapper json) {
        this(produtos, new RespostasProduto(json));
    }

    TkConsultaProduto(Produtos produtos, RespostasProduto respostas) {
        this.produtos = produtos;
        this.respostas = respostas;
    }

    @Override
    public Response act(RqRegex requisicao) {
        String caminho = requisicao.matcher().group();
        long codigo;
        try {
            codigo = Long.parseLong(requisicao.matcher().group(1));
        } catch (NumberFormatException falha) {
            return this.respostas.formatoInvalido(caminho);
        }
        try {
            return this.respostas.produto(this.produtos.produto(codigo));
        } catch (FalhaProduto falha) {
            return this.respostas.falha(falha, caminho);
        }
    }
}
