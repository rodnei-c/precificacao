package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.AplicacaoAnemica;
import br.com.precificacao.anemico.exceptions.InicializacaoBancoException;
import br.com.precificacao.anemico.testes.ConcorrenciaPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cria apenas bancos temporários em um servidor de testes explicitamente informado. */
@Tag("integracao")
@EnabledIfEnvironmentVariable(named = "BANCO_CRIACAO_TESTE_URL", matches = "jdbc:postgresql:.*")
class CriacaoBancoPostgresTest {

  @Test
  void criaBancoAntesDoFlywayEPreservaDadosAoReiniciar() throws Exception {
    String nome = nomeTemporario();
    assertFalse(existe(nome));
    try {
      long codigo;
      try (ConfigurableApplicationContext contexto = iniciar(nome, true)) {
        JdbcTemplate jdbc = contexto.getBean(JdbcTemplate.class);
        assertEquals("produto", jdbc.queryForObject("SELECT to_regclass('public.produto')::text", String.class));
        assertEquals("fatura", jdbc.queryForObject("SELECT to_regclass('public.fatura')::text", String.class));
        assertEquals("cliente", jdbc.queryForObject("SELECT to_regclass('public.cliente')::text", String.class));
        codigo = jdbc.queryForObject(
          "INSERT INTO categoria (nome) VALUES (?) RETURNING codigo", Long.class, "Categoria preservada"
        );
      }
      try (ConfigurableApplicationContext contexto = iniciar(nome, true)) {
        assertEquals("Categoria preservada", contexto.getBean(JdbcTemplate.class).queryForObject(
          "SELECT nome FROM categoria WHERE codigo = ?", String.class, codigo
        ));
      }
    } finally {
      removerBanco(nome);
    }
  }

  @Test
  void permiteDesativarACriacaoAutomatica() throws Exception {
    String nome = nomeTemporario();
    assertFalse(existe(nome), "O banco deve estar ausente antes de iniciar a aplicação.");
    try {
      BeanCreationException falha = assertThrows(BeanCreationException.class, () -> {
        try (ConfigurableApplicationContext contexto = iniciar(nome, false)) {
          throw new AssertionError("A aplicação não deveria iniciar com o banco ausente.");
        }
      });
      SQLException causa = assertInstanceOf(SQLException.class, falha.getMostSpecificCause(),
        "A inicialização deve falhar por um erro do PostgreSQL.");
      assertEquals("3D000", causa.getSQLState(),
        "Com a criação desativada, a causa deve ser o banco inexistente.");
      assertFalse(existe(nome), "A aplicação deve manter o banco ausente quando a criação está desativada.");
    } finally {
      removerBanco(nome);
    }
  }

  @Test
  void trataNomeComAspasComoUmUnicoIdentificador() throws Exception {
    String nome = nomeTemporario() + "\";--";
    try {
      new CriadorBancoPostgres(urlBanco(nome), usuario(), senha(), "postgres").garantirExistencia();

      assertTrue(existe(nome));
      try (Connection conexao = DriverManager.getConnection(urlBanco(nome), usuario(), senha())) {
        assertTrue(conexao.isValid(5));
      }
    } finally {
      removerBanco(nome);
    }
  }

  @Test
  void toleraCriacaoConcorrenteDoMesmoBanco() throws Exception {
    String nome = nomeTemporario();
    CompletableFuture<Void> primeira = null;
    CompletableFuture<Void> segunda = null;
    try {
      Runnable criar = () -> new CriadorBancoPostgres(urlBanco(nome), usuario(), senha(), "postgres")
        .garantirExistencia();
      try (Connection bloqueio = manutencao(); Connection observador = manutencao();
           var comando = bloqueio.createStatement()) {
        bloqueio.setAutoCommit(false);
        try {
          comando.setQueryTimeout(5);
          comando.execute("LOCK TABLE pg_catalog.pg_database IN SHARE MODE");
          int codigoSessao = ConcorrenciaPostgres.codigoDaSessao(bloqueio);
          primeira = CompletableFuture.runAsync(criar);
          segunda = CompletableFuture.runAsync(criar);

          List<String> consultas = ConcorrenciaPostgres.aguardarConsultasBloqueadas(observador, codigoSessao, 2);
          String criacaoEsperada = "CREATE DATABASE " + identificador(nome);
          assertEquals(List.of(criacaoEsperada, criacaoEsperada), consultas,
            "As duas inicializações precisam tentar criar o banco antes da liberação.");
        } finally {
          try {
            bloqueio.rollback();
          } finally {
            ConcorrenciaPostgres.aguardarConclusao(primeira, segunda);
          }
        }
      }

      CompletableFuture<Void> criacoes = CompletableFuture.allOf(primeira, segunda);
      assertDoesNotThrow(() -> criacoes.get(40, TimeUnit.SECONDS),
        "As duas inicializações devem terminar com sucesso após disputarem a criação.");

      assertTrue(existe(nome));
    } finally {
      removerBanco(nome);
    }
  }

