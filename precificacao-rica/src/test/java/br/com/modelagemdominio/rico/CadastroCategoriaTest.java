package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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
  @Tag("integracao")
  void cadastraCategoriaERecusaMesmoNomeComOutraCapitalizacao() throws Exception {
    PGSimpleDataSource banco = bancoDeTeste();
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
  @ValueSource(strings = {
    "{\"nome\":", "{\"nome\":\"Livros\",\"extra\":true}", "null", "[]",
    "{\"nome\":\"Livros\"} {\"nome\":\"Outro\"}"
  })
  void recusaJsonInvalidoOuComCampoDesconhecido(String corpo) throws Exception {
    RsPrint resposta = enviar(AplicacaoRica.rotas(new DataSourceSemConexao()), corpo);

    assertProblema(400, "Formato inválido", "Confira o formato do JSON e os parâmetros da URL.", resposta);
  }

  @Test
  void recusaCaractereNuloAntesDeAcessarOBanco() throws Exception {
    RsPrint resposta = enviar(AplicacaoRica.rotas(new DataSourceSemConexao()), "{\"nome\":\"Livros\\u0000outro\"}");

    assertProblema(400, "Dados inválidos", "Informe um nome de categoria válido.", resposta);
  }

  @Test
  void responde503QuandoNaoConsegueAbrirConexao() throws Exception {
    HikariDataSource bancoFechado = new HikariDataSource();
    bancoFechado.close();

    RsPrint resposta = enviar(AplicacaoRica.rotas(bancoFechado), "{\"nome\":\"Livros\"}");

    assertProblema(503, "Persistência indisponível",
      "Não foi possível concluir a operação no banco de dados.", resposta);
  }

  @Test
  @Tag("integracao")
  void falhaDeGravacaoNaoViraDuplicidadeENaoAlteraCategoriasExistentes() throws Exception {
    var banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String anterior = "Categoria preservada " + sufixo;
    String rejeitada = "Categoria rejeitada " + sufixo;
    String funcao = "falha_categoria_" + sufixo;
    String gatilho = "gatilho_categoria_" + sufixo;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        RsPrint cadastro = enviar(api, "{\"nome\":\"" + anterior + "\"}");
        assertStatus(201, cadastro);
        long codigo = this.json.readTree(cadastro.printBody()).path("codigo").asLong();
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
           IF NEW.nome = TG_ARGV[0] THEN
            RAISE EXCEPTION USING ERRCODE = '23505',
             MESSAGE = 'Falha intencional de gravacao', CONSTRAINT = 'outra_restricao';
           END IF;
           RETURN NEW;
          END;
          $teste$
          """.formatted(funcao));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER INSERT ON public.categoria"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + rejeitada + "')");

        assertProblema(503, "Persistência indisponível",
          "Não foi possível concluir a operação no banco de dados.",
          enviar(api, "{\"nome\":\"" + rejeitada + "\"}"));

        try (var consulta = conexao.prepareStatement(
          "SELECT codigo, nome FROM public.categoria WHERE nome IN (?, ?)"
        )) {
          consulta.setString(1, anterior);
          consulta.setString(2, rejeitada);
          try (var resultado = consulta.executeQuery()) {
            assertTrue(resultado.next());
            assertEquals(codigo, resultado.getLong("codigo"));
            assertEquals(anterior, resultado.getString("nome"));
            assertFalse(resultado.next());
          }
        }
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          removerCategoria(banco, rejeitada);
          removerCategoria(banco, anterior);
        }
      }
    }
  }

  private static PGSimpleDataSource bancoDeTeste() {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    return banco;
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
