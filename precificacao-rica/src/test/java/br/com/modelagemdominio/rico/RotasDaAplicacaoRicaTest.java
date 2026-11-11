package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotasDaAplicacaoRicaTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "GET|/api/faturas/simulacao|POST",
    "PATCH|/api/faturas/1|GET",
    "DELETE|/api/faturas|GET, POST",
    "DELETE|/api/produtos/1/historico-precificacao|GET",
    "DELETE|/api/produtos/1/precificacao|PATCH",
    "DELETE|/api/produtos/1/categoria|PATCH",
    "DELETE|/api/produtos/1|GET",
    "DELETE|/api/produtos|GET, POST",
    "DELETE|/api/categorias/1|PATCH",
    "DELETE|/api/categorias|GET, POST",
    "DELETE|/api/cidades|GET, POST",
    "DELETE|/api/clientes/1|GET, PATCH",
    "DELETE|/api/clientes|GET, POST"
  })
  void informaMetodosPermitidosSemAbrirConexao(String metodo, String caminho, String permitidos) throws Exception {
    var api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();

    var resposta = new RsPrint(api.act(new RqFake(metodo, caminho)));

    assertEquals("405", resposta.head().iterator().next().split(" ", 3)[1]);
    assertTrue(resposta.printHead().lines().anyMatch(("Allow: " + permitidos)::equalsIgnoreCase));
  }
}
