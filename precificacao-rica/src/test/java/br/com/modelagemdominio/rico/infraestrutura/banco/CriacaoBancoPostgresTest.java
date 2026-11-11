package br.com.modelagemdominio.rico.infraestrutura.banco;

import br.com.modelagemdominio.rico.AplicacaoRica;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Usa apenas bancos e papéis temporários em um servidor exclusivo informado pelo teste. */
@Tag("integracao")
class CriacaoBancoPostgresTest {

  @TempDir
  Path temporario;

  @Test
  void iniciaApiComBancoAusenteEPreservaDadosAoReiniciar() throws Exception {
    String nome = nomeTemporario();
    try {
      assertFalse(existe(nome));
      String localizacao;
      try (var api = this.iniciar(nome)) {
        URI endereco = api.endereco();
        var cadastro = HttpClient.newHttpClient().send(HttpRequest.newBuilder(endereco.resolve("/api/produtos"))
          .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("""
            {"nome":"Produto preservado","custo":100,"percentual":20,"modalidadePrecificacao":"ACRESCIMO_SOBRE_CUSTO"}
            """)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, cadastro.statusCode(), cadastro::body);
        localizacao = cadastro.headers().firstValue("Location").orElseThrow();
      }
      try (var api = this.iniciar(nome)) {
        var consulta = HttpClient.newHttpClient().send(HttpRequest.newBuilder(api.endereco().resolve(localizacao))
          .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, consulta.statusCode(), consulta::body);
        assertTrue(consulta.body().contains("Produto preservado"));
      }
      conferirMigracoes(nome);
    } finally {
      remover(nome);
    }
  }

  @Test
  void criacaoDesativadaMantemBancoAusenteEConservaACausa() throws Exception {
    String nome = nomeTemporario();
    try {
      assertFalse(existe(nome));
      var falha = assertThrows(FalhaInicializacaoBanco.class,
        () -> new BancoPostgres(url(nome), usuario(), senha(), false, manutencao()).preparar());
      assertEquals("3D000", assertInstanceOf(SQLException.class, falha.getCause()).getSQLState());
      assertEquals("O banco não existe e a criação automática está desativada em RICO_DB_CRIAR_SE_AUSENTE.",
        falha.getMessage());
      assertFalse(existe(nome));
    } finally {
      remover(nome);
    }
  }

  @Test
  void bancoExistenteNaoExigeCreatedbENovoBancoInformaPermissao() throws Exception {
    String nome = nomeTemporario();
    String ausente = nomeTemporario();
    String papel = nomeTemporario();
    String senhaPapel = UUID.randomUUID().toString();
    try {
      try (var admin = admin(); var comando = admin.createStatement()) {
        comando.executeUpdate("CREATE ROLE " + identificador(papel) + " LOGIN NOCREATEDB PASSWORD '" + senhaPapel + "'");
        comando.executeUpdate("CREATE DATABASE " + identificador(nome) + " OWNER " + identificador(papel));
      }
      try (var banco = new BancoPostgres(url(nome), papel, senhaPapel, true, "manutencao_inexistente").preparar()) {
        try (var conexao = banco.getConnection(); var consulta = conexao.createStatement();
             var resultado = consulta.executeQuery("SELECT count(*) FROM public.flyway_schema_history WHERE success")) {
          assertTrue(resultado.next());
          assertEquals(10, resultado.getInt(1));
        }
      }
      var falha = assertThrows(FalhaInicializacaoBanco.class,
        () -> new BancoPostgres(url(ausente), papel, senhaPapel, true, manutencao()).preparar());
      assertEquals("42501", assertInstanceOf(SQLException.class, falha.getCause()).getSQLState());
      assertTrue(falha.getMessage().contains("CREATEDB"));
      assertFalse(existe(ausente));
    } finally {
      remover(nome);
      remover(ausente);
      try (var admin = admin(); var comando = admin.createStatement()) {
        comando.executeUpdate("DROP ROLE IF EXISTS " + identificador(papel));
      }
    }
  }

  @Test
  void senhaIncorretaNaoCriaBanco() throws Exception {
    String nome = nomeTemporario();
    try {
      var falha = assertThrows(FalhaInicializacaoBanco.class,
        () -> new BancoPostgres(url(nome), usuario(), UUID.randomUUID().toString(), true, manutencao()).preparar());
      assertEquals("28P01", assertInstanceOf(SQLException.class, falha.getCause()).getSQLState());
      assertEquals("Não foi possível autenticar no PostgreSQL. Confira o usuário e DB_PASSWORD.", falha.getMessage());
      assertFalse(existe(nome));
    } finally {
      remover(nome);
    }
  }

