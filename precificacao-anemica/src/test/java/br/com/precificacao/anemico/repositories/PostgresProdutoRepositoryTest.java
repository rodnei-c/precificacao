package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.models.Produto;
import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica a migração e a persistência em um PostgreSQL de teste informado explicitamente.
 */
@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
class PostgresProdutoRepositoryTest {

  @Test
  void executaMigracaoEPersisteProdutoEmPostgres() {
    DriverManagerDataSource conexao = new DriverManagerDataSource(
      System.getenv("BANCO_TESTE_URL"), System.getenv("BANCO_TESTE_USUARIO"),
      System.getenv("BANCO_TESTE_SENHA")
    );
    Flyway.configure().dataSource(conexao).load().migrate();
    JdbcProdutoRepository repositorio = new JdbcProdutoRepository(new JdbcTemplate(conexao));
    Produto cadastrado = repositorio.salvar(new Produto(
      null, "Produto de integração", new BigDecimal("100.00"), new BigDecimal("125.00"),
      new BigDecimal("20.0000"), ModalidadePrecificacao.MARGEM_SOBRE_VENDA
    ));

    assertTrue(cadastrado.getCodigo() > 0);
    Produto consultado = repositorio.buscarPorCodigo(cadastrado.getCodigo()).orElseThrow();
    assertEquals("Produto de integração", consultado.getNome());
    assertEquals(new BigDecimal("100.00"), consultado.getCusto());
    assertEquals(new BigDecimal("125.00"), consultado.getPrecoVenda());
    assertEquals(new BigDecimal("20.0000"), consultado.getPercentual());
    assertEquals(ModalidadePrecificacao.MARGEM_SOBRE_VENDA, consultado.getModalidadePrecificacao());
    assertTrue(repositorio.buscarPorCodigo(Long.MAX_VALUE).isEmpty());
  }
}
