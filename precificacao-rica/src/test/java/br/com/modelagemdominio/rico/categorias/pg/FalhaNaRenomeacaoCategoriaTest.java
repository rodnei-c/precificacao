package br.com.modelagemdominio.rico.categorias.pg;

import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FalhaNaRenomeacaoCategoriaTest {

  @Test
  void reconheceDuplicidadeDaRestricaoDeNomeSemRegistrarIndisponibilidade() {
    var causa = postgres("23505", "ux_categoria_nome");
    var registros = new ArrayList<LogRecord>();

    var falha = new FalhaNaRenomeacaoCategoria(causa, registro(registros)).traduzida();

    assertEquals(FalhaCategoria.Motivo.NOME_DUPLICADO, falha.motivo());
    assertEquals("NOME_DUPLICADO", falha.getMessage());
    assertSame(causa, falha.getCause());
    assertTrue(registros.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("falhasDePersistencia")
  void mantemOutrasFalhasComoIndisponibilidadeComCausaELogSemDetalhesDaExcecao(SQLException causa) {
    var registros = new ArrayList<LogRecord>();

    var falha = new FalhaNaRenomeacaoCategoria(causa, registro(registros)).traduzida();

    assertEquals(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha.motivo());
    assertEquals("PERSISTENCIA_INDISPONIVEL", falha.getMessage());
    assertSame(causa, falha.getCause());
    assertEquals(1, registros.size());
    assertEquals(Level.SEVERE, registros.get(0).getLevel());
    assertEquals("Falha ao renomear categoria (SQLState: " + causa.getSQLState() + ").",
      registros.get(0).getMessage());
    assertNull(registros.get(0).getThrown());
  }

  private static Stream<SQLException> falhasDePersistencia() {
    return Stream.of(
      postgres("23505", "outra_restricao"),
      postgres("23503", "ux_categoria_nome"),
      new PSQLException("detalhe sigiloso", PSQLState.UNIQUE_VIOLATION),
      new SQLException("detalhe sigiloso ux_categoria_nome", "23505"),
      new SQLException("detalhe sigiloso")
    );
  }

  private static PSQLException postgres(String estado, String restricao) {
    return new PSQLException(new ServerErrorMessage(
      "SERROR\0C" + estado + "\0Mdetalhe sigiloso\0n" + restricao + "\0"));
  }

  private Logger registro(List<LogRecord> registros) {
    var destino = Logger.getAnonymousLogger();
    destino.setUseParentHandlers(false);
    destino.setLevel(Level.ALL);
    destino.addHandler(new Handler() {
      @Override
      public void publish(LogRecord registro) {
        registros.add(registro);
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() {
      }
    });
    return destino;
  }
}