  @Test
  void bancoQueRecusaConexoesConservaFalhaOriginal() throws Exception {
    String nome = nomeTemporario();
    try {
      try (var admin = admin(); var comando = admin.createStatement()) {
        comando.executeUpdate("CREATE DATABASE " + identificador(nome) + " WITH ALLOW_CONNECTIONS false");
      }

      var falha = assertThrows(FalhaInicializacaoBanco.class,
        () -> new BancoPostgres(url(nome), usuario(), senha(), true, "manutencao_inexistente").preparar());

      assertEquals("55000", assertInstanceOf(SQLException.class, falha.getCause()).getSQLState());
      assertEquals("Não foi possível acessar o PostgreSQL. Confira a conexão e o banco de manutenção.",
        falha.getMessage());
    } finally {
      remover(nome);
    }
  }

  @Test
  void criaNomeComAspasComoUmUnicoBanco() throws Exception {
    String nome = nomeTemporario() + "\";--";
    try {
      try (var banco = new BancoPostgres(url(nome), usuario(), senha(), true, manutencao()).preparar();
           var conexao = banco.getConnection(); var comando = conexao.createStatement();
           var resultado = comando.executeQuery("SELECT current_database()")) {
        assertTrue(resultado.next());
        assertEquals(nome, resultado.getString(1));
      }
      conferirMigracoes(nome);
    } finally {
      remover(nome);
    }
  }

  @Test
  void duasInicializacoesConcorremNaCriacaoETerminamMigradas() throws Exception {
    String nome = nomeTemporario();
    var operacoes = new ArrayList<CompletableFuture<Void>>();
    try {
      try (var bloqueio = admin(); var observador = admin(); var comando = bloqueio.createStatement()) {
        bloqueio.setAutoCommit(false);
        try {
          comando.setQueryTimeout(5);
          comando.execute("LOCK TABLE pg_catalog.pg_database IN SHARE MODE");
          int sessao;
          try (var resultado = comando.executeQuery("SELECT pg_backend_pid()")) {
            assertTrue(resultado.next());
            sessao = resultado.getInt(1);
          }
          for (int indice = 0; indice < 2; indice++) {
            operacoes.add(CompletableFuture.runAsync(() -> {
              try (var banco = new BancoPostgres(url(nome), usuario(), senha(), true, manutencao()).preparar()) {
                assertFalse(banco.isClosed());
              }
            }));
          }
          assertEquals(List.of("CREATE DATABASE " + identificador(nome), "CREATE DATABASE " + identificador(nome)),
            aguardarCriacoes(observador, sessao));
        } finally {
          bloqueio.rollback();
        }
      }
      CompletableFuture.allOf(operacoes.toArray(CompletableFuture[]::new)).get(40, TimeUnit.SECONDS);
      conferirMigracoes(nome);
    } finally {
      try {
        CompletableFuture.allOf(operacoes.toArray(CompletableFuture[]::new))
          .handle((resultado, falha) -> null).get(40, TimeUnit.SECONDS);
      } finally {
        remover(nome);
      }
    }
  }

  @Test
  void falhaDeMigracaoImpedePublicacaoDaApi() throws Exception {
    String nome = nomeTemporario();
    try {
      try (var admin = admin(); var comando = admin.createStatement()) {
        comando.executeUpdate("CREATE DATABASE " + identificador(nome));
      }
      try (var conexao = DriverManager.getConnection(url(nome), usuario(), senha());
           var comando = conexao.createStatement()) {
        comando.executeUpdate("CREATE TABLE estrutura_incompativel (codigo INTEGER)");
      }
      try (var api = this.iniciar(nome)) {
        assertTrue(api.processo().waitFor(15, TimeUnit.SECONDS), "A API deveria encerrar após a falha de migração.");
        assertTrue(api.processo().exitValue() != 0);
        String log = Files.readString(api.log());
        assertTrue(log.contains("Não foi possível aplicar as migrações"), log);
        assertFalse(log.contains("API do modelo rico em"), log);
      }
    } finally {
      remover(nome);
    }
  }

