package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.http.ProblemasHttp;
import br.com.modelagemdominio.rico.produtos.AlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        return this.json(new ProdutoResposta(produto));
    }

    Response lista(List<Produto> produtos) throws FalhaProduto {
        List<ProdutoResposta> dados = new ArrayList<>(produtos.size());
        for (Produto produto : produtos) {
            dados.add(new ProdutoResposta(produto));
        }
        return this.json(dados);
    }

    Response historico(List<AlteracaoPrecificacao> alteracoes) {
        return this.json(alteracoes.stream().map(AlteracaoResposta::new).toList());
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

    private record AlteracaoResposta(
        @JsonUnwrapped IdentidadeAlteracaoResposta identidade, LocalDateTime dataAlteracao,
        ValoresResposta anteriores, ValoresResposta novos
    ) {

        AlteracaoResposta(AlteracaoPrecificacao alteracao) {
            this(new IdentidadeAlteracaoResposta(alteracao.identidade()), alteracao.dataAlteracao(),
                new ValoresResposta(alteracao.anteriores()), new ValoresResposta(alteracao.novos()));
        }
    }

    private record IdentidadeAlteracaoResposta(long codigo, long codigoProduto) {

        IdentidadeAlteracaoResposta(AlteracaoPrecificacao.Identidade identidade) {
            this(identidade.codigo(), identidade.codigoProduto());
        }
    }

    private record ValoresResposta(BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual) {

        ValoresResposta(AlteracaoPrecificacao.Valores valores) {
            this(valores.custo(), valores.precoVenda(), valores.percentual());
        }
    }

    private record ProdutoResposta(
        long codigo,
        String nome,
        @JsonUnwrapped PrecificacaoResposta precificacao,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long codigoCategoria
    ) {

        ProdutoResposta(Produto produto) throws FalhaProduto {
            this(produto, produto.precificacao());
        }

        private ProdutoResposta(Produto produto, Precificacao precificacao) throws FalhaProduto {
            this(produto.codigo(), produto.nome(), new PrecificacaoResposta(precificacao),
                produto.codigoCategoria().map(CodigoCategoria::valor).orElse(null));
        }
    }

    private record PrecificacaoResposta(
        BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual, ModalidadePrecificacao modalidadePrecificacao
    ) {

        PrecificacaoResposta(Precificacao precificacao) {
            this(precificacao.custo().valor(), precificacao.precoVenda().valor(),
                precificacao.percentual().valor(), precificacao.modalidade());
        }
    }
}
