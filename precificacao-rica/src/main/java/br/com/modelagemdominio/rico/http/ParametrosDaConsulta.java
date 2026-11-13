package br.com.modelagemdominio.rico.http;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Lê a primeira ocorrência de um parâmetro da URL e decodifica seu valor em UTF-8. */
public final class ParametrosDaConsulta {

  private final URI endereco;

  public ParametrosDaConsulta(String endereco) {
    this(URI.create(endereco));
  }

  private ParametrosDaConsulta(URI endereco) {
    this.endereco = endereco;
  }

  public Optional<String> primeiro(String nome) {
    String consulta = this.endereco.getRawQuery();
    if (consulta == null) {
      return Optional.empty();
    }
    for (String parte : consulta.split("&")) {
      String[] campo = parte.split("=", 2);
      if (nome.equals(URLDecoder.decode(campo[0], StandardCharsets.UTF_8))) {
        String valor = campo.length == 2 ? campo[1] : "";
        return Optional.of(URLDecoder.decode(valor, StandardCharsets.UTF_8)).filter(texto -> !texto.isEmpty());
      }
    }
    return Optional.empty();
  }
}
