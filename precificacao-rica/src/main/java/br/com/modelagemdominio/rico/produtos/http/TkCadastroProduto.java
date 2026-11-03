package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.AcrescimoSobreCusto;
import br.com.modelagemdominio.rico.produtos.precificacao.MargemSobreVenda;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.Precificacoes;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import org.takes.Request;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rq.RqChunk;
import org.takes.rq.RqHeaders;
import org.takes.rq.RqLengthAware;
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
    private final Map<ModalidadePrecificacao, Precificacoes> modalidades;

    public TkCadastroProduto(Produtos produtos) {
        this(produtos, JsonMapper.builder().enable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        ).enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS).build());
    }

    public TkCadastroProduto(Produtos produtos, JsonMapper json) {
        this(produtos, json, new RespostasProduto(json), Map.of(
            ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO, new Precificacoes(new AcrescimoSobreCusto()),
            ModalidadePrecificacao.MARGEM_SOBRE_VENDA, new Precificacoes(new MargemSobreVenda())
        ));
    }

    TkCadastroProduto(Produtos produtos, JsonMapper json, RespostasProduto respostas,
                      Map<ModalidadePrecificacao, Precificacoes> modalidades) {
        this.produtos = produtos;
        this.json = json;
        this.respostas = respostas;
        this.modalidades = modalidades;
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        String caminho = requisicao.matcher().group();
        RqHeaders.Smart cabecalhos = new RqHeaders.Smart(requisicao);
        String tipo = cabecalhos.single("content-type", "")
            .split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!tipo.equals("application/json") && !(tipo.startsWith("application/") && tipo.endsWith("+json"))) {
            return new RsWithStatus(415);
        }

        Request corpo;
        if ("chunked".equalsIgnoreCase(cabecalhos.single("transfer-encoding", ""))) {
            corpo = new RqChunk(requisicao);
        } else if (!cabecalhos.single("content-length", "").isEmpty()) {
            corpo = new RqLengthAware(requisicao);
        } else {
            return this.respostas.formatoInvalido(caminho);
        }
        Cadastro entrada;
        try {
            entrada = this.json.readValue(corpo.body().readAllBytes(), Cadastro.class);
        } catch (JacksonException | IOException falha) {
            return this.respostas.formatoInvalido(caminho);
        }
        if (entrada == null) {
            return this.respostas.formatoInvalido(caminho);
        }

        NomeProduto nome;
        PrecificacaoParaCadastro precificacao;
        try {
            nome = new NomeProduto(entrada.nome());
            precificacao = precificacao(entrada);
        } catch (PrecificacaoInvalida | IllegalArgumentException falha) {
            return this.respostas.problema(400, "Dados inválidos", falha.getMessage(), caminho);
        }

        try {
            Produto produto = this.produtos.cadastrar(nome, precificacao);
            return new RsWithHeader(
                new RsWithStatus(this.respostas.produto(produto), 201),
                "Location", "/api/produtos/" + produto.codigo()
            );
        } catch (FalhaProduto falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private PrecificacaoParaCadastro precificacao(Cadastro entrada) throws PrecificacaoInvalida {
        if (entrada.modalidadePrecificacao() == null) {
            throw new PrecificacaoInvalida("Informe a modalidade de precificação.");
        }
        BigDecimal custo = entrada.custo();
        BigDecimal venda = entrada.precoVenda();
        BigDecimal percentual = entrada.percentual();
        int informados = (custo == null ? 0 : 1) + (venda == null ? 0 : 1) + (percentual == null ? 0 : 1);
        if (informados != 2) {
            throw new PrecificacaoInvalida(
                "Informe exatamente dois valores entre custo, preço de venda e percentual."
            );
        }
        Precificacoes calculo = this.modalidades.get(entrada.modalidadePrecificacao());
        if (venda == null) {
            return calculo.comCustoEPercentual(custo, percentual);
        }
        if (custo == null) {
            return calculo.comVendaEPercentual(venda, percentual);
        }
        return calculo.comCustoEVenda(custo, venda);
    }

    private record Cadastro(
        String nome,
        BigDecimal custo,
        BigDecimal precoVenda,
        BigDecimal percentual,
        ModalidadePrecificacao modalidadePrecificacao
    ) {
    }
}
