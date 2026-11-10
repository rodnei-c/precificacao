package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.pg.PgFatura;
import br.com.modelagemdominio.rico.faturas.pg.PgFaturas;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadastroFaturaTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "null", "{}", "{\"itens\":[]}", "{\"itens\":[null]}",
    "{\"itens\":[{\"codigoProduto\":1.5,\"quantidade\":1}]}",
    "{\"itens\":[{\"codigoProduto\":9223372036854775808,\"quantidade\":1}]}",
    "{\"itens\":[{\"codigoProduto\":0,\"quantidade\":1}]}",
    "{\"itens\":[{\"quantidade\":1}]}",
    "{\"itens\":[{\"codigoProduto\":1}]}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":0}]}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":0.00001}]}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1000000000000000}]}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1},{\"codigoProduto\":1,\"quantidade\":2}]}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"desconto\":{}}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"desconto\":{\"tipo\":\"VALOR_FIXO\",\"valor\":1.001}}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"desconto\":{\"tipo\":\"PERCENTUAL\",\"valor\":100.0001}}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"codigoCliente\":1.5}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"codigoCliente\":9223372036854775808}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"codigoCliente\":0}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"formaPagamento\":\"CHEQUE\"}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"formaPagamento\":0}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1,\"precoVenda\":10}]}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}],\"total\":10}",
    "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}]} {}"
  })
  void rejeitaEntradaInvalidaAntesDeAcessarOBanco(String corpo) throws Exception {
    var api = AplicacaoRica.rotas(new DataSourceSemConexao());
    for (String caminho : List.of("/api/faturas", "/api/faturas/simulacao")) {
      var resposta = enviar(api, caminho, corpo, "application/json");
      assertEquals("400", resposta.head().iterator().next().split(" ", 3)[1], caminho);
      var problema = new JsonMapper().readTree(resposta.printBody());
      assertEquals(400, problema.path("status").asInt());
      assertEquals(caminho, problema.path("instance").asString());
    }
  }

  @Test
  void exigeJsonERecusaMetodoNaoAceito() throws Exception {
    var api = AplicacaoRica.rotas(new DataSourceSemConexao());
    for (String caminho : List.of("/api/faturas", "/api/faturas/simulacao")) {
      assertEquals("415", enviar(api, caminho, "{}", "text/plain").head().iterator().next().split(" ", 3)[1]);
    }
    var resposta = new RsPrint(api.act(new RqFake("DELETE", "/api/faturas")));
    assertEquals("405", resposta.head().iterator().next().split(" ", 3)[1]);
    assertTrue(java.util.stream.StreamSupport.stream(resposta.head().spliterator(), false)
      .anyMatch(cabecalho -> "Allow: GET, POST".equalsIgnoreCase(cabecalho)));
  }

  @Test
  void falhaDeConexaoRetorna503SemExporDetalhesInternos() throws Exception {
    var banco = new HikariDataSource();
    banco.close();
    var api = AplicacaoRica.rotas(banco);
    for (String caminho : List.of("/api/faturas", "/api/faturas/simulacao")) {
      var resposta = enviar(api, caminho,
        "{\"itens\":[{\"codigoProduto\":1,\"quantidade\":1}]}", "application/json");
      assertEquals("503", resposta.head().iterator().next().split(" ", 3)[1]);
      assertEquals("Não foi possível concluir a operação no banco de dados.",
        new JsonMapper().readTree(resposta.printBody()).path("detail").asString());
    }
  }

  @Test
  void rotaLiteralDaSimulacaoPrecedeAConsultaPorCodigo() throws Exception {
    var api = AplicacaoRica.rotas(new DataSourceSemConexao());
    var resposta = new RsPrint(api.act(new RqFake("GET", "/api/faturas/simulacao")));
    assertEquals("405", resposta.head().iterator().next().split(" ", 3)[1]);
    assertTrue(java.util.stream.StreamSupport.stream(resposta.head().spliterator(), false)
      .anyMatch(cabecalho -> "Allow: POST".equalsIgnoreCase(cabecalho)));
  }

  @Test
  void constroiObjetosSqlERejeitaCodigoInvalidoSemConexao() {
    var banco = new DataSourceSemConexao();
    var faturas = new PgFaturas(banco);
    assertDoesNotThrow(() -> new PgFaturas(banco, new PgProdutosConstantes(banco),
      new PgClientes(banco, new PgCidades(banco))));
    assertEquals(42, new PgFatura(banco, 42).codigo());
    assertEquals(FalhaFatura.Motivo.CODIGO_INVALIDO,
      assertThrows(FalhaFatura.class, () -> faturas.fatura(0)).motivo());
  }

  private static RsPrint enviar(Take api, String caminho, String corpo, String tipo) throws Exception {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    return new RsPrint(api.act(new RqFake(List.of("POST " + caminho + " HTTP/1.1",
      "Content-Type: " + tipo, "Content-Length: " + bytes.length), bytes)));
  }
}
