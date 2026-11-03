package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.math.BigDecimal;
import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

final class RespostasProduto {

    private final JsonMapper json;

    RespostasProduto(JsonMapper json) {
        this.json = json;
    }

    Response produto(Produto produto) throws FalhaProduto {
        Precificacao precificacao = produto.precificacao();
        return new RsWithType(new RsWithBody(this.json.writeValueAsBytes(new ProdutoResposta(
            produto.codigo(), produto.nome(), precificacao.custo().valor(),
            precificacao.precoVenda().valor(), precificacao.percentual().valor(),
            precificacao.modalidade()
        ))), "application/json");
    }

    Response formatoInvalido(String caminho) {
        return problema(400, "Formato inválido",
            "Confira o JSON, os valores numéricos, a modalidade de precificação e o código da URL.", caminho);
    }

    Response falha(FalhaProduto falha, String caminho) {
        return switch (falha.motivo()) {
            case PRODUTO_AUSENTE -> problema(404,
                "Registro não encontrado", "Produto não encontrado.", caminho);
            case CODIGO_INVALIDO -> problema(400,
                "Dados inválidos", "O código do produto deve ser maior que zero.", caminho);
            case PERSISTENCIA_INDISPONIVEL -> problema(503,
                "Persistência indisponível", "Não foi possível concluir a operação no banco de dados.", caminho);
        };
    }

    Response problema(int status, String titulo, String detalhe, String caminho) {
        return new RsWithStatus(new RsWithType(
            new RsWithBody(this.json.writeValueAsBytes(new Problema(titulo, status, detalhe, caminho))),
            "application/problem+json"
        ), status);
    }

    private record ProdutoResposta(
        long codigo,
        String nome,
        BigDecimal custo,
        BigDecimal precoVenda,
        BigDecimal percentual,
        ModalidadePrecificacao modalidadePrecificacao
    ) {
    }

    private record Problema(String title, int status, String detail, String instance) {
    }
}
