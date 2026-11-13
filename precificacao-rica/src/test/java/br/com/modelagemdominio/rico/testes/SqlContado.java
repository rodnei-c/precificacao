package br.com.modelagemdominio.rico.testes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

/** Conta comandos JDBC da aplicação, preservando resultados e exceções do driver. */
public final class SqlContado {

  private final DataSource origem;
  private final AtomicInteger comandos;
  private final AtomicInteger conexoes;

  /** @param origem fonte real das conexões de teste */
  public SqlContado(DataSource origem) {
    this.origem = origem;
    this.comandos = new AtomicInteger();
    this.conexoes = new AtomicInteger();
  }

  /** @return fonte que observa a execução dos comandos JDBC */
  public DataSource banco() {
    return this.observar(DataSource.class, this.origem);
  }

  /** @return comandos executados desde o início ou desde a última chamada a zerar */
  public int comandos() {
    return this.comandos.get();
  }

  /** @return solicitações de conexão da aplicação na operação observada */
  public int conexoes() {
    return this.conexoes.get();
  }

  /** Inicia a contagem de uma nova operação. */
  public void zerar() {
    this.comandos.set(0);
    this.conexoes.set(0);
  }

  private <T> T observar(Class<T> tipo, T alvo) {
    return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (proxy, metodo, argumentos) -> {
      if (alvo instanceof DataSource && "getConnection".equals(metodo.getName())) {
        this.conexoes.incrementAndGet();
      }
      if (alvo instanceof Statement && metodo.getName().startsWith("execute")) {
        this.comandos.incrementAndGet();
      }
      Object resultado;
      try {
        resultado = metodo.invoke(alvo, argumentos);
      } catch (InvocationTargetException falha) {
        throw falha.getCause();
      }
      if (resultado instanceof Connection conexao) {
        return this.observar(Connection.class, conexao);
      }
      if (resultado instanceof PreparedStatement comando) {
        return this.observar(PreparedStatement.class, comando);
      }
      if (resultado instanceof Statement comando) {
        return this.observar(Statement.class, comando);
      }
      return resultado;
    }));
  }
}
