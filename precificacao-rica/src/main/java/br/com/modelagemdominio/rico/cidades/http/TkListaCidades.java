package br.com.modelagemdominio.rico.cidades.http;

import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.SiglaUf;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
      var endereco = URI.create(new RqRequestLine.Base(requisicao).uri());
      filtros = new FiltrosCidades(
        parametro(endereco, "nome").map(NomeCidade::new),
        parametro(endereco, "uf").map(SiglaUf::new).map(SiglaUf::unidade)
      );
    } catch (IllegalArgumentException falha) {
      return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
    }
    try {
      return this.respostas.lista(this.cidades.listar(filtros));
    } catch (FalhaCidade falha) {
      return this.respostas.falha(falha, caminho);
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
        return Optional.of(URLDecoder.decode(valor, StandardCharsets.UTF_8)).filter(texto -> !texto.isBlank());
      }
    }
    return Optional.empty();
  }
}
