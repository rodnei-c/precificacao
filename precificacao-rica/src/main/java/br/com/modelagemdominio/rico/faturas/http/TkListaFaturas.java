package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
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
    this.respostas = new RespostasFatura(PedidoFatura.json());
  }

  @Override
  public Response act(RqRegex requisicao) throws IOException {
    String caminho = requisicao.matcher().group();
    FiltrosFaturas filtros;
    try {
      var endereco = URI.create(new RqRequestLine.Base(requisicao).uri());
      filtros = new FiltrosFaturas(parametro(endereco, "inicio").map(TkListaFaturas::data),
        parametro(endereco, "fim").map(TkListaFaturas::data),
        parametro(endereco, "codigoCliente").map(TkListaFaturas::codigo));
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

  private static LocalDate data(String valor) {
    try {
      return LocalDate.parse(valor);
    } catch (DateTimeParseException falha) {
      throw new FaturaInvalida("Informe as datas no formato AAAA-MM-DD.");
    }
  }

  private static long codigo(String valor) {
    try {
      return Long.parseLong(valor);
    } catch (NumberFormatException falha) {
      throw new FaturaInvalida("Informe um código de cliente inteiro positivo válido.");
    }
  }

  private static Optional<String> parametro(URI endereco, String nome) {
    String consulta = endereco.getRawQuery();
    if (consulta == null) {
      return Optional.empty();
    }
    for (String parte : consulta.split("&")) {
      String[] campo = parte.split("=", 2);
      if (nome.equals(URLDecoder.decode(campo[0], StandardCharsets.UTF_8))) {
        String valor = campo.length == 2 ? campo[1] : "";
        return Optional.of(URLDecoder.decode(valor, StandardCharsets.UTF_8).strip()).filter(texto -> !texto.isEmpty());
      }
    }
    return Optional.empty();
  }
}
