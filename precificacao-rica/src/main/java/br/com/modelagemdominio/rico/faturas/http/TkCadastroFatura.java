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

/** Adapta o JSON do cadastro para os objetos do domínio, sem fórmulas de faturamento. */
public final class TkCadastroFatura implements TkRegex {

  private final Faturas faturas;
  private final Produtos produtos;
  private final JsonDeFatura json;
  private final RespostasFatura respostas;

  /**
   * @param faturas coleção de destino
   * @param produtos referências dos itens
   */
  public TkCadastroFatura(Faturas faturas, Produtos produtos) {
    this.faturas = faturas;
    this.produtos = produtos;
    this.json = new JsonDeFatura();
    this.respostas = this.json.respostas();
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
      entrada = this.json.pedido(corpo);
    } catch (JacksonException | IOException falha) {
      return this.respostas.formatoInvalido(caminho);
    }
    if (entrada == null) {
      return this.respostas.formatoInvalido(caminho);
    }
    try {
      var calculo = entrada.calculo(this.produtos);
      var vinculos = entrada.vinculos();
      return this.respostas.cadastrada(this.faturas.cadastrar(calculo, vinculos));
    } catch (IllegalArgumentException falha) {
      return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
    } catch (FalhaFatura falha) {
      return this.respostas.falha(falha, caminho);
    }
  }

}
