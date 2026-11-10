package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.exceptions.InicializacaoBancoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnderecoBancoPostgresTest {

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "jdbc:postgresql://localhost:5432/precificacao|jdbc:postgresql://localhost:5432/postgres|precificacao",
    "jdbc:postgresql://servidor:5555/base?sslmode=require&ApplicationName=api|jdbc:postgresql://servidor:5555/postgres?sslmode=require&ApplicationName=api|base",
    "jdbc:postgresql://[::1]:5432/base|jdbc:postgresql://[::1]:5432/postgres|base",
    "jdbc:postgresql://servidor1:5432,servidor2:5433/base|jdbc:postgresql://servidor1:5432,servidor2:5433/postgres|base",
    "jdbc:postgresql:base|jdbc:postgresql:postgres|base",
    "jdbc:postgresql://localhost/pre%C3%A7os|jdbc:postgresql://localhost/postgres|preços",
    "jdbc:postgresql://localhost/base+teste|jdbc:postgresql://localhost/postgres|base+teste"
  })
  void preservaServidorEParametrosAoTrocarSomenteOBanco(String url, String manutencao, String nome) {
    EnderecoBancoPostgres endereco = EnderecoBancoPostgres.daUrl(url, "postgres");

    assertEquals(url, endereco.url());
    assertEquals(manutencao, endereco.urlManutencao());
    assertEquals(nome, endereco.nome());
  }

  @Test
  void escapaAspasNoIdentificadorSql() {
    EnderecoBancoPostgres endereco = EnderecoBancoPostgres.daUrl(
      "jdbc:postgresql://localhost/base%22%3BSELECT%201%3B--", "postgres"
    );

    assertEquals("\"base\"\";SELECT 1;--\"", endereco.identificadorSql());
  }

  @Test
  void permiteBancoDeManutencaoConfiguravel() {
    EnderecoBancoPostgres endereco = EnderecoBancoPostgres.daUrl(
      "jdbc:postgresql://localhost/base?sslmode=require", "admin banco"
    );

    assertEquals("jdbc:postgresql://localhost/admin%20banco?sslmode=require", endereco.urlManutencao());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "jdbc:mysql://localhost/base", "jdbc:postgresql://localhost", "jdbc:postgresql://localhost/",
    "jdbc:postgresql:", "jdbc:postgresql://localhost/base%XX", "jdbc:postgresql://localhost/base%00"
  })
  void rejeitaEnderecoInvalidoOuBancoAusente(String url) {
    assertThrows(InicializacaoBancoException.class, () -> EnderecoBancoPostgres.daUrl(url, "postgres"));
  }

  @Test
  void rejeitaNomeQueSeriaTruncadoPeloPostgres() {
    assertThrows(InicializacaoBancoException.class,
      () -> EnderecoBancoPostgres.daUrl("jdbc:postgresql://localhost/" + "a".repeat(64), "postgres"));
    assertThrows(InicializacaoBancoException.class,
      () -> EnderecoBancoPostgres.daUrl("jdbc:postgresql://localhost/" + "á".repeat(32), "postgres"));
  }
}
