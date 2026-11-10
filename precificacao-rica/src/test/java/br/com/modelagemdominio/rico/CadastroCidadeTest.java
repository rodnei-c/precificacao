package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadastroCidadeTest {

    private final JsonMapper json = new JsonMapper();

    @Test
    @EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
    void normalizaUfEPermiteOMesmoNomeSomenteEmUfDistinta() throws Exception {
        var banco = new PGSimpleDataSource();
        banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        Take api = AplicacaoRica.rotas(banco);
        String nome = "São João d'Aliança " + UUID.randomUUID();

        try {
            JsonNode primeira = this.cidadeCadastrada(api, "  " + nome + "  ", " pr ", nome, "PR");

            JsonNode repeticao = this.resposta(400, enviar(api, this.corpo(nome.toLowerCase(Locale.ROOT), "PR")));
            assertEquals("Já existe uma cidade com esse nome na UF informada.", repeticao.path("detail").asString());

            JsonNode outra = this.cidadeCadastrada(api, nome, "go", nome, "GO");
            assertNotEquals(primeira.path("codigo").asLong(), outra.path("codigo").asLong());
        } finally {
            removerCidades(banco, nome);
        }
    }

    @ParameterizedTest
    @MethodSource("dadosInvalidos")
    void validaNomeEUfAntesDeAbrirConexao(String corpo, String detalhe) throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        assertEquals(detalhe, this.resposta(400, enviar(api, corpo)).path("detail").asString());
    }

    private static Stream<Arguments> dadosInvalidos() {
        return Stream.of(
            Arguments.of("""
                {"nome":"   ","uf":"PR"}
                """, "Informe o nome da cidade."),
            Arguments.of("""
                {"nome":"Curitiba"}
                """, "Informe a UF da cidade."),
            Arguments.of("""
                {"nome":"Curitiba","uf":"XX"}
                """, "Informe uma sigla de UF brasileira válida.")
        );
    }

    @Test
    void recusaNomeAcimaDoLimiteAntesDeAbrirConexao() throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        var problema = this.resposta(400, enviar(api, this.corpo("x".repeat(201), "PR")));

        assertEquals("O nome da cidade deve ter no máximo 200 caracteres.", problema.path("detail").asString());
    }

    @Test
    void recusaCampoExtraAntesDeAbrirConexao() throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        var problema = this.resposta(400, enviar(api, """
            {"nome":"Curitiba","uf":"PR","codigo":1}
            """));

        assertEquals("Formato inválido", problema.path("title").asString());
    }

    @Test
    void informaQuandoNaoConsegueAbrirConexao() throws Exception {
        var banco = new HikariDataSource();
        banco.close();

        var problema = this.resposta(503, enviar(AplicacaoRica.rotas(banco), this.corpo("Curitiba", "PR")));

        assertEquals("Persistência indisponível", problema.path("title").asString());
    }

    private JsonNode cidadeCadastrada(Take api, String nome, String uf, String nomeEsperado, String ufEsperada)
        throws Exception {
        RsPrint resposta = enviar(api, this.corpo(nome, uf));
        assertTrue(resposta.printHead().lines().anyMatch("Content-Type: application/json"::equalsIgnoreCase));
        JsonNode cidade = this.resposta(201, resposta);
        long codigo = cidade.path("codigo").asLong();
        assertTrue(codigo > 0);
        assertEquals(3, cidade.size());
        assertEquals(nomeEsperado, cidade.path("nome").asString());
        assertEquals(ufEsperada, cidade.path("uf").asString());
        return cidade;
    }

    private String corpo(String nome, String uf) {
        return this.json.writeValueAsString(this.json.createObjectNode().put("nome", nome).put("uf", uf));
    }

    private static RsPrint enviar(Take api, String corpo) throws Exception {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        return new RsPrint(api.act(new RqFake(List.of(
            "POST /api/cidades HTTP/1.1",
            "Content-Type: application/json",
            "Content-Length: " + bytes.length
        ), bytes)));
    }

    private JsonNode resposta(int status, RsPrint resposta) throws Exception {
        assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
        return this.json.readTree(resposta.printBody());
    }

    private static void removerCidades(PGSimpleDataSource banco, String nome) throws SQLException {
        try (var conexao = banco.getConnection();
             var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE LOWER(nome) = LOWER(?)")) {
            exclusao.setString(1, nome);
            exclusao.executeUpdate();
        }
    }
}
