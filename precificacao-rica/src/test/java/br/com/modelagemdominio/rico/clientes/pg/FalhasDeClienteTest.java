package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import java.lang.reflect.Proxy;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FalhasDeClienteTest {

  private final List<LogRecord> logs = new ArrayList<>();
  private final Logger registro = Logger.getAnonymousLogger();

  FalhasDeClienteTest() {
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

  @Test
  void classificaViolacaoDeCpfConformeAOperacao() {
    var causa = new PSQLException(new ServerErrorMessage(
      "SERROR\0C23505\0Mdetalhe do banco\0ncliente_cpf_key\0"));
    var gravacao = new FalhaNaGravacaoCliente(causa, this.registro);

    var cadastro = gravacao.noCadastro();
    assertEquals(FalhaCliente.Motivo.CPF_OU_EMAIL_DUPLICADO, cadastro.motivo());
    assertSame(causa, cadastro.getCause());
    assertTrue(this.logs.isEmpty());

    var alteracao = gravacao.naAlteracao();
    assertEquals(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, alteracao.motivo());
    assertSame(causa, alteracao.getCause());
    this.conferirLog("Falha ao alterar cliente (SQLState: 23505).");
  }

  @Test
  void mantemFalhaSemMetadadosComoIndisponibilidade() {
    var causa = new PSQLException("detalhe do banco", PSQLState.FOREIGN_KEY_VIOLATION);

    var falha = new FalhaNaGravacaoCliente(causa, this.registro).noCadastro();

    assertEquals(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha.motivo());
    assertSame(causa, falha.getCause());
    this.conferirLog("Falha ao cadastrar cliente (SQLState: 23503).");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void conservaFalhaOriginalQuandoRollbackFalha(boolean mesmaInstancia) {
    var original = new SQLException("falha original", "23503");
    var rollback = mesmaInstancia ? original : new SQLException("detalhe do rollback", "08006");
    var conexao = Connection.class.cast(Proxy.newProxyInstance(
      Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, metodo, argumentos) -> {
        assertEquals("rollback", metodo.getName());
        throw rollback;
      }
    ));

    new ReversaoDaAlteracaoCliente(conexao, this.registro).desfazer(original);

    if (mesmaInstancia) {
      assertEquals(0, original.getSuppressed().length);
    } else {
      assertEquals(1, original.getSuppressed().length);
      assertSame(rollback, original.getSuppressed()[0]);
    }
    this.conferirLog("Falha ao desfazer alteração do cliente (SQLState: " + rollback.getSQLState() + ").");
  }

  private void conferirLog(String mensagem) {
    assertEquals(1, this.logs.size());
    assertEquals(Level.SEVERE, this.logs.get(0).getLevel());
    assertEquals(mensagem, this.logs.get(0).getMessage());
    assertNull(this.logs.get(0).getThrown());
  }
}
