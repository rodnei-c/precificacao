package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListaFaturasTest {

  @ParameterizedTest
  @ValueSource(strings = {"inicio=2026-02-30", "fim=31-12-2026", "inicio=2026-11-04&fim=2026-11-03",
    "inicio=0000-01-01", "fim=%2B10000-01-01", "codigoCliente=0", "codigoCliente=-1",
    "codigoCliente=1.5", "codigoCliente=abc", "codigoCliente=9223372036854775808",
    "fim=2026-11-01T00%3A00%3A00", "inicio=2026-13-01", "codigoCliente=1%20OR%201%3D1"})
  void rejeitaFiltrosInvalidosAntesDeAcessarOBanco(String consulta) throws Exception {
    var resposta = new RsPrint(new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas()
      .act(new RqFake("GET", "/api/faturas?" + consulta)));

    assertEquals("400", resposta.head().iterator().next().split(" ", 3)[1]);
    var problema = new JsonMapper().readTree(resposta.printBody());
    assertEquals(400, problema.path("status").asInt());
    assertEquals("/api/faturas", problema.path("instance").asString());
  }

  @Test
  void falhaDeConexaoRetorna503Generico() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    var resposta = new RsPrint(new RotasDaAplicacaoRica(banco).rotas().act(new RqFake("GET", "/api/faturas")));

    assertEquals("503", resposta.head().iterator().next().split(" ", 3)[1]);
    assertEquals("Não foi possível concluir a operação no banco de dados.",
      new JsonMapper().readTree(resposta.printBody()).path("detail").asString());
  }
}
