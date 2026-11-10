package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.http.ProblemasHttp;
import br.com.modelagemdominio.rico.produtos.AlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

final class RespostasProduto {

    private final JsonMapper json;
    private final ProblemasHttp problemas;

    RespostasProduto(JsonMapper json) {
        this.json = json;
        this.problemas = new ProblemasHttp(json);
    }

    Response produto(Produto produto) throws FalhaProduto {
        return this.json(dadosDoProduto(produto));
    }

    Response lista(List<Produto> produtos) throws FalhaProduto {
        List<ProdutoResposta> dados = new ArrayList<>(produtos.size());
        for (Produto produto : produtos) {
            dados.add(dadosDoProduto(produto));
        }
        return this.json(dados);
    }

    Response historico(List<AlteracaoPrecificacao> alteracoes) {
        return this.json(alteracoes);
    }

    private static ProdutoResposta dadosDoProduto(Produto produto) throws FalhaProduto {
        Precificacao precificacao = produto.precificacao();
        return new ProdutoResposta(
            produto.codigo(), produto.nome(), precificacao.custo().valor(),
            precificacao.precoVenda().valor(), precificacao.percentual().valor(),
            precificacao.modalidade(), produto.codigoCategoria().map(CodigoCategoria::valor).orElse(null)
        );
    }

    private Response json(Object dados) {
        return new RsWithType(new RsWithBody(this.json.writeValueAsBytes(dados)), "application/json");
    }

    Response formatoInvalido(String caminho) {
        return problema(400, "Formato inválido",
            "Confira o JSON, os valores numéricos, a modalidade de precificação e o código da URL.", caminho);
    }

    Response falha(FalhaProduto falha, String caminho) {
        return switch (falha.motivo()) {
            case PRODUTO_AUSENTE -> problema(404,
                "Registro não encontrado", "Produto não encontrado.", caminho);
            case CATEGORIA_AUSENTE -> problema(404,
                "Registro não encontrado", "Categoria não encontrada.", caminho);
            case CODIGO_INVALIDO -> problema(400,
                "Dados inválidos", "O código do produto deve ser maior que zero.", caminho);
            case PERSISTENCIA_INDISPONIVEL -> problema(503,
                "Persistência indisponível", "Não foi possível concluir a operação no banco de dados.", caminho);
        };
    }

    Response problema(int status, String titulo, String detalhe, String caminho) {
        return this.problemas.resposta(status, titulo, detalhe, caminho);
    }

    private record ProdutoResposta(
        long codigo,
        String nome,
        BigDecimal custo,
        BigDecimal precoVenda,
        BigDecimal percentual,
        ModalidadePrecificacao modalidadePrecificacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long codigoCategoria
    ) {
    }
}
