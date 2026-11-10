package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import br.com.modelagemdominio.rico.faturas.ResumoFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.ValoresFatura;
import br.com.modelagemdominio.rico.faturas.http.TkConsultaFatura;
import br.com.modelagemdominio.rico.faturas.pg.PgFaturas;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.takes.Take;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class ConsultaFaturaPostgresTest {

  private static final JsonMapper JSON = new JsonMapper();

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void consultaMesmosCamposDoCadastroComVinculosIndependentesSemEscrever(boolean cliente, boolean pagamento)
    throws Exception {
    try (var cenario = new CenarioFatura()) {
      var pedido = cenario.pedido();
      if (pagamento) {
        pedido.put("formaPagamento", "BOLETO");
      }
      if (!cliente) {
        pedido.remove("codigoCliente");
      }
      JsonNode original = cenario.cadastrar(pedido, 201);
      var consulta = Consulta.somenteLeitura(cenario);

      JsonNode resposta = consulta.obter(original.path("codigo").asLong(), 200);

      assertEquals(original, resposta);
      assertEquals(cliente, resposta.has("codigoCliente"));
      assertEquals(pagamento, resposta.has("formaPagamento"));
      numero("1.03", resposta.path("total"));
    }
  }

  @Test
  void preservaPrecisaoDoItemEDoPercentualNaConsulta() throws Exception {
    try (var cenario = new CenarioFatura("1.23")) {
      var pedido = cenario.pedido();
      ((ObjectNode) pedido.path("itens").get(0)).put("quantidade", new BigDecimal("0.1234"));
      pedido.set("desconto", JSON.createObjectNode().put("tipo", "PERCENTUAL")
        .put("valor", new BigDecimal("12.3456")));
      JsonNode original = cenario.cadastrar(pedido, 201);

      JsonNode resposta = Consulta.somenteLeitura(cenario).obter(original.path("codigo").asLong(), 200);

      assertEquals(original, resposta);
      numero("1.23", resposta.path("itens").get(0).path("precoVenda"));
      numero("0.151782", resposta.path("itens").get(0).path("subtotal"));
      numero("0.151782", resposta.path("subtotal"));
      numero("0.018738398592", resposta.path("descontoAplicado"));
      numero("0.13", resposta.path("total"));
    }
  }

  @Test
  void acompanhaPrecoAtualLimitaFixoERetomaOriginalSemModificarLeituraAnterior() throws Exception {
    try (var cenario = new CenarioFatura("100.00")) {
      var pedido = cenario.pedido();
      ((ObjectNode) pedido.path("itens").get(0)).put("quantidade", 1);
      pedido.set("desconto", JSON.createObjectNode().put("tipo", "VALOR_FIXO").put("valor", 30));
      JsonNode original = cenario.cadastrar(pedido, 201);
      long codigo = original.path("codigo").asLong();
      var consulta = Consulta.somenteLeitura(cenario);
      Fatura leituraAnterior = new PgFaturas(cenario.banco).fatura(codigo);
      assertEquals(original, consulta.obter(codigo, 200));

      alterarPreco(cenario, "12.34");
      JsonNode reduzida = consulta.obter(codigo, 200);
      assertEquals(normalizar(esperadoComPreco(original, "12.34", "12.34", "0.00")), normalizar(reduzida));

      alterarPreco(cenario, "50.00");
      JsonNode recuperada = consulta.obter(codigo, 200);
      assertEquals(normalizar(esperadoComPreco(original, "50.00", "30.00", "20.00")), normalizar(recuperada));
      assertEquals(0, new BigDecimal("100.00").compareTo(leituraAnterior.subtotal()));
      assertEquals(new BigDecimal("70.00"), leituraAnterior.total());
      try (var conexao = cenario.banco.getConnection();
        var dados = conexao.prepareStatement("SELECT tipo_desconto, valor_desconto FROM public.fatura WHERE id = ?")) {
        dados.setLong(1, codigo);
        try (var resultado = dados.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals("VALOR_FIXO", resultado.getString("tipo_desconto"));
          assertEquals(0, new BigDecimal("30.00").compareTo(resultado.getBigDecimal("valor_desconto")));
        }
      }
    }
  }

  @Test
  void mudancaEntreLeituraESerializacaoNaoMisturaPrecosDaResposta() throws Exception {
    try (var cenario = new CenarioFatura("10.00", "20.00")) {
      var pedido = cenario.pedido();
      for (JsonNode item : pedido.path("itens")) {
        ((ObjectNode) item).put("quantidade", 1);
      }
      pedido.set("desconto", JSON.createObjectNode().put("tipo", "PERCENTUAL").put("valor", 10));
      JsonNode original = cenario.cadastrar(pedido, 201);
      long codigo = original.path("codigo").asLong();
      var consulta = Consulta.somenteLeitura(cenario);
      var durante = new FaturasComAlteracao(new PgFaturas(consulta.sql.banco()), () -> aumentarPrecos(cenario));
      var api = new TkFork(new FkRegex("/api/faturas/([^/]+)", new TkConsultaFatura(durante)));

      assertEquals(original, new Consulta(api, consulta.sql).obter(codigo, 200));

      var esperado = ((ObjectNode) original).deepCopy();
      ((ObjectNode) esperado.path("itens").get(0)).put("precoVenda", new BigDecimal("30.00"))
        .put("subtotal", new BigDecimal("30.00"));
      ((ObjectNode) esperado.path("itens").get(1)).put("precoVenda", new BigDecimal("20.00"))
        .put("subtotal", new BigDecimal("20.00"));
      esperado.put("subtotal", new BigDecimal("50.00")).put("descontoAplicado", new BigDecimal("5.00"))
        .put("total", new BigDecimal("45.00"));
      JsonNode novaLeitura = consulta.obter(codigo, 200);
      assertEquals(normalizar(esperado), normalizar(novaLeitura));
    }
  }

  @Test
  void retorna404ParaFaturaRemovida() throws Exception {
    try (var cenario = new CenarioFatura()) {
      long codigo = cenario.cadastrar(cenario.pedido(), 201).path("codigo").asLong();
      try (var conexao = cenario.banco.getConnection();
        var itens = conexao.prepareStatement("DELETE FROM public.item_fatura WHERE fatura_id = ?");
        var fatura = conexao.prepareStatement("DELETE FROM public.fatura WHERE id = ?")) {
        itens.setLong(1, codigo);
        itens.executeUpdate();
        fatura.setLong(1, codigo);
        assertEquals(1, fatura.executeUpdate());
      }

      assertEquals(JSON.readTree("""
        {"title":"Registro não encontrado","status":404,"detail":"Fatura não encontrada.",
         "instance":"/api/faturas/%d"}
        """.formatted(codigo)), Consulta.somenteLeitura(cenario).obter(codigo, 404));
    }
  }

  @Test
  void faturaSemItensRetorna503SemRepararOuAlterarOsDados() throws Exception {
    try (var cenario = new CenarioFatura()) {
      JsonNode original = cenario.cadastrar(cenario.pedido(), 201);
      long codigo = original.path("codigo").asLong();
      try (var conexao = cenario.banco.getConnection();
        var excluir = conexao.prepareStatement("DELETE FROM public.item_fatura WHERE fatura_id = ?")) {
        excluir.setLong(1, codigo);
        excluir.executeUpdate();
      }
      var consulta = Consulta.somenteLeitura(cenario);
      assertEquals("Não foi possível concluir a operação no banco de dados.",
        consulta.obter(codigo, 503).path("detail").asString());
      try (var conexao = cenario.banco.getConnection();
        var dados = conexao.prepareStatement("""
          SELECT (SELECT count(*) FROM public.fatura WHERE id = ?) AS faturas,
            (SELECT count(*) FROM public.item_fatura WHERE fatura_id = ?) AS itens
          """); var inserir = conexao.prepareStatement("""
          INSERT INTO public.item_fatura (fatura_id, produto_codigo, quantidade, posicao) VALUES (?, ?, ?, ?)
          """)) {
        dados.setLong(1, codigo);
        dados.setLong(2, codigo);
        try (var resultado = dados.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(1, resultado.getInt("faturas"));
          assertEquals(0, resultado.getInt("itens"));
        }
        for (int posicao = 0; posicao < original.path("itens").size(); posicao++) {
          JsonNode item = original.path("itens").get(posicao);
          inserir.setLong(1, codigo);
          inserir.setLong(2, item.path("codigoProduto").asLong());
          inserir.setBigDecimal(3, item.path("quantidade").decimalValue());
          inserir.setInt(4, posicao);
          inserir.executeUpdate();
        }
      }
      assertEquals(original, consulta.obter(codigo, 200));
    }
  }

  private static void alterarPreco(CenarioFatura cenario, String preco) throws Exception {
    byte[] corpo = JSON.writeValueAsBytes(JSON.createObjectNode().put("custo", 1)
      .put("precoVenda", new BigDecimal(preco)));
    var resposta = new RsPrint(cenario.api.act(new RqFake(List.of(
      "PATCH /api/produtos/" + cenario.produtos.get(0) + "/precificacao HTTP/1.1",
      "Content-Type: application/json", "Content-Length: " + corpo.length), corpo)));
    assertEquals("200", resposta.head().iterator().next().split(" ", 3)[1], resposta.printBody());
  }

  private static ObjectNode esperadoComPreco(JsonNode original, String preco, String aplicado, String total) {
    var esperado = ((ObjectNode) original).deepCopy();
    ((ObjectNode) esperado.path("itens").get(0)).put("precoVenda", new BigDecimal(preco))
      .put("subtotal", new BigDecimal(preco));
    return esperado.put("subtotal", new BigDecimal(preco)).put("descontoAplicado", new BigDecimal(aplicado))
      .put("total", new BigDecimal(total));
  }

  private static void aumentarPrecos(CenarioFatura cenario) {
    try (var conexao = cenario.banco.getConnection(); var comando = conexao.prepareStatement("""
      UPDATE public.produto SET preco_venda = preco_venda + 10, percentual = percentual + 1000
      WHERE codigo = ANY (?)
      """)) {
      var produtos = conexao.createArrayOf("bigint", cenario.produtos.toArray(Long[]::new));
      try {
        comando.setArray(1, produtos);
        assertEquals(2, comando.executeUpdate());
      } finally {
        produtos.free();
      }
    } catch (SQLException falha) {
      throw new AssertionError("Não foi possível alterar os preços após a leitura do teste.", falha);
    }
  }

  private static JsonNode normalizar(JsonNode valor) {
    if (valor.isNumber()) {
      return JSON.getNodeFactory().numberNode(valor.decimalValue().stripTrailingZeros());
    }
    if (valor.isObject()) {
      var objeto = JSON.createObjectNode();
      for (String campo : valor.propertyNames()) {
        objeto.set(campo, normalizar(valor.path(campo)));
      }
      return objeto;
    }
    if (valor.isArray()) {
      var lista = JSON.createArrayNode();
      for (JsonNode item : valor) {
        lista.add(normalizar(item));
      }
      return lista;
    }
    return valor;
  }

  private static void numero(String esperado, JsonNode valor) {
    assertTrue(valor.isNumber());
    assertEquals(0, new BigDecimal(esperado).compareTo(valor.decimalValue()), valor.toString());
  }

  private record Consulta(Take api, SqlContado sql) {

    static Consulta somenteLeitura(CenarioFatura cenario) throws SQLException {
      var banco = cenario.bancoSomenteLeitura();
      try (var conexao = banco.getConnection(); var consulta = conexao.createStatement();
        var resultado = consulta.executeQuery("SHOW transaction_read_only")) {
        assertTrue(resultado.next());
        assertEquals("on", resultado.getString(1));
      }
      var sql = new SqlContado(banco);
      return new Consulta(AplicacaoRica.rotas(sql.banco()), sql);
    }

    JsonNode obter(long codigo, int status) throws Exception {
      this.sql.zerar();
      var resposta = new RsPrint(this.api.act(new RqFake("GET", "/api/faturas/" + codigo)));
      JsonNode corpo = JSON.readTree(resposta.printBody());
      assertEquals(Integer.toString(status), resposta.head().iterator().next().split(" ", 3)[1], corpo.toString());
      assertEquals(1, this.sql.conexoes());
      assertEquals(1, this.sql.comandos(), "Cabeçalho, itens e preços devem vir da mesma consulta.");
      return corpo;
    }
  }

  private record FaturasComAlteracao(Faturas origem, Runnable alterarPrecos) implements Faturas {

    @Override
    public Fatura fatura(long codigo) throws FalhaFatura {
      Fatura lida = this.origem.fatura(codigo);
      this.alterarPrecos.run();
      return lida;
    }

    @Override
    public Fatura cadastrar(CalculoFatura calculo, VinculosFatura vinculos) throws FalhaFatura {
      return this.origem.cadastrar(calculo, vinculos);
    }

    @Override
    public ValoresFatura simular(CalculoFatura calculo, VinculosFatura vinculos) throws FalhaFatura {
      return this.origem.simular(calculo, vinculos);
    }

    @Override
    public List<ResumoFatura> listar(FiltrosFaturas filtros) throws FalhaFatura {
      return this.origem.listar(filtros);
    }
  }
}
