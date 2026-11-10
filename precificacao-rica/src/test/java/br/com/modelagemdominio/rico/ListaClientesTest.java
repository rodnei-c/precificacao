package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.clientes.AlteracaoCliente;
import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.http.TkListaClientes;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.takes.Take;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaClientesTest {

  @Test
  void devolveArrayVazioQuandoNaoHaClientes() throws Exception {
    Clientes vazios = new Clientes() {
      @Override
      public Cliente cadastrar(IdentificacaoCliente identificacao, ContatoCliente contato,
                  Optional<Endereco> endereco) {
        throw new AssertionError("A listagem não deve cadastrar um cliente.");
      }

      @Override
      public Cliente alterar(long codigo, AlteracaoCliente alteracao) {
        throw new AssertionError("A listagem não deve alterar o e-mail de um cliente.");
      }

      @Override
      public Cliente cliente(long codigo) {
        throw new AssertionError("A listagem não deve consultar cada cliente pelo código.");
      }

      @Override
      public List<Cliente> listar() {
        return List.of();
      }
    };
    Take api = new TkFork(new FkRegex("/api/clientes", new TkListaClientes(vazios)));

    var resposta = consultar(api);

    assertStatus(200, resposta);
    assertTrue(resposta.printHead().lines().anyMatch("Content-Type: application/json"::equalsIgnoreCase));
    var json = new JsonMapper();
    assertEquals(json.createArrayNode(), json.readTree(resposta.printBody()));
  }

  @Test
  void informaIndisponibilidadeEmVezDeDevolverListaVazia() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    var resposta = consultar(AplicacaoRica.rotas(banco));

    assertStatus(503, resposta);
    var problema = new JsonMapper().readTree(resposta.printBody());
    assertEquals("Persistência indisponível", problema.path("title").asString());
    assertEquals("/api/clientes", problema.path("instance").asString());
  }

  private static RsPrint consultar(Take api) throws Exception {
    return new RsPrint(api.act(new RqFake("GET", "/api/clientes")));
  }

  private static void assertStatus(int status, RsPrint resposta) throws IOException {
    assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
  }
}
