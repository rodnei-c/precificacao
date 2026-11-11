package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FalhaNaGravacaoProdutoTest {

  private static final String REFERENCIA_CATEGORIA = "produto_categoria_codigo_fkey";

  @Test
  void reconheceCategoriaAusenteQuandoAGravacaoIncluiAVinculacao() {
    var causa = postgres("23503", REFERENCIA_CATEGORIA);
    var logs = new ArrayList<LogRecord>();

    var falha = new FalhaNaGravacaoProduto(causa, true, registro(logs)).traduzida();

    assertEquals(FalhaProduto.Motivo.CATEGORIA_AUSENTE, falha.motivo());
    assertSame(causa, falha.getCause());
    assertTrue(logs.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("falhasDePersistencia")
  void registraOutrasFalhasComCausaPreservada(SQLException causa, boolean comCategoria) {
    var logs = new ArrayList<LogRecord>();

    var falha = new FalhaNaGravacaoProduto(causa, comCategoria, registro(logs)).traduzida();

    assertEquals(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha.motivo());
    assertSame(causa, falha.getCause());
    assertEquals(1, logs.size());
    assertEquals(Level.SEVERE, logs.get(0).getLevel());
    assertEquals("Falha ao gravar produto (SQLState: " + causa.getSQLState() + ").", logs.get(0).getMessage());
    assertNull(logs.get(0).getThrown());
  }

  private static Stream<Arguments> falhasDePersistencia() {
    return Stream.of(
      Arguments.of(postgres("23503", REFERENCIA_CATEGORIA), false),
      Arguments.of(postgres("23503", "outra_referencia"), true),
      Arguments.of(postgres("23505", REFERENCIA_CATEGORIA), true),
      Arguments.of(new PSQLException("detalhe da falha", PSQLState.FOREIGN_KEY_VIOLATION), true),
      Arguments.of(new SQLException("detalhe da falha", "23503"), true)
    );
  }

  private static PSQLException postgres(String estado, String restricao) {
    return new PSQLException(new ServerErrorMessage(
      "SERROR\0C" + estado + "\0Mdetalhe da falha\0n" + restricao + "\0"));
  }

  private static Logger registro(List<LogRecord> logs) {
    var destino = Logger.getAnonymousLogger();
    destino.setUseParentHandlers(false);
    destino.addHandler(new Handler() {
      @Override
      public void publish(LogRecord log) {
        logs.add(log);
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
