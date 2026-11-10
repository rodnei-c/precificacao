package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.http.CorpoJson;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.io.IOException;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Adapta o pedido compartilhado para uma simulação sem persistência. */
public final class TkSimulacaoFatura implements TkRegex {

  private final Faturas faturas;
  private final Produtos produtos;
  private final JsonMapper json;
  private final RespostasFatura respostas;

  /**
   * @param faturas coleção responsável pela preparação sem gravação
   * @param produtos referências dos itens
   */
  public TkSimulacaoFatura(Faturas faturas, Produtos produtos) {
    this.faturas = faturas;
    this.produtos = produtos;
    this.json = PedidoFatura.json();
    this.respostas = new RespostasFatura(this.json);
  }

  @Override
  public Response act(RqRegex requisicao) throws IOException {
    var corpo = new CorpoJson(requisicao);
    if (!corpo.temTipoJson()) {
      return new RsWithStatus(415);
    }
    String caminho = requisicao.matcher().group();
    PedidoFatura entrada;
    try {
      entrada = this.json.readValue(corpo.bytes(), PedidoFatura.class);
    } catch (JacksonException | IOException falha) {
      return this.respostas.formatoInvalido(caminho);
    }
    if (entrada == null) {
      return this.respostas.formatoInvalido(caminho);
    }
    try {
      var calculo = entrada.calculo(this.produtos);
      var vinculos = entrada.vinculos();
      return this.respostas.simulacao(this.faturas.simular(calculo, vinculos));
    } catch (IllegalArgumentException falha) {
      return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
    } catch (FalhaFatura falha) {
      return this.respostas.falha(falha, caminho);
    }
  }
}
