package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integracao")
class AlteracaoTelefonePostgresTest {

  private final JsonMapper json = new JsonMapper();

  @Test
  void alteraRemoveEOmiteTelefonePreservandoDemaisDados() throws Exception {
    try (var cliente = this.clienteDeTeste()) {
      var esperado = ((ObjectNode) cliente.original()).deepCopy().put("telefone", "(41) 3333-4444");
      this.conferirAlteracao(cliente, "{\"telefone\":\"  (41) 3333-4444  \"}", esperado);

      String email = "intermediario-" + UUID.randomUUID() + "@example.com";
      esperado.put("email", email);
      this.conferirAlteracao(cliente, "{\"email\":\"" + email + "\"}", esperado);

      esperado.putNull("telefone");
      this.conferirAlteracao(cliente, "{\"telefone\":null}", esperado);

      String outroEmail = "ambos-" + UUID.randomUUID() + "@example.com";
      esperado.put("email", outroEmail).put("telefone", "(41) 98888-7777");
      this.conferirAlteracao(cliente,
        "{\"email\":\"" + outroEmail + "\",\"telefone\":\"(41) 98888-7777\"}", esperado);
    }
  }

  @Test
  void emailDuplicadoImpedeTambemAAlteracaoDoTelefone() throws Exception {
    try (var cliente = this.clienteDeTeste()) {
      String ocupado = "ocupado-" + UUID.randomUUID() + "@example.com";
      try {
        JsonNode outro = this.corpo(201, enviar(cliente.api(), "POST", "/api/clientes", """
          {"nome":"Cliente ocupado","cpf":"52998224725","email":"%s"}
          """.formatted(ocupado)));

        JsonNode problema = this.alterar(cliente, """
          {"email":"%s","telefone":"(41) 98888-7777"}
          """.formatted(ocupado), 400);

        assertEquals("Já existe um cliente com o e-mail informado.", problema.path("detail").asString());
        assertEquals(cliente.original(), this.consultar(cliente));
        assertEquals(outro, this.corpo(200, new RsPrint(cliente.api().act(new RqFake(
          "GET", "/api/clientes/" + outro.path("codigo").asLong()
        )))));
      } finally {
        try (var conexao = cliente.banco().getConnection();
          var exclusao = conexao.prepareStatement("DELETE FROM public.cliente WHERE email = ?")) {
          exclusao.setString(1, ocupado);
          exclusao.executeUpdate();
        }
      }
    }
  }

