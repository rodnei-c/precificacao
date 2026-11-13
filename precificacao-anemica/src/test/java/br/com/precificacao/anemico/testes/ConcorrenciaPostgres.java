package br.com.precificacao.anemico.testes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Sincroniza operações de teste por bloqueios observáveis no PostgreSQL. */
public final class ConcorrenciaPostgres {

  private static final String CONSULTAS_BLOQUEADAS = """
    WITH RECURSIVE bloqueados(pid) AS (
      SELECT pid FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid))
      UNION
      SELECT atividade.pid FROM pg_stat_activity atividade
      JOIN bloqueados ON bloqueados.pid = ANY(pg_blocking_pids(atividade.pid))
    )
    SELECT atividade.query FROM bloqueados
    JOIN pg_stat_activity atividade USING (pid)
    WHERE atividade.datname = current_database() AND atividade.backend_type = 'client backend'
    ORDER BY atividade.pid
    """;

  private ConcorrenciaPostgres() {
  }

  /**
   * @param conexao sessão que mantém o bloqueio de preparação do teste
   * @return identificador da sessão no PostgreSQL
   * @throws SQLException quando não for possível consultar a sessão
   */
  public static int codigoDaSessao(Connection conexao) throws SQLException {
    try (var comando = conexao.createStatement()) {
      comando.setQueryTimeout(2);
      try (var resultado = comando.executeQuery("SELECT pg_backend_pid()")) {
        resultado.next();
        return resultado.getInt(1);
      }
    }
  }

  /**
   * @param observador conexão independente, com confirmação automática
   * @param codigoSessao sessão que impede o andamento das operações
   * @param quantidade número de operações que precisam estar bloqueadas
   * @return consultas bloqueadas direta ou indiretamente pela sessão
   * @throws SQLException quando a observação do banco falhar
   * @throws InterruptedException quando a espera for interrompida
   */
  public static List<String> aguardarConsultasBloqueadas(
    Connection observador, int codigoSessao, int quantidade
  ) throws SQLException, InterruptedException {
    long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    List<String> ultimasConsultas = List.of();
    try (var consulta = observador.prepareStatement(CONSULTAS_BLOQUEADAS)) {
      consulta.setInt(1, codigoSessao);
      consulta.setQueryTimeout(2);
      while (System.nanoTime() < limite) {
        List<String> consultas = new ArrayList<>();
        try (var resultado = consulta.executeQuery()) {
          while (resultado.next()) {
            consultas.add(resultado.getString(1));
          }
        }
        if (consultas.size() == quantidade) {
          return consultas;
        }
        ultimasConsultas = consultas;
        Thread.sleep(25);
      }
    }
    throw new AssertionError("Esperadas " + quantidade + " consultas simultaneamente bloqueadas; observadas: "
      + ultimasConsultas.size() + ". Consultas: " + ultimasConsultas);
  }

  /**
   * Aguarda o encerramento das tarefas durante a limpeza, sem substituir a falha original.
   * O teste deve conferir os resultados das tarefas após liberar o bloqueio.
   *
   * @param operacoes tarefas iniciadas, incluindo posições ainda não preenchidas
   * @throws InterruptedException quando a espera for interrompida
   * @throws ExecutionException quando a coordenação das tarefas falhar
   * @throws TimeoutException quando alguma tarefa não encerrar em quarenta segundos
   */
  public static void aguardarConclusao(CompletableFuture<?>... operacoes)
    throws InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<?>[] iniciadas = Arrays.stream(operacoes).filter(Objects::nonNull)
      .toArray(CompletableFuture<?>[]::new);
    CompletableFuture.allOf(iniciadas).handle((resultado, falha) -> null).get(40, TimeUnit.SECONDS);
  }
}
