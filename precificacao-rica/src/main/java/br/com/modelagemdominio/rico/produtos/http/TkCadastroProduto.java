package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.http.CorpoJson;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithHeader;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TkCadastroProduto implements TkRegex {

    private final Produtos produtos;
    private final JsonMapper json;
    private final RespostasProduto respostas;

    public TkCadastroProduto(Produtos produtos) {
        this(produtos, JsonMapper.builder().enable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        ).enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS).build());
    }

    public TkCadastroProduto(Produtos produtos, JsonMapper json) {
        this.produtos = produtos;
        this.json = json;
        this.respostas = new RespostasProduto(json);
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        String caminho = requisicao.matcher().group();
        CorpoJson corpo = new CorpoJson(requisicao);
        if (!corpo.temTipoJson()) {
            return new RsWithStatus(415);
        }

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
        NomeProduto nome;
        Optional<CodigoCategoria> codigoCategoria;
        PrecificacaoParaCadastro precificacao;
        try {
            nome = entrada.nomeDoProduto();
            codigoCategoria = entrada.categoria();
            precificacao = entrada.precificacao();
        } catch (PrecificacaoInvalida | IllegalArgumentException falha) {
            return this.respostas.problema(400, "Dados inválidos", falha.getMessage(), caminho);
        }

        try {
            Produto produto = this.produtos.cadastrar(nome, precificacao, codigoCategoria);
            return new RsWithHeader(
                new RsWithStatus(this.respostas.produto(produto), 201),
                "Location", "/api/produtos/" + produto.codigo()
            );
        } catch (FalhaProduto falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private record Cadastro(
        String nome,
        BigDecimal custo,
        BigDecimal precoVenda,
        BigDecimal percentual,
        ModalidadePrecificacao modalidadePrecificacao,
        BigDecimal codigoCategoria
    ) {

        NomeProduto nomeDoProduto() {
            return new NomeProduto(this.nome);
        }

        Optional<CodigoCategoria> categoria() {
            return Optional.ofNullable(this.codigoCategoria).map(CodigoCategoria::new);
        }

        PrecificacaoParaCadastro precificacao() throws PrecificacaoInvalida {
            var valores = new ValoresParaPrecificar(this.custo, this.precoVenda, this.percentual);
            return valores.precificacao(this.modalidadePrecificacao);
        }
    }
}
