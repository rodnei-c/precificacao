package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
            Arguments.of("{}", "Informe o e-mail ou o telefone a ser atualizado."),
            Arguments.of("{\"email\":null}", "O campo email é obrigatório."),
            Arguments.of("{\"email\":\"invalido\"}", "Informe um e-mail válido."),
            Arguments.of("{\"email\":123}", "Confira o formato do JSON."),
            Arguments.of("{\"telefone\":123}", "Confira o formato do JSON."),
            Arguments.of("{\"telefone\":\"  \"}", "O campo telefone é obrigatório."),
            Arguments.of("{\"nome\":\"Outro nome\"}", "Confira o formato do JSON."),
            Arguments.of("{\"email\":\"novo@example.com\",\"cpf\":\"12345678909\"}", "Confira o formato do JSON.")
        );
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

    @Test
    @EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
    void atualizaRemoveEConservaTelefoneEmAlteracoesSimultaneas() throws Exception {
        var banco = new PGSimpleDataSource();
        banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        String sufixo = UUID.randomUUID().toString();
        String aplicacao = "teste-contatos-" + sufixo;
        banco.setApplicationName(aplicacao);
        Take api = AplicacaoRica.rotas(banco);
        String emailOriginal = "antes-" + sufixo + "@example.com";
        String emailIntermediario = "meio-" + sufixo + "@example.com";
        String emailFinal = "depois-" + sufixo + "@example.com";
        long codigo = this.corpo(201, enviar(api, "POST", "/api/clientes", """
            {"nome":"Cliente contatos","cpf":"11144477735","email":"%s"}
            """.formatted(emailOriginal))).path("codigo").asLong();

        try {
            JsonNode atualizado = this.alterar(200, api, codigo, """
                {"telefone":"  (41) 3333-4444  "}
                """);
            assertEquals("(41) 3333-4444", atualizado.path("telefone").asString());
            assertEquals(emailOriginal, atualizado.path("email").asString());

            JsonNode removido = this.alterar(200, api, codigo, """
                {"telefone":null}
                """);
            assertTrue(removido.path("telefone").isNull());
            assertEquals(emailOriginal, removido.path("email").asString());

            JsonNode ambos = this.alterar(200, api, codigo, """
                {"email":"%s","telefone":"(41) 98888-7777"}
                """.formatted(emailIntermediario));
            assertEquals(emailIntermediario, ambos.path("email").asString());
            assertEquals("(41) 98888-7777", ambos.path("telefone").asString());

            this.alterar(400, api, codigo, """
                {"email":"%s","telefone":"inválido"}
                """.formatted(emailFinal));
            assertEquals(ambos, this.consultar(api, codigo));

            this.atualizarEmParalelo(banco, aplicacao, api, codigo, emailFinal);
            var finalizado = this.consultar(api, codigo);
            assertEquals(emailFinal, finalizado.path("email").asString());
            assertEquals("+55 (41) 96666-5555", finalizado.path("telefone").asString());
            assertEquals("11144477735", finalizado.path("cpf").asString());
        } finally {
            try (var conexao = banco.getConnection();
                 var exclusao = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ?")) {
                exclusao.setLong(1, codigo);
                exclusao.executeUpdate();
            }
        }
    }

    private void atualizarEmParalelo(PGSimpleDataSource banco, String aplicacao, Take api, long codigo,
                                     String emailFinal) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try (var bloqueio = banco.getConnection(); var monitor = banco.getConnection()) {
            bloqueio.setAutoCommit(false);
            try {
                bloquearCliente(bloqueio, codigo);
                var email = executor.submit(() -> enviar(api, "PATCH", "/api/clientes/" + codigo, """
                    {"email":"%s"}
                    """.formatted(emailFinal)));
                var telefone = executor.submit(() -> enviar(api, "PATCH", "/api/clientes/" + codigo, """
                    {"telefone":"+55 (41) 96666-5555"}
                    """));

                aguardarAtualizacoesBloqueadas(monitor, aplicacao);
                bloqueio.commit();

                assertEquals(emailFinal, this.corpo(200, email.get(10, TimeUnit.SECONDS)).path("email").asString());
                assertEquals("+55 (41) 96666-5555",
                    this.corpo(200, telefone.get(10, TimeUnit.SECONDS)).path("telefone").asString());
            } finally {
                bloqueio.rollback();
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static void bloquearCliente(Connection conexao, long codigo) throws SQLException {
        try (var consulta = conexao.prepareStatement("SELECT codigo FROM public.cliente WHERE codigo = ? FOR UPDATE")) {
            consulta.setLong(1, codigo);
            try (var resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
            }
        }
    }

    private static void aguardarAtualizacoesBloqueadas(Connection monitor, String aplicacao) throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        try (var consulta = monitor.prepareStatement("""
            SELECT count(*) FROM pg_stat_activity
            WHERE datname = current_database() AND application_name = ?
                AND wait_event_type = 'Lock' AND query LIKE 'UPDATE public.cliente SET %'
            """)) {
            consulta.setString(1, aplicacao);
            while (System.nanoTime() < limite) {
                try (var resultado = consulta.executeQuery()) {
                    if (resultado.next() && resultado.getInt(1) == 2) {
                        return;
                    }
                }
                Thread.sleep(25);
            }
        }
        fail("As duas atualizações deveriam aguardar o bloqueio da mesma linha.");
    }

    private JsonNode consultar(Take api, long codigo) throws Exception {
        return this.corpo(200, new RsPrint(api.act(new RqFake("GET", "/api/clientes/" + codigo))));
    }

    private JsonNode alterar(int status, Take api, long codigo, String corpo) throws Exception {
        return this.corpo(status, enviar(api, "PATCH", "/api/clientes/" + codigo, corpo));
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
