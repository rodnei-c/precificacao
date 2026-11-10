package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Produtos;
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

public final class TkListaProdutos implements TkRegex {

  private final Produtos produtos;
  private final RespostasProduto respostas;

  public TkListaProdutos(Produtos produtos) {
    this(produtos, new RespostasProduto(new JsonMapper()));
  }

  TkListaProdutos(Produtos produtos, RespostasProduto respostas) {
    this.produtos = produtos;
    this.respostas = respostas;
  }

  @Override
  public Response act(RqRegex requisicao) throws IOException {
    String caminho = requisicao.matcher().group();
    FiltrosProdutos filtros;
    try {
      filtros = filtros(URI.create(new RqRequestLine.Base(requisicao).uri()));
    } catch (IllegalArgumentException falha) {
      return this.respostas.problema(400, "Dados inválidos",
        "Confira a modalidade de precificação e o código da categoria nos filtros.", caminho);
    }
    try {
      return this.respostas.lista(this.produtos.listar(filtros));
    } catch (FalhaProduto falha) {
      return this.respostas.falha(falha, caminho);
    }
  }

  private static FiltrosProdutos filtros(URI endereco) {
    return new FiltrosProdutos(
      parametro(endereco, "nome"),
      parametro(endereco, "modalidadePrecificacao").map(String::trim).map(ModalidadePrecificacao::valueOf),
      parametro(endereco, "categoria").map(String::trim).map(Long::parseLong).map(CodigoCategoria::new)
    );
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
        return Optional.of(URLDecoder.decode(valor, StandardCharsets.UTF_8)).filter(texto -> !texto.isEmpty());
      }
    }
    return Optional.empty();
  }
}
