package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlteracaoClienteTest {

  private final JsonMapper json = new JsonMapper();

  @ParameterizedTest
  @MethodSource("alteracoesInvalidas")
  void rejeitaCorpoInvalidoAntesDeAcessarOBanco(String corpo, String detalhe) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    assertEquals(detalhe, this.problema(400, api, "1", corpo).path("detail").asString());
  }

  private static Stream<Arguments> alteracoesInvalidas() {
    return Stream.of(
      Arguments.of("", "Confira o formato do JSON."),
      Arguments.of("{}", "Informe o e-mail, telefone ou endereço a ser atualizado."),
      Arguments.of("{\"email\":null}", "O campo email é obrigatório."),
      Arguments.of("{\"email\":\"invalido\"}", "Informe um e-mail válido."),
      Arguments.of("{\"email\":\"" + "x".repeat(243) + "@example.com\"}",
        "O campo email deve conter texto válido de até 254 caracteres."),
      Arguments.of("{\"email\":\"novo@example.com\"} {}", "Confira o formato do JSON."),
      Arguments.of("{\"email\":123}", "Confira o formato do JSON."),
      Arguments.of("{\"telefone\":123}", "Confira o formato do JSON."),
      Arguments.of("{\"telefone\":\"  \"}", "O campo telefone é obrigatório."),
      Arguments.of("{\"nome\":\"Outro nome\"}", "Confira o formato do JSON."),
      Arguments.of("{\"email\":\"novo@example.com\",\"cpf\":\"12345678909\"}", "Confira o formato do JSON."),
      Arguments.of("{\"email\":\"novo@example.com\",\"telefone\":\"inválido\"}",
        "Informe um telefone fixo ou celular no formato brasileiro, com DDD."),
      Arguments.of("{\"email\":\"invalido\",\"telefone\":\"(41) 3333-4444\"}", "Informe um e-mail válido.")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{}", "{\"cep\":\"80000000\",\"logradouro\":\"Rua\",\"numero\":\"1\",\"bairro\":\"Centro\"}",
    "{\"cep\":\"80000-000\"}",
    "{\"cep\":\"123\",\"logradouro\":\"Rua\",\"numero\":\"1\",\"bairro\":\"Centro\",\"codigoCidade\":1}",
    "{\"cep\":\"80000000\",\"logradouro\":\"Rua\",\"numero\":\"1\",\"bairro\":\"Centro\",\"codigoCidade\":1.5}",
    "{\"cep\":\"80000000\",\"logradouro\":\"Rua\",\"numero\":\"1\",\"bairro\":\"Centro\",\"codigoCidade\":0}",
    "{\"cep\":\"80000000\",\"logradouro\":\"Rua\",\"numero\":\"1\",\"bairro\":\"Centro\",\"codigoCidade\":9223372036854775808}",
    "{\"cep\":\"80000000\",\"logradouro\":\"Rua\",\"numero\":123,\"bairro\":\"Centro\",\"codigoCidade\":1}",
    "{\"cep\":\"80000000\",\"logradouro\":\"Rua\",\"numero\":\"1\",\"bairro\":\"Centro\",\"codigoCidade\":1,\"extra\":true}",
    "[]", "\"Rua\""
  })
  void rejeitaEnderecoInvalidoSemGravarContatos(String endereco) throws Exception {
    this.problema(400, AplicacaoRica.rotas(new DataSourceSemConexao()), "1",
      "{\"email\":\"novo@example.com\",\"telefone\":\"(41) 3333-4444\",\"endereco\":" + endereco + "}");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "abc"})
  void rejeitaCodigoInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    JsonNode problema = this.problema(400, api, codigo, "{\"email\":\"novo@example.com\"}");

    assertEquals("Informe um código de cliente inteiro positivo válido.", problema.path("detail").asString());
  }

  @Test
  void informaIndisponibilidadeAoAlterarEmail() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    JsonNode problema = this.problema(503, AplicacaoRica.rotas(banco), "1", "{\"email\":\"novo@example.com\"}");

    assertEquals("Persistência indisponível", problema.path("title").asString());
  }

  private JsonNode problema(int status, Take api, String codigo, String corpo) throws Exception {
    return this.corpo(status, enviar(api, "PATCH", "/api/clientes/" + codigo, corpo));
  }

  private static RsPrint enviar(Take api, String metodo, String caminho, String corpo) throws Exception {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    return new RsPrint(api.act(new RqFake(List.of(
      metodo + " " + caminho + " HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + bytes.length
    ), bytes)));
  }

  private JsonNode corpo(int status, RsPrint resposta) throws Exception {
    assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
    return this.json.readTree(resposta.printBody());
  }
}
