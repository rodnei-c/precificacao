package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.SqlContado;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class SimulacaoFaturaPostgresTest {

  private static final JsonMapper JSON = new JsonMapper();

  @ParameterizedTest
  @CsvSource({"SEM,0,0,1.03", "PERCENTUAL,12.3456,0.12715968,0.90", "VALOR_FIXO,0.03,0.03,1.00",
    "VALOR_FIXO,1.03,1.03,0.00", "PERCENTUAL,100,1.03,0.00"})
  void confereValoresIndependentesEquivalenciaComCadastroENenhumaGravacao(
    String tipo, String valor, String aplicado, String total
  ) throws Exception {
    try (var cenario = new CenarioFatura()) {
      cenario.cadastrar(cenario.pedido(), 201);
      var pedido = cenario.pedido().put("formaPagamento", "PIX");
      if (!"SEM".equals(tipo)) {
        pedido.set("desconto", JSON.createObjectNode().put("tipo", tipo).put("valor", new BigDecimal(valor)));
      }
      EstadoDoBanco antes = EstadoDoBanco.capturar(cenario);
      JsonNode simulado = Simulacao.somenteLeitura(cenario).enviar(pedido, 200, 2);

      assertEquals(JSON.readTree("""
        {"itens":[{"codigoProduto":%d,"quantidade":0.5,"precoVenda":1.05,"subtotal":0.525},
                  {"codigoProduto":%d,"quantidade":0.5,"precoVenda":1.01,"subtotal":0.505}],
         "subtotal":1.03,"descontoAplicado":%s,"total":%s}
        """.formatted(cenario.produtos.get(1), cenario.produtos.get(0), aplicado, total)), simulado);
      assertEquals(antes, EstadoDoBanco.capturar(cenario));

      var cadastrado = ((ObjectNode) cenario.cadastrar(pedido, 201)).deepCopy();
      cadastrado.remove(List.of("codigo", "dataCriacao", "codigoCliente", "formaPagamento"));
      assertEquals(cadastrado, simulado);
    }
  }

  @Test
  void preservaPrecisaoDaQuantidadeEDoPercentualSemIdentidadeDeFatura() throws Exception {
    try (var cenario = new CenarioFatura("1.23")) {
      var pedido = cenario.pedido();
      ((ObjectNode) pedido.path("itens").get(0)).put("quantidade", new BigDecimal("0.1234"));
      pedido.set("desconto", JSON.createObjectNode().put("tipo", "PERCENTUAL")
        .put("valor", new BigDecimal("12.3456")));
      cenario.cadastrar(pedido, 201);
      EstadoDoBanco antes = EstadoDoBanco.capturar(cenario);

      JsonNode simulado = Simulacao.somenteLeitura(cenario).enviar(pedido, 200, 2);

      assertEquals(JSON.readTree("""
        {"itens":[{"codigoProduto":%d,"quantidade":0.1234,"precoVenda":1.23,"subtotal":0.151782}],
         "subtotal":0.151782,"descontoAplicado":0.018738398592,"total":0.13}
        """.formatted(cenario.produtos.get(0))), simulado);
      assertEquals(antes, EstadoDoBanco.capturar(cenario));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void aceitaOpcionaisOmitidosOuNulosSemDevolveLos(boolean explicitos) throws Exception {
    try (var cenario = new CenarioFatura()) {
      cenario.cadastrar(cenario.pedido(), 201);
      var pedido = cenario.pedido();
      pedido.remove("codigoCliente");
      if (explicitos) {
        pedido.putNull("codigoCliente").putNull("formaPagamento").putNull("desconto");
      }
      EstadoDoBanco antes = EstadoDoBanco.capturar(cenario);

      JsonNode simulado = Simulacao.somenteLeitura(cenario).enviar(pedido, 200, 1);

      assertEquals(Set.of("itens", "subtotal", "descontoAplicado", "total"), simulado.propertyNames());
      assertEquals(0, new BigDecimal("1.03").compareTo(simulado.path("total").decimalValue()));
      assertEquals(antes, EstadoDoBanco.capturar(cenario));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"cliente", "produto", "desconto"})
  void rejeitaReferenciasAusentesEDescontoInicialExcedenteSemAlterarDados(String invalido) throws Exception {
    try (var cenario = new CenarioFatura()) {
      cenario.cadastrar(cenario.pedido(), 201);
      var pedido = cenario.pedido();
      int status = 404;
      if ("cliente".equals(invalido)) {
        pedido.put("codigoCliente", Long.MAX_VALUE);
      } else if ("produto".equals(invalido)) {
        ((ObjectNode) pedido.path("itens").get(1)).put("codigoProduto", Long.MAX_VALUE);
      } else {
        pedido.set("desconto", JSON.createObjectNode().put("tipo", "VALOR_FIXO").put("valor", new BigDecimal("1.04")));
        status = 400;
      }
      EstadoDoBanco antes = EstadoDoBanco.capturar(cenario);

      JsonNode problema = Simulacao.somenteLeitura(cenario).enviar(pedido, status, "cliente".equals(invalido) ? 1 : 2);

      assertEquals("/api/faturas/simulacao", problema.path("instance").asString());
      assertEquals("cliente".equals(invalido) ? "Cliente não encontrado."
        : "produto".equals(invalido) ? "Produto não encontrado."
        : "O desconto fixo não pode ultrapassar o subtotal inicial da fatura.", problema.path("detail").asString());
      assertEquals(antes, EstadoDoBanco.capturar(cenario));
    }
  }

  @Test
  void novaSimulacaoUsaPrecoAtualSemModificarAFaturaExistenteOuResultadoAnterior() throws Exception {
    try (var cenario = new CenarioFatura()) {
      cenario.cadastrar(cenario.pedido(), 201);
      EstadoDoBanco antes = EstadoDoBanco.capturar(cenario);
      var simulacao = Simulacao.somenteLeitura(cenario);
      JsonNode original = simulacao.enviar(cenario.pedido(), 200, 2);
      byte[] corpo = JSON.writeValueAsBytes(JSON.createObjectNode().put("custo", 1).put("precoVenda", new BigDecimal("2.01")));
      var alteracao = new RsPrint(cenario.api.act(new RqFake(List.of(
        "PATCH /api/produtos/" + cenario.produtos.get(0) + "/precificacao HTTP/1.1",
        "Content-Type: application/json", "Content-Length: " + corpo.length), corpo)));
      assertEquals("200", alteracao.head().iterator().next().split(" ", 3)[1], alteracao.printBody());

      JsonNode atual = simulacao.enviar(cenario.pedido(), 200, 2);

      assertEquals(0, new BigDecimal("1.53").compareTo(atual.path("total").decimalValue()));
      assertEquals(0, new BigDecimal("2.01").compareTo(atual.path("itens").get(1).path("precoVenda").decimalValue()));
      assertEquals(0, new BigDecimal("1.03").compareTo(original.path("total").decimalValue()));
      assertEquals(antes, EstadoDoBanco.capturar(cenario));
    }
  }

  private record EstadoDoBanco(JsonNode faturas, JsonNode itens, long ultimoCodigo, boolean sequenciaUsada) {

    static EstadoDoBanco capturar(CenarioFatura cenario) throws SQLException {
      try (var conexao = cenario.banco.getConnection(); var consulta = conexao.createStatement();
        var resultado = consulta.executeQuery("""
          SELECT COALESCE((SELECT jsonb_agg(to_jsonb(f) ORDER BY f.id) FROM public.fatura f), '[]')::text AS faturas,
            COALESCE((SELECT jsonb_agg(to_jsonb(i) ORDER BY i.fatura_id, i.posicao) FROM public.item_fatura i), '[]')::text AS itens,
            last_value, is_called FROM public.fatura_id_seq
          """)) {
        assertTrue(resultado.next());
        return new EstadoDoBanco(JSON.readTree(resultado.getString("faturas")), JSON.readTree(resultado.getString("itens")),
          resultado.getLong("last_value"), resultado.getBoolean("is_called"));
      }
    }
  }

  private record Simulacao(Take api, SqlContado sql) {

    static Simulacao somenteLeitura(CenarioFatura cenario) throws SQLException {
      var banco = cenario.bancoSomenteLeitura();
      try (var conexao = banco.getConnection(); var consulta = conexao.createStatement();
        var resultado = consulta.executeQuery("SHOW transaction_read_only")) {
        assertTrue(resultado.next());
        assertEquals("on", resultado.getString(1));
      }
      var sql = new SqlContado(banco);
      return new Simulacao(new RotasDaAplicacaoRica(sql.banco()).rotas(), sql);
    }

    JsonNode enviar(JsonNode pedido, int status, int comandos) throws Exception {
      byte[] corpo = JSON.writeValueAsBytes(pedido);
      this.sql.zerar();
      var resposta = new RsPrint(this.api.act(new RqFake(List.of("POST /api/faturas/simulacao HTTP/1.1",
        "Content-Type: application/json", "Content-Length: " + corpo.length), corpo)));
      JsonNode dados = JSON.readTree(resposta.printBody());
      assertEquals(Integer.toString(status), resposta.head().iterator().next().split(" ", 3)[1], dados.toString());
      assertEquals(1, this.sql.conexoes());
      assertEquals(comandos, this.sql.comandos(), "A preparação deve consultar os produtos em conjunto.");
      return dados;
    }
  }
}
