package br.com.modelagemdominio.rico;

import java.util.List;
import java.util.stream.Collectors;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rq.RqMethod;
import org.takes.rs.RsWithHeader;
import org.takes.rs.RsWithStatus;

/** Seleciona a operação HTTP depois que o caminho já foi reconhecido. */
final class RotaPorMetodo implements TkRegex {

  private final List<Metodo> metodos;
  private final String permitidos;

  RotaPorMetodo(Metodo... metodos) {
    this(List.of(metodos));
  }

  private RotaPorMetodo(List<Metodo> metodos) {
    this.metodos = metodos;
    this.permitidos = metodos.stream().map(Metodo::nome).collect(Collectors.joining(", "));
  }

  @Override
  public Response act(RqRegex requisicao) throws Exception {
    String solicitado = new RqMethod.Base(requisicao).method();
    for (Metodo metodo : this.metodos) {
      if (metodo.nome().equals(solicitado)) {
        return metodo.acao().act(requisicao);
      }
    }
    return new RsWithHeader(new RsWithStatus(405), "Allow", this.permitidos);
  }

  record Metodo(String nome, TkRegex acao) {
  }
}
