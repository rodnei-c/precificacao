package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.http.ParametrosDaConsulta;
import java.io.IOException;
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
      var parametros = new ParametrosDaConsulta(new RqRequestLine.Base(requisicao).uri());
      filtros = new FiltrosDeProdutosNaUrl(parametros).interpretados();
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
}
