package br.com.modelagemdominio.rico.infraestrutura.banco;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnderecoPostgresTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "jdbc:postgresql://localhost:5432/base|jdbc:postgresql://localhost:5432/postgres|base",
    "jdbc:postgresql://host:5555/base?sslmode=require&ApplicationName=api|jdbc:postgresql://host:5555/postgres?sslmode=require&ApplicationName=api|base",
    "jdbc:postgresql://[::1]:5432/base|jdbc:postgresql://[::1]:5432/postgres|base",
    "jdbc:postgresql://host1:5432,host2:5433/base|jdbc:postgresql://host1:5432,host2:5433/postgres|base",
    "jdbc:postgresql:base|jdbc:postgresql:postgres|base",
    "jdbc:postgresql://localhost/pre%C3%A7os|jdbc:postgresql://localhost/postgres|preços",
    "jdbc:postgresql://localhost/base%2Bteste|jdbc:postgresql://localhost/postgres|base+teste"
  })
  void preservaDestinoEOpcoesDaConexao(String url, String manutencao, String nome) {
    var endereco = new UrlPostgres(url, "postgres").endereco();
    assertEquals(url, endereco.url());
    assertEquals(nome, endereco.nome());
    assertEquals(manutencao, endereco.manutencao());
  }

  @Test
  void protegeIdentificadorESuportaManutencaoConfiguravel() {
    var endereco = new UrlPostgres("jdbc:postgresql://localhost/base%22%3B--?sslmode=require", "admin banco").endereco();
    assertEquals("\"base\"\";--\"", endereco.identificadorSql());
    assertEquals("jdbc:postgresql://localhost/admin%20banco?sslmode=require", endereco.manutencao());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "jdbc:mysql://localhost/base", "jdbc:postgresql://localhost", "jdbc:postgresql://localhost/",
    "jdbc:postgresql:", "jdbc:postgresql://localhost/base%XX", "jdbc:postgresql://localhost/base%00",
    "jdbc:postgresql://localhost/base?PGDBNAME=outro"
  })
  void rejeitaUrlSemBancoValidoOuQueDesviaAManutencao(String url) {
    assertThrows(FalhaInicializacaoBanco.class, () -> new UrlPostgres(url, "postgres").endereco());
  }

  @Test
  void rejeitaNomesQueSeriamTruncadosEmBytes() {
    for (String nome : new String[]{"a".repeat(64), "á".repeat(32)}) {
      assertThrows(FalhaInicializacaoBanco.class,
        () -> new UrlPostgres("jdbc:postgresql://localhost/" + nome, "postgres").endereco());
    }
  }

  @ParameterizedTest
  @CsvSource({"'  TrUe  ', true", "'  FaLsE  ', false"})
  void interpretaOpcaoDeCriacaoDoAmbiente(String valor, boolean esperado) {
    var configuracao = new ConfiguracaoPostgres(Map.of("DB_PASSWORD", "teste", "RICO_DB_CRIAR_SE_AUSENTE", valor));

    assertEquals(esperado, configuracao.criarSeAusente());
  }

  @Test
  void construcaoNaoAbreConexoesENaoAceitaConfiguracaoAmbigua() {
    var ambiente = Map.of("RICO_DB_URL", "jdbc:postgresql://127.0.0.1:1/base", "DB_PASSWORD", "teste");
    assertDoesNotThrow(() -> new BancoPostgres(ambiente));
    assertThrows(FalhaInicializacaoBanco.class, () -> new BancoPostgres(Map.of()));
    assertThrows(FalhaInicializacaoBanco.class, () -> new BancoPostgres(Map.of(
      "DB_PASSWORD", "teste", "RICO_DB_CRIAR_SE_AUSENTE", "talvez"
    )));
  }
}
