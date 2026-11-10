package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import tools.jackson.databind.json.JsonMapper;

public final class TkConsultaCliente implements TkRegex {

  private final Clientes clientes;
  private final RespostasCliente respostas;

  public TkConsultaCliente(Clientes clientes) {
    this.clientes = clientes;
    this.respostas = new RespostasCliente(new JsonMapper());
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
      return this.respostas.cliente(this.clientes.cliente(codigo));
    } catch (FalhaCliente falha) {
      return this.respostas.falha(falha, caminho);
    }
  }
}
