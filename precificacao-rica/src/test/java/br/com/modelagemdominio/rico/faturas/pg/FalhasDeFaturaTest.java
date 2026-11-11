package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import java.lang.reflect.Proxy;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FalhasDeFaturaTest {

  private final List<LogRecord> logs = new ArrayList<>();
  private final Logger registro = Logger.getAnonymousLogger();

  FalhasDeFaturaTest() {
    this.registro.setUseParentHandlers(false);
    this.registro.addHandler(new Handler() {
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
  }

  @ParameterizedTest
  @CsvSource({
    "fatura_cliente_codigo_fkey, CLIENTE_AUSENTE",
    "item_fatura_produto_codigo_fkey, PRODUTO_AUSENTE"
  })
  void classificaVinculoNaCadeiaDoLoteSemPerderAExcecaoOriginal(String restricao, FalhaFatura.Motivo motivo) {
    var lote = new BatchUpdateException("detalhe do lote", "23503", new int[]{1});
    lote.setNextException(this.erroPostgres("23503", "outra_restricao_fkey"));
    lote.setNextException(this.erroPostgres("23503", restricao));

    var falha = new FalhaNaGravacaoDeFatura(lote, this.registro).traduzida();

    assertEquals(motivo, falha.motivo());
    assertSame(lote, falha.getCause());
    assertTrue(this.logs.isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "23503, outra_restricao_fkey",
    "23514, item_fatura_produto_codigo_fkey"
  })
  void classificaComoIndisponibilidadeSeEstadoOuRestricaoNaoCorrespondem(String estado, String restricao) {
    this.conferirIndisponibilidadeNaGravacao(this.erroPostgres(estado, restricao));
  }

  @Test
  void classificaComoIndisponibilidadeQuandoDriverNaoInformaRestricao() {
    this.conferirIndisponibilidadeNaGravacao(
      new PSQLException("detalhe do banco", PSQLState.FOREIGN_KEY_VIOLATION));
  }

  @Test
  void trataChaveEstrangeiraDuranteConsultaComoFalhaDeLeitura() {
    var causa = this.erroPostgres("23503", "fatura_cliente_codigo_fkey");
    var falha = new FalhaNaLeituraDeFatura(causa, this.registro).traduzida();

    assertEquals(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha.motivo());
    assertSame(causa, falha.getCause());
    this.conferirLog("Falha ao ler fatura (PSQLException, SQLState: 23503).");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void conservaCausaOriginalSeRollbackTambemFalhar(boolean mesmaInstancia) {
    var original = new SQLException("falha original", "23514");
    var rollback = mesmaInstancia ? original : new SQLException("detalhe do rollback", "08006");
    var conexao = Connection.class.cast(Proxy.newProxyInstance(
      Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, metodo, argumentos) -> {
        assertEquals("rollback", metodo.getName());
        throw rollback;
      }
    ));

    new ReversaoDaOperacaoDeFatura(conexao, this.registro).desfazer(original);

    this.conferirLog("Falha ao desfazer operação de fatura (SQLState: " + rollback.getSQLState() + ").");
    assertArrayEquals(mesmaInstancia ? new Throwable[0] : new Throwable[]{rollback}, original.getSuppressed());
  }

  private PSQLException erroPostgres(String estado, String restricao) {
    return new PSQLException(new ServerErrorMessage(
      "SERROR\0C" + estado + "\0Mdetalhe do banco\0n" + restricao + "\0"));
  }

  private void conferirIndisponibilidadeNaGravacao(SQLException causa) {
    var falha = new FalhaNaGravacaoDeFatura(causa, this.registro).traduzida();

    assertEquals(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha.motivo());
    assertSame(causa, falha.getCause());
    this.conferirLog("Falha ao cadastrar fatura (SQLState: " + causa.getSQLState() + ").");
  }

  private void conferirLog(String mensagem) {
    assertEquals(1, this.logs.size());
    var log = this.logs.get(0);
    assertEquals(Level.SEVERE, log.getLevel());
    assertEquals(mensagem, log.getMessage());
    assertNull(log.getThrown());
  }
}
