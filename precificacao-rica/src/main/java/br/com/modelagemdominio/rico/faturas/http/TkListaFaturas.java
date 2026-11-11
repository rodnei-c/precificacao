package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import br.com.modelagemdominio.rico.http.ParametrosDaConsulta;
import java.io.IOException;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rq.RqRequestLine;

/** Adapta os filtros da URL e devolve os resumos sem recalcular faturas. */
public final class TkListaFaturas implements TkRegex {

  private final Faturas faturas;
  private final RespostasFatura respostas;

  /** @param faturas coleção de origem, sem consulta durante a construção */
  public TkListaFaturas(Faturas faturas) {
    this.faturas = faturas;
    this.respostas = new JsonDeFatura().respostas();
  }

  @Override
  public Response act(RqRegex requisicao) throws IOException {
    String caminho = requisicao.matcher().group();
    FiltrosFaturas filtros;
    try {
      var parametros = new ParametrosDaConsulta(new RqRequestLine.Base(requisicao).uri());
      filtros = new FiltrosDeFaturasNaUrl(parametros).interpretados();
    } catch (FaturaInvalida falha) {
      return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
    } catch (IllegalArgumentException falha) {
      return this.respostas.dadosInvalidos("Confira os parâmetros de consulta.", caminho);
    }
    try {
      return this.respostas.lista(this.faturas.listar(filtros));
    } catch (FalhaFatura falha) {
      return this.respostas.falha(falha, caminho);
    }
  }
}
