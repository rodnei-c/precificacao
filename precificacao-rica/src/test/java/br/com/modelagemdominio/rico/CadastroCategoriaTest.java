package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadastroCategoriaTest {

    private final JsonMapper json = new JsonMapper();

    @Test
    @EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
    void cadastraCategoriaERecusaMesmoNomeComOutraCapitalizacao() throws Exception {
        PGSimpleDataSource banco = new PGSimpleDataSource();
        banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        Take api = AplicacaoRica.rotas(banco);
        String nomeDeTeste = "Categoria d'água " + UUID.randomUUID();
        String nome = nomeDeTeste + "x".repeat(200 - nomeDeTeste.length());

        try {
            RsPrint cadastro = enviar(api, "{\"nome\":\"  " + nome + "  \"}");

            assertStatus(201, cadastro);
            var resposta = this.json.readTree(cadastro.printBody());
            long codigo = resposta.path("codigo").asLong();
            assertTrue(codigo > 0);
            assertEquals(2, resposta.size());
            assertEquals(nome, resposta.path("nome").asString());

            RsPrint repeticao = enviar(api, "{\"nome\":\"c" + nome.substring(1) + "\"}");

            assertProblema(400, "Dados inválidos", "Já existe uma categoria com esse nome.", repeticao);
            try (var conexao = banco.getConnection();
                 var consulta = conexao.prepareStatement(
                     "SELECT codigo, nome FROM public.categoria WHERE LOWER(nome) = LOWER(?)"
                 )) {
                consulta.setString(1, nome);
                try (var resultado = consulta.executeQuery()) {
                    assertTrue(resultado.next());
                    assertEquals(codigo, resultado.getLong("codigo"));
                    assertEquals(nome, resultado.getString("nome"));
                    assertFalse(resultado.next());
                }
            }
        } finally {
            removerCategoria(banco, nome);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nome\":null}", "{\"nome\":\"   \"}"})
    void recusaNomeAusenteOuEmBrancoSemConsultarOBanco(String corpo) throws Exception {
        RsPrint resposta = enviar(AplicacaoRica.rotas(new DataSourceSemConexao()), corpo);

        assertProblema(400, "Dados inválidos", "Informe o nome da categoria.", resposta);
    }

    @Test
    void recusaNomeAcimaDoLimiteSemConsultarOBanco() throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        RsPrint resposta = enviar(api, "{\"nome\":\"" + "x".repeat(201) + "\"}");

        assertProblema(400, "Dados inválidos", "O nome da categoria deve ter no máximo 200 caracteres.", resposta);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"nome\":", "{\"nome\":\"Livros\",\"extra\":true}"})
    void recusaJsonInvalidoOuComCampoDesconhecido(String corpo) throws Exception {
        RsPrint resposta = enviar(AplicacaoRica.rotas(new DataSourceSemConexao()), corpo);

        assertProblema(400, "Formato inválido", "Confira o formato do JSON e os parâmetros da URL.", resposta);
    }

    @Test
    void responde503QuandoNaoConsegueAbrirConexao() throws Exception {
        HikariDataSource bancoFechado = new HikariDataSource();
        bancoFechado.close();

        RsPrint resposta = enviar(AplicacaoRica.rotas(bancoFechado), "{\"nome\":\"Livros\"}");

        assertProblema(503, "Persistência indisponível",
            "Não foi possível concluir a operação no banco de dados.", resposta);
    }

    private static RsPrint enviar(Take api, String corpo) throws Exception {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        return new RsPrint(api.act(new RqFake(List.of(
            "POST /api/categorias HTTP/1.1",
            "Content-Type: application/json",
            "Content-Length: " + bytes.length
        ), bytes)));
    }

    private static void assertStatus(int status, RsPrint resposta) throws Exception {
        String primeiraLinha = resposta.head().iterator().next();
        assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], resposta::toString);
    }

    private void assertProblema(int status, String titulo, String detalhe, RsPrint resposta) throws Exception {
        assertStatus(status, resposta);
        assertTrue(resposta.printHead().lines().anyMatch(
            "Content-Type: application/problem+json"::equalsIgnoreCase
        ));
        var problema = this.json.readTree(resposta.printBody());
        assertEquals(status, problema.path("status").asInt());
        assertEquals(titulo, problema.path("title").asString());
        assertEquals(detalhe, problema.path("detail").asString());
        assertEquals("/api/categorias", problema.path("instance").asString());
    }

    private static void removerCategoria(PGSimpleDataSource banco, String nome) throws SQLException {
        try (var conexao = banco.getConnection();
             var exclusao = conexao.prepareStatement("DELETE FROM public.categoria WHERE LOWER(nome) = LOWER(?)")) {
            exclusao.setString(1, nome);
            exclusao.executeUpdate();
        }
    }
}
