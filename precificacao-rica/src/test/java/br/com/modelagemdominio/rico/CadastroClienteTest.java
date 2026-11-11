package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import br.com.modelagemdominio.rico.testes.SqlContado;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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
  @Tag("integracao")
  void cadastraEListaComESemEnderecoERecusaDuplicidades() throws Exception {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
    banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
    var sql = new SqlContado(banco);
    Take api = new RotasDaAplicacaoRica(sql.banco()).rotas();
    assertEquals(0, sql.conexoes());
    assertEquals(0, sql.comandos());
    String sufixo = UUID.randomUUID().toString();
    String nomeCidade = "Cidade cliente " + sufixo;
    String email = "Cliente-" + sufixo + "@example.com";
    String outroEmail = "outro-" + sufixo + "@example.com";
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
        sql.zerar();
        this.conferirListagem(api, completo, simples);
        assertEquals(1, sql.conexoes());
        assertEquals(1, sql.comandos());
      } finally {
        excluir(conexao, "DELETE FROM public.cliente WHERE LOWER(email) IN (LOWER(?), LOWER(?))", email, outroEmail);
        excluir(conexao, "DELETE FROM public.cidade WHERE nome = ?", nomeCidade);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("camposInvalidos")
  void validaAntesDeAcessarOBanco(String campo, String valor, String mensagem) throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();
    ObjectNode cliente = this.cliente(CPF, "cliente@example.com").put(campo, valor);

    assertEquals(mensagem, this.cadastrar(400, api, cliente).path("detail").asString());
  }

  private static Stream<Arguments> camposInvalidos() {
    return Stream.of(
      Arguments.of("nome", "  ", "O campo nome é obrigatório."),
      Arguments.of("nome", "x".repeat(201), "O campo nome deve conter texto válido de até 200 caracteres."),
      Arguments.of("nome", "Nome\0inválido", "O campo nome deve conter texto válido de até 200 caracteres."),
      Arguments.of("cpf", "123.456.789-00", "Informe um CPF válido."),
      Arguments.of("cpf", "11111111111", "Informe um CPF válido."),
      Arguments.of("email", "cliente@exemplo", "Informe um e-mail válido."),
      Arguments.of("telefone", "9999-0000", "Informe um telefone fixo ou celular no formato brasileiro, com DDD.")
    );
  }

  @Test
  void validaEnderecoCompletoECamposOpcionaisAntesDeAcessarOBanco() throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();
    ObjectNode cliente = this.cliente(CPF, "cliente@example.com");
    var endereco = this.endereco().put("codigoCidade", 1);
    cliente.set("endereco", endereco);

    endereco.remove("bairro");
    assertEquals("O campo bairro é obrigatório.", this.cadastrar(400, api, cliente).path("detail").asString());

    endereco.put("bairro", "Centro").put("cep", "8000-000");
    assertEquals("Informe um CEP com oito dígitos.", this.cadastrar(400, api, cliente).path("detail").asString());

    endereco.put("cep", "80000-000").put("complemento", "x".repeat(101));
    assertEquals("O campo complemento deve conter texto válido de até 100 caracteres.",
      this.cadastrar(400, api, cliente).path("detail").asString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1.5", "9223372036854775808", "null"})
  void recusaCodigoDeCidadeInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();
    ObjectNode cliente = this.cliente(CPF, "cliente@example.com");
    ObjectNode endereco = this.endereco();
    endereco.set("codigoCidade", this.json.readTree(codigo));
    cliente.set("endereco", endereco);

    assertEquals("Informe um código de cidade inteiro positivo válido.",
      this.cadastrar(400, api, cliente).path("detail").asString());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "null",
    """
      {"nome":"Ana","cpf":"12345678909","email":"ana@example.com","apelido":"extra"}
      """,
    """
      {"nome":"Ana","cpf":"12345678909","email":"ana@example.com","endereco":{
        "cep":"80000-000","logradouro":"Rua A","numero":"1","bairro":"Centro",
        "codigoCidade":1,"cidade":"extra"}}
      """
  })
  void recusaCorpoInvalidoECamposExtrasAntesDeAcessarOBanco(String corpo) throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();

    JsonNode problema = this.resposta(400, enviar(api, "/api/clientes", corpo));

    assertEquals("Formato inválido", problema.path("title").asString());
  }

  @Test
  void indisponibilidadeDoBancoNaoViraDuplicidade() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    JsonNode resposta = this.cadastrar(503, new RotasDaAplicacaoRica(banco).rotas(),
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
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    return new RsPrint(api.act(new RqFake(List.of(
      "POST " + caminho + " HTTP/1.1",
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
