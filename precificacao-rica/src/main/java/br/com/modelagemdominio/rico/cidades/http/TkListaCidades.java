package br.com.modelagemdominio.rico.cidades.http;

import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.http.ParametrosDaConsulta;
import java.io.IOException;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rq.RqRequestLine;
import tools.jackson.databind.json.JsonMapper;

public final class TkListaCidades implements TkRegex {

  private final Cidades cidades;
  private final RespostasCidade respostas;

  public TkListaCidades(Cidades cidades) {
    this.cidades = cidades;
    this.respostas = new RespostasCidade(new JsonMapper());
  }

  @Override
  public Response act(RqRegex requisicao) throws IOException {
    String caminho = requisicao.matcher().group();
    FiltrosCidades filtros;
    try {
      var parametros = new ParametrosDaConsulta(new RqRequestLine.Base(requisicao).uri());
      filtros = new FiltrosDeCidadesNaUrl(parametros).interpretados();
    } catch (IllegalArgumentException falha) {
      return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
    }
    try {
      return this.respostas.lista(this.cidades.listar(filtros));
    } catch (FalhaCidade falha) {
      return this.respostas.falha(falha, caminho);
    }
  }
}
