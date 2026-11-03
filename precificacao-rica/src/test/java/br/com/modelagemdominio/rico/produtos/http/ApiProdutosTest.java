package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.AplicacaoRica;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
class ApiProdutosTest {

    private static final Pattern ENDERECO = Pattern.compile("http://127\\.0\\.0\\.1:\\d+");

    @TempDir
    Path temporario;

    @Test
    void cadastraProdutoEConsultaPeloLocation() throws Exception {
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
                            "modalidadePrecificacao": "MARGEM_SOBRE_VENDA"
                        }
                        """.formatted(nome)))
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
                    "precoVenda": 125.00,
                    "percentual": 20.0000,
                    "modalidadePrecificacao": "MARGEM_SOBRE_VENDA"
                }
                """.formatted(codigo, nome)), produto);

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

    private void removerProduto(String nome) throws SQLException {
        try (var conexao = DriverManager.getConnection(
                 System.getenv("RICO_BANCO_TESTE_URL"),
                 System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"),
                 System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", "")
             );
             var exclusao = conexao.prepareStatement("DELETE FROM public.produto WHERE nome = ?")) {
            exclusao.setString(1, nome);
            exclusao.executeUpdate();
        }
    }
}
