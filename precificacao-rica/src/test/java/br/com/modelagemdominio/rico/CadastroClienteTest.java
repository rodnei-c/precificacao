package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadastroClienteTest {

    private static final String CPF = "12345678909";
    private static final String OUTRO_CPF = "52998224725";

    private final JsonMapper json = new JsonMapper();

    @Test
    @EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
    void cadastraEListaComESemEnderecoERecusaDuplicidades() throws Exception {
        var banco = new PGSimpleDataSource();
        banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        Take api = AplicacaoRica.rotas(banco);
        String sufixo = UUID.randomUUID().toString();
        String nomeCidade = "Cidade cliente " + sufixo;
        String email = "Cliente-" + sufixo + "@example.com";
        String outroEmail = "outro-" + sufixo + "@example.com";
        String novoEmail = "Novo-" + sufixo + "@example.com";
        try (Connection conexao = banco.getConnection()) {
            try {
                JsonNode cidade = this.resposta(201, enviar(api, "/api/cidades", """
                    {"nome":"%s","uf":"PR"}
                    """.formatted(nomeCidade)));
                long codigoCidade = cidade.path("codigo").asLong();
                JsonNode completo = this.cadastrar(201, api, clienteCompleto(email, codigoCidade));
                this.conferirRespostaCompleta(completo, email, codigoCidade);

                this.conferirDuplicidade(api, this.cliente(CPF, outroEmail));
                this.conferirDuplicidade(api, this.cliente(OUTRO_CPF, email.toLowerCase(Locale.ROOT)));

                ObjectNode semCidade = this.cliente(OUTRO_CPF, outroEmail);
                semCidade.set("endereco", this.endereco().put("codigoCidade", Long.MAX_VALUE));
                JsonNode ausente = this.cadastrar(404, api, semCidade);
                assertEquals("Cidade não encontrada.", ausente.path("detail").asString());

                JsonNode simples = this.cadastrar(201, api, this.cliente(OUTRO_CPF, outroEmail));
                assertTrue(simples.path("codigo").asLong() > 0);
                assertTrue(simples.path("telefone").isNull());
                assertTrue(simples.path("endereco").isNull());
                conferirGravacao(conexao, completo.path("codigo").asLong(), simples.path("codigo").asLong(),
                    email, outroEmail, codigoCidade);
                this.conferirListagem(api, completo, simples);
                this.conferirConsultas(api, completo, simples);
                this.conferirAlteracaoEmail(api, completo, simples, novoEmail);
                this.conferirReversaoSeRespostaNaoPuderSerMontada(conexao, api, simples, email);
            } finally {
                excluir(conexao, "DELETE FROM public.cliente WHERE LOWER(email) IN (LOWER(?), LOWER(?), LOWER(?))",
                    email, outroEmail, novoEmail);
                excluir(conexao, "DELETE FROM public.cidade WHERE nome = ?", nomeCidade);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("camposInvalidos")
    void validaAntesDeAcessarOBanco(String campo, String valor, String mensagem) throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());
        ObjectNode cliente = this.cliente(CPF, "cliente@example.com").put(campo, valor);

        assertEquals(mensagem, this.cadastrar(400, api, cliente).path("detail").asString());
    }

    private static Stream<Arguments> camposInvalidos() {
        return Stream.of(
            Arguments.of("nome", "  ", "O campo nome é obrigatório."),
            Arguments.of("cpf", "123.456.789-00", "Informe um CPF válido."),
            Arguments.of("cpf", "11111111111", "Informe um CPF válido."),
            Arguments.of("email", "cliente@exemplo", "Informe um e-mail válido."),
            Arguments.of("telefone", "9999-0000", "Informe um telefone fixo ou celular no formato brasileiro, com DDD.")
        );
    }

    @Test
    void validaEnderecoCompletoECodigoExatoAntesDeAcessarOBanco() throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());
        ObjectNode cliente = this.cliente(CPF, "cliente@example.com");
        var endereco = this.endereco().put("codigoCidade", 1);
        cliente.set("endereco", endereco);

        endereco.remove("bairro");
        assertEquals("O campo bairro é obrigatório.", this.cadastrar(400, api, cliente).path("detail").asString());

        endereco.put("bairro", "Centro").put("codigoCidade", 1.5);
        assertEquals("Informe um código de cidade inteiro positivo válido.",
            this.cadastrar(400, api, cliente).path("detail").asString());

        endereco.put("codigoCidade", 1).put("cep", "8000-000");
        assertEquals("Informe um CEP com oito dígitos.", this.cadastrar(400, api, cliente).path("detail").asString());
    }

    @Test
    void indisponibilidadeDoBancoNaoViraDuplicidade() throws Exception {
        var banco = new HikariDataSource();
        banco.close();

        JsonNode resposta = this.cadastrar(503, AplicacaoRica.rotas(banco),
            this.cliente(CPF, "cliente@example.com"));

        assertEquals("Persistência indisponível", resposta.path("title").asString());
    }

    private ObjectNode clienteCompleto(String email, long codigoCidade) {
        var cliente = this.cliente(" 123.456.789-09 ", " " + email + " ")
            .put("nome", "  Ana d'Ávila  ").put("telefone", " +55 (41) 99999-0000 ");
        cliente.set("endereco", this.endereco().put("complemento", "  ").put("codigoCidade", codigoCidade));
        return cliente;
    }

    private ObjectNode cliente(String cpf, String email) {
        return this.json.createObjectNode().put("nome", "Cliente de teste").put("cpf", cpf).put("email", email);
    }

    private ObjectNode endereco() {
        return this.json.createObjectNode().put("cep", " 80000-000 ")
            .put("logradouro", " Rua das Flores ").put("numero", "12A").put("bairro", "Centro");
    }

    private void conferirRespostaCompleta(JsonNode completo, String email, long codigoCidade) {
        long codigo = completo.path("codigo").asLong();
        assertTrue(codigo > 0);
        assertEquals(this.json.readTree("""
            {
                "codigo":%d,"nome":"Ana d'Ávila","cpf":"12345678909","email":"%s",
                "telefone":"+55 (41) 99999-0000",
                "endereco":{
                    "cep":"80000000","logradouro":"Rua das Flores","numero":"12A","bairro":"Centro",
                    "complemento":null,"codigoCidade":%d
                }
            }
            """.formatted(codigo, email, codigoCidade)), completo);
    }

    private void conferirAlteracaoEmail(Take api, JsonNode completo, JsonNode simples, String novoEmail)
        throws Exception {
        long codigo = completo.path("codigo").asLong();
        ObjectNode esperado = ((ObjectNode) completo).deepCopy().put("email", novoEmail);

        assertEquals(esperado, this.alterarEmail(200, api, codigo, "  " + novoEmail + "  "));
        assertEquals(esperado, this.consultar(200, api, codigo));

        String emailOcupado = simples.path("email").asString().toUpperCase(Locale.ROOT);
        JsonNode conflito = this.alterarEmail(400, api, codigo, emailOcupado);
        assertEquals("Já existe um cliente com o e-mail informado.", conflito.path("detail").asString());
        assertEquals(esperado, this.consultar(200, api, codigo));
        assertEquals(simples, this.consultar(200, api, simples.path("codigo").asLong()));

        JsonNode ausente = this.alterarEmail(404, api, Long.MAX_VALUE, novoEmail);
        assertEquals("Cliente não encontrado.", ausente.path("detail").asString());
    }

    private void conferirReversaoSeRespostaNaoPuderSerMontada(
        Connection conexao, Take api, JsonNode cliente, String emailLivre
    ) throws Exception {
        long codigo = cliente.path("codigo").asLong();
        try (var atualizar = conexao.prepareStatement("UPDATE public.cliente SET telefone = 'inválido' WHERE codigo = ?")) {
            atualizar.setLong(1, codigo);
            assertEquals(1, atualizar.executeUpdate());
        }

        assertEquals("Persistência indisponível", this.alterarEmail(503, api, codigo, emailLivre).path("title").asString());

        try (var consulta = conexao.prepareStatement("SELECT email FROM public.cliente WHERE codigo = ?")) {
            consulta.setLong(1, codigo);
            try (var resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals(cliente.path("email").asString(), resultado.getString("email"));
            }
        }
    }

    private JsonNode alterarEmail(int status, Take api, long codigo, String email) throws Exception {
        String corpo = this.json.writeValueAsString(this.json.createObjectNode().put("email", email));
        return this.resposta(status, enviar(api, "PATCH", "/api/clientes/" + codigo, corpo));
    }

    private void conferirConsultas(Take api, JsonNode completo, JsonNode simples) throws Exception {
        assertEquals(completo, this.consultar(200, api, completo.path("codigo").asLong()));
        assertEquals(simples, this.consultar(200, api, simples.path("codigo").asLong()));
        JsonNode ausente = this.consultar(404, api, Long.MAX_VALUE);
        assertEquals("Cliente não encontrado.", ausente.path("detail").asString());
    }

    private JsonNode consultar(int status, Take api, long codigo) throws Exception {
        var resposta = new RsPrint(api.act(new RqFake("GET", "/api/clientes/" + codigo)));
        return this.resposta(status, resposta);
    }

    private void conferirListagem(Take api, JsonNode completo, JsonNode simples) throws Exception {
        var resposta = new RsPrint(api.act(new RqFake("GET", "/api/clientes")));
        JsonNode clientes = this.resposta(200, resposta);
        assertTrue(clientes.isArray());
        long codigoCompleto = completo.path("codigo").asLong();
        long codigoSimples = simples.path("codigo").asLong();
        var cadastrados = this.json.createArrayNode();
        for (JsonNode cliente : clientes) {
            long codigo = cliente.path("codigo").asLong();
            if (codigo == codigoCompleto || codigo == codigoSimples) {
                cadastrados.add(cliente);
            }
        }
        assertEquals(this.json.createArrayNode().add(completo).add(simples), cadastrados);
    }

    private void conferirDuplicidade(Take api, ObjectNode cliente) throws Exception {
        assertEquals("Já existe um cliente com o CPF ou o e-mail informado.",
            this.cadastrar(400, api, cliente).path("detail").asString());
    }

    private static void conferirGravacao(Connection conexao, long completo, long simples, String email,
                                          String outroEmail, long codigoCidade) throws SQLException {
        try (var consulta = conexao.prepareStatement("""
            SELECT codigo, cpf, email, telefone, cep, cidade_codigo
            FROM public.cliente WHERE LOWER(email) IN (LOWER(?), LOWER(?)) ORDER BY codigo
            """)) {
            consulta.setString(1, email);
            consulta.setString(2, outroEmail);
            try (var resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                assertEquals(completo, resultado.getLong("codigo"));
                assertEquals(CPF, resultado.getString("cpf"));
                assertEquals(email, resultado.getString("email"));
                assertEquals("80000000", resultado.getString("cep"));
                assertEquals(codigoCidade, resultado.getLong("cidade_codigo"));
                assertTrue(resultado.next());
                assertEquals(simples, resultado.getLong("codigo"));
                assertEquals(outroEmail, resultado.getString("email"));
                assertNull(resultado.getString("telefone"));
                assertNull(resultado.getObject("cidade_codigo"));
                assertFalse(resultado.next());
            }
        }
    }

    private JsonNode cadastrar(int status, Take api, ObjectNode cliente) throws Exception {
        return this.resposta(status, enviar(api, "/api/clientes", this.json.writeValueAsString(cliente)));
    }

    private JsonNode resposta(int status, RsPrint resposta) throws Exception {
        assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
        return this.json.readTree(resposta.printBody());
    }

    private static RsPrint enviar(Take api, String caminho, String corpo) throws Exception {
        return enviar(api, "POST", caminho, corpo);
    }

    private static RsPrint enviar(Take api, String metodo, String caminho, String corpo) throws Exception {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        return new RsPrint(api.act(new RqFake(List.of(
            metodo + " " + caminho + " HTTP/1.1",
            "Content-Type: application/json",
            "Content-Length: " + bytes.length
        ), bytes)));
    }

    private static void excluir(Connection conexao, String sql, String... valores) throws SQLException {
        try (var exclusao = conexao.prepareStatement(sql)) {
            for (int indice = 0; indice < valores.length; indice++) {
                exclusao.setString(indice + 1, valores[indice]);
            }
            exclusao.executeUpdate();
        }
    }
}
