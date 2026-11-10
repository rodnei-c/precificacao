package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.io.IOException;
import java.util.Optional;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.misc.Href;
import org.takes.rq.RqHref;
import tools.jackson.databind.json.JsonMapper;

public final class TkListaProdutos implements TkRegex {

    private final Produtos produtos;
    private final RespostasProduto respostas;

    public TkListaProdutos(Produtos produtos) {
        this(produtos, new RespostasProduto(new JsonMapper()));
    }

    TkListaProdutos(Produtos produtos, RespostasProduto respostas) {
        this.produtos = produtos;
        this.respostas = respostas;
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        String caminho = requisicao.matcher().group();
        FiltrosProdutos filtros;
        try {
            filtros = filtros(new RqHref.Base(requisicao).href());
        } catch (IllegalArgumentException falha) {
            return this.respostas.problema(400, "Dados inválidos",
                "Confira a modalidade de precificação e o código da categoria nos filtros.", caminho);
        }
        try {
            return this.respostas.lista(this.produtos.listar(filtros));
        } catch (FalhaProduto falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private static FiltrosProdutos filtros(Href href) {
        return new FiltrosProdutos(
            parametro(href, "nome").map(String::strip).filter(nome -> !nome.isEmpty()),
            parametro(href, "modalidadePrecificacao").map(String::trim).map(ModalidadePrecificacao::valueOf),
            parametro(href, "categoria").map(String::trim).map(Long::parseLong).map(CodigoCategoria::new)
        );
    }

    private static Optional<String> parametro(Href href, String nome) {
        var valores = href.param(nome).iterator();
        if (!valores.hasNext()) {
            return Optional.empty();
        }
        return Optional.of(valores.next()).filter(valor -> !valor.isEmpty());
    }
}
