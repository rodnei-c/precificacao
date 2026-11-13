package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Faturas;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/** Consulta a coleção uma vez e adapta os valores materializados para HTTP. */
public final class TkConsultaFatura implements TkRegex {

  private final Faturas faturas;
  private final RespostasFatura respostas;

  /** @param faturas coleção consultada; a construção não acessa o banco */
  public TkConsultaFatura(Faturas faturas) {
    this.faturas = faturas;
    this.respostas = new RespostasFatura(JsonMapper.builder()
      .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build());
  }

  @Override
  public Response act(RqRegex requisicao) {
    String caminho = requisicao.matcher().group();
    long codigo;
    try {
      codigo = Long.parseLong(requisicao.matcher().group(1));
    } catch (NumberFormatException falha) {
      return this.respostas.codigoInvalido(caminho);
    }
    try {
      return this.respostas.fatura(this.faturas.fatura(codigo));
    } catch (FalhaFatura falha) {
      return this.respostas.falha(falha, caminho);
    }
  }
}