  @Test
  void bancoExistenteNaoExigeCreatedbEErroDePermissaoEhExplicito() throws Exception {
    String nome = nomeTemporario();
    String papel = nomeTemporario();
    String ausente = nomeTemporario();
    String senhaTemporaria = UUID.randomUUID().toString();
    try (Connection admin = manutencao(); var comando = admin.createStatement()) {
      comando.executeUpdate("CREATE ROLE " + identificador(papel)
        + " LOGIN PASSWORD '" + senhaTemporaria + "' NOCREATEDB");
      try {
        comando.executeUpdate("CREATE DATABASE " + identificador(nome) + " OWNER " + identificador(papel));
        assertDoesNotThrow(() -> new CriadorBancoPostgres(
          urlBanco(nome), papel, senhaTemporaria, "manutencao_que_nao_existe"
        ).garantirExistencia());

        InicializacaoBancoException falha = assertThrows(InicializacaoBancoException.class,
          () -> new CriadorBancoPostgres(urlBanco(ausente), papel, senhaTemporaria, "postgres")
            .garantirExistencia());
        assertTrue(falha.getMessage().contains("CREATEDB"));
        assertFalse(existe(ausente));

        InicializacaoBancoException autenticacao = assertThrows(InicializacaoBancoException.class,
          () -> new CriadorBancoPostgres(urlBanco(nome), papel, "senha-incorreta", "postgres")
            .garantirExistencia());
        assertEquals("28P01", ((SQLException) autenticacao.getCause()).getSQLState());
      } finally {
        removerBanco(nome);
        comando.executeUpdate("DROP ROLE " + identificador(papel));
      }
    }
  }

  private ConfigurableApplicationContext iniciar(String nome, boolean criar) {
    SpringApplication aplicacao = new SpringApplication(AplicacaoAnemica.class);
    aplicacao.setWebApplicationType(WebApplicationType.NONE);
    aplicacao.addInitializers(contexto -> contexto.getEnvironment().getPropertySources()
      .addFirst(new MapPropertySource("banco-temporario-de-criacao", Map.of(
        "spring.datasource.url", urlBanco(nome),
        "spring.datasource.username", usuario(),
        "spring.datasource.password", senha(),
        "precificacao.banco.criar-automaticamente", criar,
        "precificacao.banco.manutencao", "postgres"
      ))));
    return aplicacao.run();
  }

  private boolean existe(String nome) throws SQLException {
    try (Connection conexao = manutencao();
         var consulta = conexao.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
      consulta.setString(1, nome);
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }

  private void removerBanco(String nome) throws SQLException {
    if (!nome.startsWith("criacao_teste_")) {
      throw new IllegalArgumentException("A limpeza só pode remover bancos temporários deste teste.");
    }
    try (Connection conexao = manutencao(); var comando = conexao.createStatement()) {
      comando.executeUpdate("DROP DATABASE IF EXISTS " + identificador(nome));
    }
  }

  private Connection manutencao() throws SQLException {
    return DriverManager.getConnection(System.getenv("BANCO_CRIACAO_TESTE_URL"), usuario(), senha());
  }

  private String urlBanco(String nome) {
    return EnderecoBancoPostgres.daUrl(System.getenv("BANCO_CRIACAO_TESTE_URL"), nome).urlManutencao();
  }

  private String nomeTemporario() {
    return "criacao_teste_" + UUID.randomUUID().toString().replace("-", "");
  }

  private String identificador(String nome) {
    return "\"" + nome.replace("\"", "\"\"") + "\"";
  }

  private String usuario() {
    return System.getenv("BANCO_TESTE_USUARIO");
  }

  private String senha() {
    return System.getenv("BANCO_TESTE_SENHA");
  }
}
