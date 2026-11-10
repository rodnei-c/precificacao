package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CadastroCidadeTest {

  private final JsonMapper json = new JsonMapper();

  @Test
  @Tag("integracao")
  void normalizaUfEPermiteOMesmoNomeSomenteEmUfDistinta() throws Exception {
    var banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    String inicio = "São João d'Aliança " + UUID.randomUUID();
    String nome = inicio + "x".repeat(200 - inicio.length());

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
        """, "Informe uma sigla de UF brasileira válida."),
      Arguments.of("{\"nome\":\"Cidade\\u0000inválida\",\"uf\":\"PR\"}", "Informe um nome de cidade válido.")
    );
  }

  @Test
  void recusaNomeAcimaDoLimiteAntesDeAbrirConexao() throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    var problema = this.resposta(400, enviar(api, this.corpo("x".repeat(201), "PR")));

    assertEquals("O nome da cidade deve ter no máximo 200 caracteres.", problema.path("detail").asString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"nome\":\"Curitiba\",\"uf\":\"PR\",\"codigo\":1}", "null"})
  void recusaCorpoInvalidoAntesDeAbrirConexao(String corpo) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    var problema = this.resposta(400, enviar(api, corpo));

    assertEquals("Formato inválido", problema.path("title").asString());
  }

  @Test
  void informaQuandoNaoConsegueAbrirConexao() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    var problema = this.resposta(503, enviar(AplicacaoRica.rotas(banco), this.corpo("Curitiba", "PR")));

    assertEquals("Persistência indisponível", problema.path("title").asString());
  }

  @Test
  @Tag("integracao")
  void falhaDeOutraRestricaoNaoViraDuplicidadeENaoAlteraCidadesExistentes() throws Exception {
    var banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    var cidades = new PgCidades(banco);
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String nomePreservado = "Cidade preservada " + sufixo;
    String nomeEmFalha = "Cidade rejeitada " + sufixo;
    String funcao = "falha_cidade_" + sufixo;
    String gatilho = "gatilho_cidade_" + sufixo;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        this.cidadeCadastrada(api, nomePreservado, "SC", nomePreservado, "SC");
        var antes = cidades.listar();
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF NEW.nome = TG_ARGV[0] THEN
              RAISE EXCEPTION USING ERRCODE = '23505',
                MESSAGE = 'Falha intencional no cadastro', CONSTRAINT = 'outra_restricao';
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER INSERT ON public.cidade"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + nomeEmFalha + "')");

        var problema = this.resposta(503, enviar(api, this.corpo(nomeEmFalha, "PR")));
        assertEquals(this.json.readTree("""
          {"title":"Persistência indisponível","status":503,
           "detail":"Não foi possível concluir a operação no banco de dados.","instance":"/api/cidades"}
          """), problema);
        assertEquals(antes, cidades.listar());

        comando.execute("DROP FUNCTION public." + funcao + "() CASCADE");
        this.cidadeCadastrada(api, nomeEmFalha, "PR", nomeEmFalha, "PR");
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          removerCidades(banco, nomeEmFalha);
          removerCidades(banco, nomePreservado);
        }
      }
    }
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

  private static PGSimpleDataSource bancoDeTeste() {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    return banco;
  }
}
