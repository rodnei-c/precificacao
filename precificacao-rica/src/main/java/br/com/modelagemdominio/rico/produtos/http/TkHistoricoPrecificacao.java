package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.HistoricoDePrecificacao;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TkHistoricoPrecificacao implements TkRegex {

    private final HistoricoDePrecificacao historico;
    private final RespostasProduto respostas;

    public TkHistoricoPrecificacao(HistoricoDePrecificacao historico) {
        this(historico, new RespostasProduto(
            JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build()
        ));
    }

    TkHistoricoPrecificacao(HistoricoDePrecificacao historico, RespostasProduto respostas) {
        this.historico = historico;
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
            return this.respostas.historico(this.historico.doProduto(codigo));
        } catch (FalhaProduto falha) {
            return this.respostas.falha(falha, caminho);
        }
    }
}
