package br.com.precificacao.anemico;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Inicia o servidor completo em porta aleatória, usando somente o banco de teste informado.
 */
@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
class AplicacaoPostgresTest {

  @Test
  void iniciaServidorEVerificaOperacoesDeProduto() throws Exception {
    SpringApplication aplicacao = new SpringApplication(AplicacaoAnemica.class);
    aplicacao.addInitializers(contexto -> contexto.getEnvironment().getPropertySources()
      .addFirst(new MapPropertySource("conexao-exclusiva-de-teste", Map.of(
        "spring.datasource.url", System.getenv("BANCO_TESTE_URL"),
        "spring.datasource.username", System.getenv("BANCO_TESTE_USUARIO"),
        "spring.datasource.password", System.getenv("BANCO_TESTE_SENHA"),
        "server.port", "0",
        "server.address", "127.0.0.1"
      ))));

    try (ConfigurableApplicationContext contexto = aplicacao.run()) {
      String endereco = "http://127.0.0.1:"
        + contexto.getEnvironment().getProperty("local.server.port");
      HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      JsonMapper json = contexto.getBean(JsonMapper.class);

      HttpResponse<String> cadastro = enviar(cliente, endereco, """
        {
          "nome": "Produto integrado",
          "precoVenda": 125.00,
          "percentual": 20.0000,
          "modalidadePrecificacao": "MARGEM_SOBRE_VENDA"
        }
        """);
      assertEquals(201, cadastro.statusCode(), cadastro.body());
      assertEquals(0, new BigDecimal("100.00").compareTo(
        json.readTree(cadastro.body()).get("custo").decimalValue()
      ));

      URI consulta = URI.create(endereco + cadastro.headers().firstValue("Location").orElseThrow());
      HttpResponse<String> produto = cliente.send(
        HttpRequest.newBuilder(consulta).timeout(Duration.ofSeconds(10)).GET().build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );
      assertEquals(200, produto.statusCode(), produto.body());
      assertEquals("Produto integrado", json.readTree(produto.body()).get("nome").stringValue());

      URI precificacao = URI.create(consulta + "/precificacao");
      HttpResponse<String> alteracao = alterar(cliente, precificacao, """
        {"custo": 120, "percentual": 20}
        """);
      assertEquals(200, alteracao.statusCode(), alteracao.body());
      assertEquals(0, new BigDecimal("150.00").compareTo(
        json.readTree(alteracao.body()).get("precoVenda").decimalValue()
      ));
      assertEquals(
        "MARGEM_SOBRE_VENDA",
        json.readTree(alteracao.body()).get("modalidadePrecificacao").stringValue()
      );

      HttpResponse<String> alteracaoInvalida = alterar(cliente, precificacao, """
        {"custo": 200, "precoVenda": 240, "percentual": 20}
        """);
      assertEquals(400, alteracaoInvalida.statusCode(), alteracaoInvalida.body());

      HttpResponse<String> produtoAtualizado = cliente.send(
        HttpRequest.newBuilder(consulta).timeout(Duration.ofSeconds(10)).GET().build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );
      assertEquals(200, produtoAtualizado.statusCode(), produtoAtualizado.body());
      assertEquals(json.readTree(alteracao.body()), json.readTree(produtoAtualizado.body()));

      HttpResponse<String> produtoAusente = alterar(
        cliente, URI.create(endereco + "/api/produtos/9223372036854775807/precificacao"), """
        {"custo": 120, "percentual": 20}
        """
      );
      assertEquals(404, produtoAusente.statusCode(), produtoAusente.body());

      HttpResponse<String> modalidadeNumerica = enviar(cliente, endereco, """
        {"nome":"Inválido","custo":100,"percentual":20,"modalidadePrecificacao":0}
        """);
      assertEquals(400, modalidadeNumerica.statusCode(), modalidadeNumerica.body());

      HttpResponse<String> campoDesconhecido = enviar(cliente, endereco, """
        {
          "nome":"Inválido","custo":100,"percentual":20,
          "modalidadePrecificacao":"ACRESCIMO_SOBRE_CUSTO","campoInexistente":1
        }
        """);
      assertEquals(400, campoDesconhecido.statusCode(), campoDesconhecido.body());
    }
  }

  private HttpResponse<String> alterar(HttpClient cliente, URI precificacao, String corpo)
    throws Exception {
    HttpRequest requisicao = HttpRequest.newBuilder(precificacao)
      .timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .method("PATCH", HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build();
    return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private HttpResponse<String> enviar(HttpClient cliente, String endereco, String corpo)
    throws Exception {
    HttpRequest requisicao = HttpRequest.newBuilder(URI.create(endereco + "/api/produtos"))
      .timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build();
    return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }
}