  private ApiTemporaria iniciar(String nome) throws Exception {
    Path log = this.temporario.resolve(UUID.randomUUID() + ".log");
    var processo = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
      "-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8",
      "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
      AplicacaoRica.class.getName());
    var ambiente = processo.environment();
    ambiente.put("RICO_DB_URL", url(nome));
    ambiente.put("RICO_DB_USER", usuario());
    ambiente.put("DB_PASSWORD", senha());
    ambiente.put("RICO_DB_MANUTENCAO", manutencao());
    ambiente.put("RICO_DB_CRIAR_SE_AUSENTE", "true");
    ambiente.put("RICO_SERVER_PORT", "0");
    return new ApiTemporaria(processo.redirectErrorStream(true).redirectOutput(log.toFile()).start(), log);
  }

  private record ApiTemporaria(Process processo, Path log) implements AutoCloseable {

    URI endereco() throws Exception {
      var formato = Pattern.compile("http://127\\.0\\.0\\.1:\\d+");
      long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
      while (this.processo.isAlive() && System.nanoTime() < limite) {
        var endereco = formato.matcher(Files.readString(this.log));
        if (endereco.find()) {
          return URI.create(endereco.group());
        }
        Thread.sleep(25);
      }
      return fail("A API não ficou pronta. " + Files.readString(this.log));
    }

    @Override
    public void close() throws Exception {
      this.processo.destroyForcibly();
      assertTrue(this.processo.waitFor(5, TimeUnit.SECONDS), "O processo da API não foi encerrado.");
    }
  }

  private static List<String> aguardarCriacoes(Connection observador, int sessao) throws Exception {
    long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    try (var consulta = observador.prepareStatement("""
      WITH RECURSIVE bloqueados(pid) AS (
        SELECT pid FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid))
        UNION
        SELECT atividade.pid FROM pg_stat_activity atividade
        JOIN bloqueados ON bloqueados.pid = ANY(pg_blocking_pids(atividade.pid))
      )
      SELECT atividade.query FROM pg_stat_activity atividade JOIN bloqueados USING (pid)
      WHERE atividade.datname = current_database() AND atividade.backend_type = 'client backend'
      ORDER BY atividade.pid
      """)) {
      consulta.setInt(1, sessao);
      consulta.setQueryTimeout(2);
      while (System.nanoTime() < limite) {
        var consultas = new ArrayList<String>();
        try (var resultado = consulta.executeQuery()) {
          while (resultado.next()) {
            consultas.add(resultado.getString(1));
          }
        }
        if (consultas.size() == 2) {
          return consultas;
        }
        Thread.sleep(25);
      }
    }
    return fail("As duas inicializações deveriam estar bloqueadas na criação do mesmo banco.");
  }

  private static void conferirMigracoes(String nome) throws Exception {
    try (var conexao = DriverManager.getConnection(url(nome), usuario(), senha());
         var consulta = conexao.createStatement();
         var resultado = consulta.executeQuery("SELECT count(*) FROM public.flyway_schema_history WHERE success")) {
      assertTrue(resultado.next());
      assertEquals(10, resultado.getInt(1));
    }
  }

  private static boolean existe(String nome) throws SQLException {
    try (var admin = admin(); var consulta = admin.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
      consulta.setQueryTimeout(5);
      consulta.setString(1, nome);
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }

  private static void remover(String nome) throws SQLException {
    if (!nome.startsWith("rico_teste_")) {
      throw new IllegalArgumentException("A limpeza aceita somente bancos temporários deste teste.");
    }
    try (var admin = admin(); var comando = admin.createStatement()) {
      comando.setQueryTimeout(5);
      comando.executeUpdate("DROP DATABASE IF EXISTS " + identificador(nome) + " WITH (FORCE)");
    }
  }

  private static String identificador(String valor) {
    return "\"" + valor.replace("\"", "\"\"") + "\"";
  }

  private static String nomeTemporario() {
    return "rico_teste_" + UUID.randomUUID().toString().replace("-", "");
  }

  private static String usuario() {
    return System.getenv("RICO_BANCO_TESTE_USUARIO");
  }

  private static String senha() {
    return System.getenv("RICO_BANCO_TESTE_SENHA");
  }

  private static String manutencao() {
    return org.postgresql.PGProperty.PG_DBNAME.getOrDefault(org.postgresql.Driver.parseURL(
      System.getenv("RICO_BANCO_CRIACAO_TESTE_URL"), new java.util.Properties()
    ));
  }

  private static Connection admin() throws SQLException {
    return DriverManager.getConnection(System.getenv("RICO_BANCO_CRIACAO_TESTE_URL"), usuario(), senha());
  }

  private static String url(String nome) {
    String manutencao = System.getenv("RICO_BANCO_CRIACAO_TESTE_URL");
    int inicioParametros = manutencao.indexOf('?');
    String endereco = inicioParametros < 0 ? manutencao : manutencao.substring(0, inicioParametros);
    String opcoes = inicioParametros < 0 ? "" : manutencao.substring(inicioParametros);
    int inicio = endereco.startsWith("jdbc:postgresql://")
      ? endereco.indexOf('/', "jdbc:postgresql://".length()) + 1 : "jdbc:postgresql:".length();
    return endereco.substring(0, inicio) + URLEncoder.encode(nome, StandardCharsets.UTF_8).replace("+", "%20") + opcoes;
  }
}
