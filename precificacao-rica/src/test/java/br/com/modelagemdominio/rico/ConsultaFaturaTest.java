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

class ConsultaFaturaTest {

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808"})
  void rejeitaCodigoInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
    var api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();
    String caminho = "/api/faturas/" + codigo;

    var resposta = new RsPrint(api.act(new RqFake("GET", caminho)));

    assertEquals("400", resposta.head().iterator().next().split(" ", 3)[1]);
    var problema = new JsonMapper().readTree(resposta.printBody());
    assertEquals("Informe um código de fatura inteiro positivo válido.", problema.path("detail").asString());
    assertEquals(caminho, problema.path("instance").asString());
  }

  @Test
  void retorna503GenericoQuandoOBancoNaoPodeSerConsultado() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    var resposta = new RsPrint(new RotasDaAplicacaoRica(banco).rotas().act(new RqFake("GET", "/api/faturas/1")));

    assertEquals("503", resposta.head().iterator().next().split(" ", 3)[1]);
    assertEquals(new JsonMapper().readTree("""
      {"title":"Persistência indisponível","status":503,
       "detail":"Não foi possível concluir a operação no banco de dados.","instance":"/api/faturas/1"}
      """), new JsonMapper().readTree(resposta.printBody()));
  }
}
