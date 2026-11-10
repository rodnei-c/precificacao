package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.SqlContado;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class ListaFaturasPostgresTest {

  private static final JsonMapper JSON = new JsonMapper();

  @Test
  void filtraOrdenaEContaProdutosSemLerPrecosOuClientes() throws Exception {
    try (var cenario = new CenarioFatura(); var leitura = Leitura.criar(cenario)) {
      long outroCliente = cenario.novoCliente("11144477735");
      long clienteSemFaturas = cenario.novoCliente("12345678909");
      JsonNode inicio = criar(cenario, "2026-11-01T00:00:00", cenario.cliente, "PIX", 2);
      JsonNode fimA = criar(cenario, "2026-11-03T23:59:59.999999", cenario.cliente, null, 2);
      JsonNode anterior = criar(cenario, "2026-10-31T23:59:59.999999", cenario.cliente, null, 2);
      JsonNode posterior = criar(cenario, "2026-11-04T00:00:00", null, null, 1);
      JsonNode meio = criar(cenario, "2026-11-02T12:00:00", outroCliente, "BOLETO", 1);
      JsonNode fimB = criar(cenario, "2026-11-03T23:59:59.999999", outroCliente, null, 2);

      JsonNode todos = lista(posterior, fimB, fimA, meio, inicio, anterior);
      assertEquals(todos, leitura.obter("", 200));
      assertEquals(lista(posterior, fimB, fimA, meio, inicio), leitura.obter("inicio=2026-11-01", 200));
      assertEquals(lista(fimB, fimA, meio, inicio, anterior), leitura.obter("fim=2026-11-03", 200));
      assertEquals(lista(fimB, fimA, meio, inicio), leitura.obter("inicio=2026-11-01&fim=2026-11-03", 200));
      assertEquals(lista(fimA, inicio, anterior), leitura.obter("codigoCliente=" + cenario.cliente, 200));
      assertEquals(lista(fimB, meio), leitura.obter("codigoCliente=" + outroCliente, 200));
      String combinados = "inicio=2026-11-01&fim=2026-11-03&codigoCliente=" + cenario.cliente;
      assertEquals(lista(fimA, inicio), leitura.obter(combinados, 200));
      assertEquals(lista(fimA, inicio, anterior), leitura.obter("fim=2026-11-03&codigoCliente=" + cenario.cliente, 200));
      assertEquals(lista(fimB, meio), leitura.obter("inicio=2026-11-01&codigoCliente=" + outroCliente, 200));
      assertEquals(lista(fimB, fimA), leitura.obter("inicio=2026-11-03&fim=2026-11-03", 200));
      assertEquals(lista(), leitura.obter("inicio=2026-11-05", 200));
      assertEquals(lista(), leitura.obter("codigoCliente=" + clienteSemFaturas, 200));
      assertEquals(lista(), leitura.obter("codigoCliente=" + Long.MAX_VALUE, 200));
      assertEquals(todos, leitura.obter("inicio=&fim=+++&codigoCliente=", 200));
      assertEquals(lista(fimA, inicio), leitura.obter(
        "%69nicio=2026%2D11%2D01&fim=2026-11-03&codigoCliente=%20" + cenario.cliente + "%20", 200));

      // Uma falha real de leitura deve ser 503, sem ser convertida em lista vazia.
      try (var conexao = cenario.banco.getConnection(); var comando = conexao.createStatement()) {
        comando.execute("REVOKE SELECT ON public.item_fatura FROM " + leitura.papel);
        try {
          assertEquals("Não foi possível concluir a operação no banco de dados.",
            leitura.obter("", 503).path("detail").asString());
        } finally {
          comando.execute("GRANT SELECT ON public.item_fatura TO " + leitura.papel);
        }
      }
      assertEquals(todos, leitura.obter("", 200));
    }
  }

  @Test
  void incluiAsDatasExtremasAceitasSemPerderOFinalDoUltimoDia() throws Exception {
    try (var cenario = new CenarioFatura(); var leitura = Leitura.criar(cenario)) {
      JsonNode primeiro = criar(cenario, "0001-01-01T00:00:00", cenario.cliente, null, 1);
      JsonNode ultimo = criar(cenario, "9999-12-31T23:59:59.999999", null, "DINHEIRO", 2);

      assertEquals(lista(ultimo, primeiro), leitura.obter("inicio=0001-01-01&fim=9999-12-31", 200));
      assertEquals(lista(ultimo), leitura.obter("inicio=9999-12-31&fim=9999-12-31", 200));
      assertEquals(lista(primeiro), leitura.obter("inicio=0001-01-01&fim=0001-01-01", 200));
    }
  }

  private static JsonNode criar(CenarioFatura cenario, String data, Long cliente, String pagamento, int quantidadeItens)
    throws Exception {
    var pedido = cenario.pedido();
    if (cliente == null) {
      pedido.remove("codigoCliente");
    } else {
      pedido.put("codigoCliente", cliente);
    }
    if (pagamento != null) {
      pedido.put("formaPagamento", pagamento);
    }
    if (quantidadeItens == 1) {
      ((ArrayNode) pedido.path("itens")).remove(1);
      ((ObjectNode) pedido.path("itens").get(0)).put("quantidade", new java.math.BigDecimal("0.125"));
    }
    long codigo = cenario.cadastrar(pedido, 201).path("codigo").asLong();
    LocalDateTime horario = LocalDateTime.parse(data);
    try (var conexao = cenario.banco.getConnection();
      var atualizar = conexao.prepareStatement("UPDATE public.fatura SET data_criacao = ? WHERE id = ?")) {
      atualizar.setObject(1, horario);
      atualizar.setLong(2, codigo);
      assertEquals(1, atualizar.executeUpdate());
    }
    var esperado = JSON.createObjectNode().put("codigo", codigo)
      .put("dataCriacao", horario.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).put("quantidadeItens", quantidadeItens);
    if (cliente != null) {
      esperado.put("codigoCliente", cliente);
    }
    if (pagamento != null) {
      esperado.put("formaPagamento", pagamento);
    }
    return JSON.readTree(JSON.writeValueAsString(esperado));
  }

  private static JsonNode lista(JsonNode... resumos) {
    var lista = JSON.createArrayNode();
    for (JsonNode resumo : resumos) {
      lista.add(resumo);
    }
    return lista;
  }

  private record Leitura(Take api, SqlContado sql, PGSimpleDataSource administrador, String papel) implements AutoCloseable {

    static Leitura criar(CenarioFatura cenario) throws Exception {
      String papel = "teste_resumo_" + UUID.randomUUID().toString().replace("-", "");
      String senha = UUID.randomUUID().toString();
      try (var conexao = cenario.banco.getConnection(); var comando = conexao.createStatement()) {
        comando.execute("CREATE ROLE " + papel + " LOGIN PASSWORD '" + senha + "'");
        try {
          comando.execute("GRANT USAGE ON SCHEMA public TO " + papel);
          comando.execute("GRANT SELECT ON public.fatura, public.item_fatura TO " + papel);
          var banco = new PGSimpleDataSource();
          banco.setURL(cenario.banco.getURL());
          banco.setUser(papel);
          banco.setPassword(senha);
          banco.setOptions("-c default_transaction_read_only=on");
          try (var leitura = banco.getConnection(); var consulta = leitura.createStatement();
            var resultado = consulta.executeQuery("""
              SELECT current_setting('transaction_read_only') AS somente_leitura,
                has_table_privilege(current_user, 'public.produto', 'SELECT') AS produtos,
                has_table_privilege(current_user, 'public.cliente', 'SELECT') AS clientes
              """)) {
            assertTrue(resultado.next());
            assertEquals("on", resultado.getString("somente_leitura"));
            assertFalse(resultado.getBoolean("produtos"));
            assertFalse(resultado.getBoolean("clientes"));
          }
          var sql = new SqlContado(banco);
          return new Leitura(AplicacaoRica.rotas(sql.banco()), sql, cenario.banco, papel);
        } catch (Exception | AssertionError falha) {
          try {
            limpar(cenario.banco, papel);
          } catch (SQLException limpeza) {
            falha.addSuppressed(limpeza);
          }
          throw falha;
        }
      }
    }

    JsonNode obter(String filtros, int status) throws Exception {
      this.sql.zerar();
      var resposta = new RsPrint(this.api.act(new RqFake("GET", "/api/faturas" + (filtros.isEmpty() ? "" : "?" + filtros))));
      JsonNode dados = JSON.readTree(resposta.printBody());
      assertEquals(Integer.toString(status), resposta.head().iterator().next().split(" ", 3)[1], dados.toString());
      assertEquals(1, this.sql.conexoes());
      assertEquals(1, this.sql.comandos(), "A listagem deve buscar somente os resumos em uma consulta.");
      return dados;
    }

    @Override
    public void close() throws SQLException {
      limpar(this.administrador, this.papel);
    }

    private static void limpar(PGSimpleDataSource administrador, String papel) throws SQLException {
      try (var conexao = administrador.getConnection(); var comando = conexao.createStatement()) {
        comando.execute("REVOKE SELECT ON public.fatura, public.item_fatura FROM " + papel);
        comando.execute("REVOKE USAGE ON SCHEMA public FROM " + papel);
        comando.execute("DROP ROLE " + papel);
      }
    }
  }
}