  @Test
  void falhaDepoisDaEscritaDesfazEmailETelefoneJuntos() throws Exception {
    try (var cliente = this.clienteDeTeste();
      var conexao = cliente.banco().getConnection(); var comando = conexao.createStatement()) {
      String sufixo = UUID.randomUUID().toString().replace("-", "");
      String funcao = "falha_contatos_" + sufixo;
      String antes = "gatilho_contatos_antes_" + sufixo;
      String depois = "gatilho_contatos_depois_" + sufixo;
      String sequencia = "marcador_contatos_" + sufixo;
      String email = "composto-" + sufixo + "@example.com";
      String telefone = "(41) 98888-7777";
      String entrada = this.json.writeValueAsString(this.json.createObjectNode()
        .put("email", email).put("telefone", telefone));
      try {
        comando.execute("CREATE SEQUENCE public." + sequencia);
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF OLD.codigo = TG_ARGV[0]::BIGINT AND NEW.email = TG_ARGV[1] AND NEW.telefone = TG_ARGV[2] THEN
              IF TG_WHEN = 'BEFORE' THEN
                NEW.cpf := '11111111111';
              ELSE
                PERFORM nextval('public.%s');
              END IF;
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao, sequencia));
        String acao = " ON public.cliente FOR EACH ROW EXECUTE FUNCTION public." + funcao
          + "('" + cliente.codigo() + "', '" + email + "', '" + telefone + "')";
        comando.execute("CREATE TRIGGER " + antes + " BEFORE UPDATE OF email, telefone" + acao);
        comando.execute("CREATE TRIGGER " + depois + " AFTER UPDATE" + acao);

        JsonNode problema = this.alterar(cliente, entrada, 503);
        assertEquals(this.json.readTree("""
          {"title":"Persistência indisponível","status":503,
           "detail":"Não foi possível concluir a operação no banco de dados.","instance":"/api/clientes/%d"}
          """.formatted(cliente.codigo())), problema);
        conferirMarcador(conexao, sequencia, 1);
        assertEquals(cliente.original(), this.consultar(cliente));

        comando.execute("DROP TRIGGER " + antes + " ON public.cliente");
        ObjectNode esperado = ((ObjectNode) cliente.original()).deepCopy().put("email", email).put("telefone", telefone);
        this.conferirAlteracao(cliente, entrada, esperado);
        conferirMarcador(conexao, sequencia, 2);
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          comando.execute("DROP SEQUENCE IF EXISTS public." + sequencia);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"+55 (41) 96666-5555\"", "null"})
  void alteracoesConcorrentesDeEmailETelefoneNaoSeSobrescrevem(String telefoneJson) throws Exception {
    try (var cliente = this.clienteDeTeste()) {
      String email = "depois-" + UUID.randomUUID() + "@example.com";
      var executor = Executors.newFixedThreadPool(2);
      try {
        try (var bloqueio = cliente.banco().getConnection(); var monitor = cliente.banco().getConnection()) {
          bloqueio.setAutoCommit(false);
          try {
            int sessao = bloquearCliente(bloqueio, cliente.codigo());
            var alteracaoEmail = executor.submit(() -> enviar(cliente.api(), "PATCH", "/api/clientes/" + cliente.codigo(),
              "{\"email\":\"" + email + "\"}"));
            var alteracaoTelefone = executor.submit(() -> enviar(cliente.api(), "PATCH", "/api/clientes/" + cliente.codigo(),
              "{\"telefone\":" + telefoneJson + "}"));

            aguardarBloqueios(monitor, sessao, cliente.banco().getApplicationName());
            bloqueio.commit();
            assertEquals(email, this.corpo(200, alteracaoEmail.get(15, TimeUnit.SECONDS)).path("email").asString());
            assertEquals(this.json.readTree(telefoneJson),
              this.corpo(200, alteracaoTelefone.get(15, TimeUnit.SECONDS)).path("telefone"));
          } finally {
            bloqueio.rollback();
          }
        }
      } finally {
        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
          executor.shutdownNow();
          assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "As operações de teste devem encerrar.");
        }
      }
      ObjectNode esperado = ((ObjectNode) cliente.original()).deepCopy().put("email", email);
      esperado.set("telefone", this.json.readTree(telefoneJson));
      conferirContatosGravados(cliente, esperado);
      assertEquals(esperado, this.consultar(cliente));
    }
  }

  private static int bloquearCliente(Connection conexao, long codigo) throws SQLException {
    try (var consulta = conexao.prepareStatement("SELECT codigo FROM public.cliente WHERE codigo = ? FOR UPDATE")) {
      consulta.setLong(1, codigo);
      consulta.setQueryTimeout(2);
      try (var resultado = consulta.executeQuery()) {
        assertTrue(resultado.next());
      }
    }
    try (var consulta = conexao.createStatement(); var resultado = consulta.executeQuery("SELECT pg_backend_pid()")) {
      assertTrue(resultado.next());
      return resultado.getInt(1);
    }
  }

  private static void aguardarBloqueios(Connection monitor, int sessao, String aplicacao) throws Exception {
    long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    try (var consulta = monitor.prepareStatement("""
      WITH RECURSIVE bloqueados(pid) AS (
        SELECT pid FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid))
        UNION
        SELECT atividade.pid FROM pg_stat_activity atividade
        JOIN bloqueados ON bloqueados.pid = ANY(pg_blocking_pids(atividade.pid))
      )
      SELECT count(*) FROM bloqueados JOIN pg_stat_activity atividade USING (pid)
      WHERE atividade.datname = current_database() AND atividade.backend_type = 'client backend'
        AND atividade.application_name = ?
      """)) {
      consulta.setInt(1, sessao);
      consulta.setString(2, aplicacao);
      consulta.setQueryTimeout(2);
      while (System.nanoTime() < limite) {
        try (var resultado = consulta.executeQuery()) {
          if (resultado.next() && resultado.getInt(1) == 2) {
            return;
          }
        }
        Thread.sleep(25);
      }
    }
    fail("As duas requisições devem aguardar a linha bloqueada pela sessão de teste.");
  }

  private ClienteDeTeste clienteDeTeste() throws Exception {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    String sufixo = UUID.randomUUID().toString();
    banco.setApplicationName("teste-contatos-" + sufixo);
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String nomeCidade = "Cidade dos contatos " + sufixo;
    String email = "antes-" + sufixo + "@example.com";
    try {
      var cidade = new PgCidades(banco).cadastrar(new NomeCidade(nomeCidade), UnidadeFederativa.PR);
      ObjectNode pedido = this.json.createObjectNode().put("nome", "Cliente d'Ávila")
        .put("cpf", "11144477735").put("email", email).put("telefone", "+55 (41) 99999-0000");
      pedido.set("endereco", this.json.createObjectNode().put("cep", "80000-000")
        .put("logradouro", "Rua das Flores").put("numero", "12A").put("bairro", "Centro")
        .put("complemento", "Bloco A").put("codigoCidade", cidade.codigo()));
      JsonNode original = this.corpo(201, enviar(api, "POST", "/api/clientes", this.json.writeValueAsString(pedido)));
      assertTrue(original.path("codigo").asLong() > 0);
      return new ClienteDeTeste(banco, api, original, sql);
    } catch (Exception | AssertionError falha) {
      try (var conexao = banco.getConnection();
        var cliente = conexao.prepareStatement("DELETE FROM public.cliente WHERE email = ?");
        var cidade = conexao.prepareStatement("DELETE FROM public.cidade WHERE nome = ?")) {
        cliente.setString(1, email);
        cliente.executeUpdate();
        cidade.setString(1, nomeCidade);
        cidade.executeUpdate();
      } catch (SQLException falhaAoLimpar) {
        falha.addSuppressed(falhaAoLimpar);
      }
      throw falha;
    }
  }

  private record ClienteDeTeste(PGSimpleDataSource banco, Take api, JsonNode original, SqlContado sql)
    implements AutoCloseable {

    long codigo() {
      return this.original.path("codigo").asLong();
    }

    @Override
    public void close() throws SQLException {
      try (var conexao = this.banco.getConnection();
        var cliente = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ?");
        var cidade = conexao.prepareStatement("DELETE FROM public.cidade WHERE codigo = ?")) {
        cliente.setLong(1, this.codigo());
        cliente.executeUpdate();
        cidade.setLong(1, this.original.path("endereco").path("codigoCidade").asLong());
        cidade.executeUpdate();
      }
    }
  }

  private void conferirAlteracao(ClienteDeTeste cliente, String entrada, JsonNode esperado) throws Exception {
    assertEquals(esperado, this.alterar(cliente, entrada, 200));
    assertEquals(esperado, this.consultar(cliente));
  }

  private JsonNode alterar(ClienteDeTeste cliente, String entrada, int status) throws Exception {
    cliente.sql().zerar();
    JsonNode resposta = this.corpo(status, enviar(cliente.api(), "PATCH", "/api/clientes/" + cliente.codigo(), entrada));
    assertEquals(1, cliente.sql().conexoes());
    return resposta;
  }

  private JsonNode consultar(ClienteDeTeste cliente) throws Exception {
    return this.corpo(200, new RsPrint(cliente.api().act(new RqFake("GET", "/api/clientes/" + cliente.codigo()))));
  }

  private JsonNode corpo(int status, RsPrint resposta) throws Exception {
    String primeiraLinha = resposta.head().iterator().next();
    assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], primeiraLinha);
    return this.json.readTree(resposta.printBody());
  }

  private static RsPrint enviar(Take api, String metodo, String caminho, String corpo) throws Exception {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    return new RsPrint(api.act(new RqFake(List.of(
      metodo + " " + caminho + " HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + bytes.length
    ), bytes)));
  }

  private static void conferirMarcador(Connection conexao, String sequencia, long esperado) throws SQLException {
    try (var consulta = conexao.createStatement();
      var resultado = consulta.executeQuery("SELECT last_value, is_called FROM public." + sequencia)) {
      assertTrue(resultado.next());
      assertTrue(resultado.getBoolean("is_called"));
      assertEquals(esperado, resultado.getLong("last_value"));
    }
  }

  private static void conferirContatosGravados(ClienteDeTeste cliente, JsonNode esperado) throws SQLException {
    try (var conexao = cliente.banco().getConnection();
      var consulta = conexao.prepareStatement("SELECT email, telefone FROM public.cliente WHERE codigo = ?")) {
      consulta.setLong(1, cliente.codigo());
      try (var resultado = consulta.executeQuery()) {
        assertTrue(resultado.next());
        assertEquals(esperado.path("email").asString(), resultado.getString("email"));
        String telefone = esperado.path("telefone").isNull() ? null : esperado.path("telefone").asString();
        assertEquals(telefone, resultado.getString("telefone"));
      }
    }
  }
}
