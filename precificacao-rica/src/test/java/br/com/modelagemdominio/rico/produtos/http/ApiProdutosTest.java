package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.AplicacaoRica;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integracao")
class ApiProdutosTest {

    private static final Pattern ENDERECO = Pattern.compile("http://127\\.0\\.0\\.1:\\d+");

    @TempDir
    Path temporario;

    @ParameterizedTest
    @CsvSource({"ACRESCIMO_SOBRE_CUSTO, 120.00", "MARGEM_SOBRE_VENDA, 125.00"})
    void cadastraProdutoEConsultaPeloLocation(String modalidade, String precoVenda) throws Exception {
        String nome = "Café d'origem " + UUID.randomUUID();
        Path log = this.temporario.resolve("api.log");
        Process api = iniciarApi(log);
        try {
            URI endereco = enderecoDaApi(api, log);
            HttpClient cliente = HttpClient.newHttpClient();
            JsonMapper json = new JsonMapper();
            HttpResponse<String> cadastro = cliente.send(
                HttpRequest.newBuilder(endereco.resolve("/api/produtos"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                            "nome": "  %s  ",
                            "custo": 100,
                            "percentual": 20,
                            "modalidadePrecificacao": "%s"
                        }
                        """.formatted(nome, modalidade)))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(201, cadastro.statusCode(), cadastro::body);
            var produto = json.readTree(cadastro.body());
            long codigo = produto.path("codigo").asLong();
            assertTrue(codigo > 0, cadastro::body);
            String location = cadastro.headers().firstValue("Location").orElseThrow();
            assertEquals("/api/produtos/" + codigo, location);
            assertEquals(json.readTree("""
                {
                    "codigo": %d,
                    "nome": "%s",
                    "custo": 100.00,
                    "precoVenda": %s,
                    "percentual": 20.0000,
                    "modalidadePrecificacao": "%s"
                }
                """.formatted(codigo, nome, precoVenda, modalidade)), produto);

            HttpResponse<String> consulta = cliente.send(
                HttpRequest.newBuilder(endereco.resolve(location))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(200, consulta.statusCode(), consulta::body);
            assertEquals(produto, json.readTree(consulta.body()));
        } finally {
            try {
                api.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            } finally {
                removerProduto(nome);
            }
        }
    }

    @Test
    void recalculaPelaModalidadeOriginalERegistraSomenteMudancas() throws Exception {
        String nome = "Produto para recálculo " + UUID.randomUUID();
        Path log = this.temporario.resolve("api-precificacao.log");
        Process api = iniciarApi(log);
        try {
            URI endereco = enderecoDaApi(api, log);
            HttpClient cliente = HttpClient.newHttpClient();
            JsonMapper json = new JsonMapper();
            var cadastro = enviarJson(cliente, endereco.resolve("/api/produtos"), "POST", """
                {"nome":"%s","custo":100,"percentual":20,"modalidadePrecificacao":"MARGEM_SOBRE_VENDA"}
                """.formatted(nome));
            assertEquals(201, cadastro.statusCode(), cadastro::body);
            long codigo = json.readTree(cadastro.body()).path("codigo").asLong();
            URI alteracao = endereco.resolve("/api/produtos/" + codigo + "/precificacao");

            var primeira = enviarJson(cliente, alteracao, "PATCH", """
                {"custo":100,"percentual":25}
                """);
            assertEquals(200, primeira.statusCode(), primeira::body);
            var esperado = json.readTree("""
                {
                    "codigo":%d,
                    "nome":"%s",
                    "custo":100.00,
                    "precoVenda":133.33,
                    "percentual":25.0000,
                    "modalidadePrecificacao":"MARGEM_SOBRE_VENDA"
                }
                """.formatted(codigo, nome));
            assertEquals(esperado, json.readTree(primeira.body()));

            var repeticao = enviarJson(cliente, alteracao, "PATCH", """
                {"custo":100.000,"percentual":25.00000}
                """);
            assertEquals(200, repeticao.statusCode(), repeticao::body);
            assertEquals(esperado, json.readTree(repeticao.body()));

            var invalida = enviarJson(cliente, alteracao, "PATCH", """
                {"custo":100,"precoVenda":140,"percentual":25}
                """);
            assertEquals(400, invalida.statusCode(), invalida::body);
            assertEquals("Informe exatamente dois valores entre custo, preço de venda e percentual.",
                json.readTree(invalida.body()).path("detail").asString());

            var ausente = enviarJson(cliente, endereco.resolve("/api/produtos/" + Long.MAX_VALUE + "/precificacao"),
                "PATCH", """
                    {"custo":100,"percentual":25}
                    """);
            assertEquals(404, ausente.statusCode(), ausente::body);
            assertEquals("Produto não encontrado.", json.readTree(ausente.body()).path("detail").asString());

            conferirHistorico(codigo);
        } finally {
            try {
                api.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            } finally {
                removerProduto(nome);
            }
        }
    }

    @Test
    void consultaHistoricoDoProdutoEmOrdemDecrescenteDeCodigo() throws Exception {
        String nome = "Produto para histórico " + UUID.randomUUID();
        Path log = this.temporario.resolve("api-historico.log");
        Process api = iniciarApi(log);
        try {
            URI endereco = enderecoDaApi(api, log);
            HttpClient cliente = HttpClient.newHttpClient();
            JsonMapper json = new JsonMapper();
            var cadastro = enviarJson(cliente, endereco.resolve("/api/produtos"), "POST", """
                {"nome":"%s","custo":100,"percentual":20,"modalidadePrecificacao":"ACRESCIMO_SOBRE_CUSTO"}
                """.formatted(nome));
            assertEquals(201, cadastro.statusCode(), cadastro::body);
            long codigo = json.readTree(cadastro.body()).path("codigo").asLong();
            URI produto = endereco.resolve("/api/produtos/" + codigo + "/");
            URI historico = produto.resolve("historico-precificacao");

            var vazio = consultar(cliente, historico);
            assertEquals(200, vazio.statusCode(), vazio::body);
            assertEquals(json.readTree("[]"), json.readTree(vazio.body()));

            var primeira = enviarJson(cliente, produto.resolve("precificacao"), "PATCH", """
                {"custo":110,"percentual":20}
                """);
            assertEquals(200, primeira.statusCode(), primeira::body);
            var segunda = enviarJson(cliente, produto.resolve("precificacao"), "PATCH", """
                {"custo":110,"percentual":25}
                """);
            assertEquals(200, segunda.statusCode(), segunda::body);

            var consulta = consultar(cliente, historico);
            assertEquals(200, consulta.statusCode(), consulta::body);
            var registros = json.readTree(consulta.body());
            assertTrue(registros.isArray());
            assertEquals(2, registros.size());
            long codigoPrimeira = registros.get(1).path("codigo").asLong();
            long codigoSegunda = registros.get(0).path("codigo").asLong();
            assertTrue(codigoPrimeira > 0 && codigoSegunda > codigoPrimeira);
            for (var registro : registros) {
                String data = registro.path("dataAlteracao").asString();
                assertEquals(0, LocalDateTime.parse(data).getNano() % 1000);
            }
            assertEquals(json.readTree("""
                [
                    {
                        "codigo":%d,"codigoProduto":%d,"dataAlteracao":"%s",
                        "anteriores":{"custo":110.00,"precoVenda":132.00,"percentual":20.0000},
                        "novos":{"custo":110.00,"precoVenda":137.50,"percentual":25.0000}
                    },
                    {
                        "codigo":%d,"codigoProduto":%d,"dataAlteracao":"%s",
                        "anteriores":{"custo":100.00,"precoVenda":120.00,"percentual":20.0000},
                        "novos":{"custo":110.00,"precoVenda":132.00,"percentual":20.0000}
                    }
                ]
                """.formatted(codigoSegunda, codigo, registros.get(0).path("dataAlteracao").asString(),
                    codigoPrimeira, codigo, registros.get(1).path("dataAlteracao").asString())), registros);

            var ausente = consultar(cliente,
                endereco.resolve("/api/produtos/" + Long.MAX_VALUE + "/historico-precificacao"));
            assertEquals(404, ausente.statusCode(), ausente::body);
        } finally {
            try {
                api.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            } finally {
                removerProduto(nome);
            }
        }
    }

    private static HttpResponse<String> consultar(HttpClient cliente, URI endereco) throws Exception {
        return cliente.send(HttpRequest.newBuilder(endereco)
            .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> enviarJson(HttpClient cliente, URI endereco, String metodo, String corpo)
        throws Exception {
        return cliente.send(HttpRequest.newBuilder(endereco)
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .method(metodo, HttpRequest.BodyPublishers.ofString(corpo))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void conferirHistorico(long codigo) throws SQLException {
        try (Connection conexao = conectar();
             var consulta = conexao.prepareStatement("""
                 SELECT custo_anterior, preco_venda_anterior, percentual_anterior,
                        custo_novo, preco_venda_novo, percentual_novo
                 FROM public.historico_precificacao WHERE produto_codigo = ?
                 """)) {
            consulta.setLong(1, codigo);
            try (var resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals("100.00", resultado.getBigDecimal("custo_anterior").toPlainString());
                assertEquals("125.00", resultado.getBigDecimal("preco_venda_anterior").toPlainString());
                assertEquals("20.0000", resultado.getBigDecimal("percentual_anterior").toPlainString());
                assertEquals("100.00", resultado.getBigDecimal("custo_novo").toPlainString());
                assertEquals("133.33", resultado.getBigDecimal("preco_venda_novo").toPlainString());
                assertEquals("25.0000", resultado.getBigDecimal("percentual_novo").toPlainString());
                assertFalse(resultado.next());
            }
        }
    }

    private Process iniciarApi(Path log) throws Exception {
        ProcessBuilder processo = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            AplicacaoRica.class.getName()
        );
        var ambiente = processo.environment();
        ambiente.put("RICO_DB_URL", System.getenv("RICO_BANCO_TESTE_URL"));
        ambiente.put("RICO_DB_USER", System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        ambiente.put("DB_PASSWORD", System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        ambiente.put("RICO_SERVER_PORT", "0");
        return processo.redirectErrorStream(true).redirectOutput(log.toFile()).start();
    }

    private URI enderecoDaApi(Process api, Path log) throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (api.isAlive() && System.nanoTime() < limite) {
            var endereco = ENDERECO.matcher(Files.readString(log));
            if (endereco.find()) {
                return URI.create(endereco.group());
            }
            Thread.sleep(50);
        }
        return fail("A API de teste não iniciou. " + Files.readString(log));
    }

    private static Connection conectar() throws SQLException {
        return DriverManager.getConnection(
            System.getenv("RICO_BANCO_TESTE_URL"),
            System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"),
            System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", "")
        );
    }

    private void removerProduto(String nome) throws SQLException {
        try (Connection conexao = conectar();
             var historico = conexao.prepareStatement("""
                 DELETE FROM public.historico_precificacao WHERE produto_codigo IN (
                     SELECT codigo FROM public.produto WHERE nome = ?
                 )
                 """);
             var produto = conexao.prepareStatement("DELETE FROM public.produto WHERE nome = ?")) {
            historico.setString(1, nome);
            historico.executeUpdate();
            produto.setString(1, nome);
            produto.executeUpdate();
        }
    }
}
