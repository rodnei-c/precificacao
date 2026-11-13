package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricoPrecificacaoTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"custo\":100,\"percentual\":20,\"modalidadePrecificacao\":\"MARGEM_SOBRE_VENDA\"}",
    "{\"custo\":100,\"percentual\":20,\"codigoCategoria\":1}"
  })
  void patchNaoAceitaTrocaDeModalidadeOuCategoria(String corpo) throws Exception {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    var api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();
    var resposta = api.act(new RqFake(List.of(
      "PATCH /api/produtos/1/precificacao HTTP/1.1",
      "Content-Type: application/json", "Content-Length: " + bytes.length
    ), bytes));

    assertEquals(400, status(resposta.head().iterator().next()));
  }

  @Test
  void rejeitaCodigoInvalidoAntesDeConsultarOBanco() throws Exception {
    var api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();

    for (String codigo : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
      var resposta = api.act(new RqFake("GET", "/api/produtos/" + codigo + "/historico-precificacao"));
      assertEquals(400, status(resposta.head().iterator().next()));
    }
  }

  @Test
  void informaIndisponibilidadeEmVezDeRetornarHistoricoVazio() throws Exception {
    var banco = new HikariDataSource();
    banco.close();
    String caminho = "/api/produtos/1/historico-precificacao";

    var resposta = new RsPrint(new RotasDaAplicacaoRica(banco).rotas().act(new RqFake("GET", caminho)));

    assertEquals(503, status(resposta.head().iterator().next()));
    assertEquals(caminho, new JsonMapper().readTree(resposta.printBody()).path("instance").asString());
  }

  private static int status(String linha) {
    return Integer.parseInt(linha.split(" ", 3)[1]);
  }
}
