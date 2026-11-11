package br.com.modelagemdominio.rico.cidades.pg;

import br.com.modelagemdominio.rico.cidades.CidadeConstante;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CidadesDaConsultaTest {

  @Test
  void executaSomenteNaLeituraEFechaOResultadoAoMaterializar() throws SQLException {
    var leituras = new AtomicInteger();
    var fechamentos = new AtomicInteger();
    var resultado = simular(ResultSet.class, (proxy, metodo, argumentos) -> switch (metodo.getName()) {
      case "next" -> leituras.getAndIncrement() == 0;
      case "getLong" -> 7L;
      case "getString" -> "nome".equals(argumentos[0]) ? "Curitiba" : "PR";
      case "close" -> fechamentos.incrementAndGet();
      default -> throw new AssertionError(metodo);
    });
    var execucoes = new AtomicInteger();
    var cidades = new CidadesDaConsulta(consulta(resultado, execucoes));

    assertEquals(0, execucoes.get());
    var lista = cidades.cidades();

    var cidade = new CidadeConstante(7, new NomeCidade("Curitiba"), UnidadeFederativa.PR);
    assertEquals(List.of(cidade), lista);
    assertThrows(UnsupportedOperationException.class, () -> lista.add(cidade));
    assertEquals(1, execucoes.get());
    assertEquals(1, fechamentos.get());
  }

  @Test
  void fechaOResultadoQuandoUmaColunaFalha() {
    var falha = new SQLException("Falha ao ler nome");
    var fechamentos = new AtomicInteger();
    var resultado = simular(ResultSet.class, (proxy, metodo, argumentos) -> switch (metodo.getName()) {
      case "next" -> true;
      case "getLong" -> 7L;
      case "getString" -> throw falha;
      case "close" -> fechamentos.incrementAndGet();
      default -> throw new AssertionError(metodo);
    });
    var cidades = new CidadesDaConsulta(consulta(resultado, new AtomicInteger()));

    assertSame(falha, assertThrows(SQLException.class, cidades::cidades));
    assertEquals(1, fechamentos.get());
  }

  private static PreparedStatement consulta(ResultSet resultado, AtomicInteger execucoes) {
    return simular(PreparedStatement.class, (proxy, metodo, argumentos) -> {
      if (!"executeQuery".equals(metodo.getName())) {
        throw new AssertionError(metodo);
      }
      execucoes.incrementAndGet();
      return resultado;
    });
  }

  private static <T> T simular(Class<T> tipo, InvocationHandler comportamento) {
    return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, comportamento));
  }
}
