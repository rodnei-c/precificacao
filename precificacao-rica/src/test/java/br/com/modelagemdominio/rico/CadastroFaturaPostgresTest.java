package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.pg.PgFaturas;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class CadastroFaturaPostgresTest {

  private static final JsonMapper JSON = new JsonMapper();

  @ParameterizedTest
  @EnumSource(FormaPagamento.class)
  void gravaItensNaOrdemDescontoOriginalEReferenciasComCadaPagamento(FormaPagamento pagamento) throws Exception {
    try (var cenario = new CenarioFatura()) {
      var pedido = cenario.pedido().put("formaPagamento", pagamento.name());
      pedido.set("desconto", desconto("PERCENTUAL", "12.3456"));
      JsonNode resposta = cenario.cadastrar(pedido, 201);
      assertEquals(5, cenario.sql.comandos(), "As referências, escrita e retorno não fazem uma consulta por item.");
      assertEquals(cenario.cliente, resposta.path("codigoCliente").asLong());
      assertEquals(pagamento.name(), resposta.path("formaPagamento").asString());
      assertEquals(JSON.readTree("""
        [{"codigoProduto":%d,"quantidade":0.5,"precoVenda":1.05,"subtotal":0.525},
         {"codigoProduto":%d,"quantidade":0.5,"precoVenda":1.01,"subtotal":0.505}]
        """.formatted(cenario.produtos.get(1), cenario.produtos.get(0))), resposta.path("itens"));
      assertEquals(0, new java.math.BigDecimal("1.0300").compareTo(resposta.path("subtotal").decimalValue()));
      assertEquals(0, new java.math.BigDecimal("0.1271596800").compareTo(resposta.path("descontoAplicado").decimalValue()));
      assertEquals(0, new java.math.BigDecimal("0.90").compareTo(resposta.path("total").decimalValue()));
      try (var conexao = cenario.banco.getConnection(); var consulta = conexao.prepareStatement("""
        SELECT data_criacao, tipo_desconto, valor_desconto, cliente_codigo, forma_pagamento
        FROM public.fatura WHERE id = ?
        """)) {
        consulta.setLong(1, resposta.path("codigo").asLong());
        try (var resultado = consulta.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(resultado.getObject("data_criacao", LocalDateTime.class),
            LocalDateTime.parse(resposta.path("dataCriacao").asString()));
          assertEquals("PERCENTUAL", resultado.getString("tipo_desconto"));
          assertEquals(new java.math.BigDecimal("12.3456"), resultado.getBigDecimal("valor_desconto"));
          assertEquals(cenario.cliente, resultado.getLong("cliente_codigo"));
          assertEquals(pagamento.name(), resultado.getString("forma_pagamento"));
        }
        conferirItens(conexao, resposta);
      }
      // O objeto retornado pela coleção já contém sua leitura: acessores não abrem conexões.
      var fatura = new PgFaturas(cenario.sql.banco()).fatura(resposta.path("codigo").asLong());
      cenario.sql.zerar();
      assertEquals(2, fatura.itens().size());
      assertEquals(0, fatura.total().compareTo(new java.math.BigDecimal("0.90")));
      assertEquals(pagamento, fatura.vinculos().formaPagamento().orElseThrow());
      assertEquals(0, cenario.sql.conexoes());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void opcionaisOmitidosOuNulosFicamAusentesNaRespostaENoBanco(boolean explicitos) throws Exception {
    try (var cenario = new CenarioFatura()) {
      var pedido = cenario.pedido();
      pedido.remove("codigoCliente");
      if (explicitos) {
        pedido.putNull("codigoCliente").putNull("formaPagamento").putNull("desconto");
      }
      JsonNode resposta = cenario.cadastrar(pedido, 201);
      assertEquals(4, cenario.sql.comandos());
      assertFalse(resposta.has("codigoCliente"));
      assertFalse(resposta.has("formaPagamento"));
      assertEquals(0, resposta.path("descontoAplicado").decimalValue().signum());
      assertEquals(0, resposta.path("total").decimalValue().compareTo(new java.math.BigDecimal("1.03")));
      try (var conexao = cenario.banco.getConnection(); var consulta = conexao.prepareStatement("""
        SELECT num_nonnulls(tipo_desconto, valor_desconto, cliente_codigo, forma_pagamento)
        FROM public.fatura WHERE id = ?
        """)) {
        consulta.setLong(1, resposta.path("codigo").asLong());
        try (var resultado = consulta.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(0, resultado.getInt(1));
        }
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"VALOR_FIXO,0,1.03", "PERCENTUAL,0,1.03", "VALOR_FIXO,1.03,0.00"})
  void preservaTipoEValorMesmoComDescontoZeroOuTotalZero(String tipo, String valor, String total) throws Exception {
    try (var cenario = new CenarioFatura()) {
      var pedido = cenario.pedido();
      pedido.set("desconto", desconto(tipo, valor));
      JsonNode resposta = cenario.cadastrar(pedido, 201);
      assertEquals(0, new java.math.BigDecimal(total).compareTo(resposta.path("total").decimalValue()));
      try (var conexao = cenario.banco.getConnection();
        var consulta = conexao.prepareStatement("SELECT tipo_desconto, valor_desconto FROM public.fatura WHERE id = ?")) {
        consulta.setLong(1, resposta.path("codigo").asLong());
        try (var resultado = consulta.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(tipo, resultado.getString("tipo_desconto"));
          assertEquals(0, new java.math.BigDecimal(valor).compareTo(resultado.getBigDecimal("valor_desconto")));
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"cliente", "produto", "desconto"})
  void validaTudoAntesDeIniciarAEscrita(String invalido) throws Exception {
    try (var cenario = new CenarioFatura(); var gatilho = Gatilho.criar(cenario, "fatura", "BEFORE INSERT",
      "RAISE EXCEPTION 'não deveria iniciar a escrita';")) {
      var pedido = cenario.pedido();
      int status = 404;
      if ("cliente".equals(invalido)) {
        pedido.put("codigoCliente", Long.MAX_VALUE);
      } else if ("produto".equals(invalido)) {
        ((ObjectNode) pedido.path("itens").get(1)).put("codigoProduto", Long.MAX_VALUE);
      } else {
        pedido.set("desconto", desconto("VALOR_FIXO", "1.04"));
        status = 400;
      }
      cenario.cadastrar(pedido, status);
      cenario.conferirAusenciaDeGravacao();
      assertEquals(0, gatilho.marcador());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"P0001", "23503"})
  void falhaNoSegundoItemDesfazCabecalhoEPrimeiroItem(String estado) throws Exception {
    try (var cenario = new CenarioFatura(); var gatilho = Gatilho.criar(cenario, "item_fatura", "AFTER INSERT", """
      PERFORM nextval('public.%s');
      IF NEW.posicao = 1 THEN
        RAISE EXCEPTION 'falha no segundo item' USING ERRCODE = '%s', CONSTRAINT = 'outra_restricao';
      END IF;
      """.formatted("{sequencia}", estado))) {
      JsonNode problema = cenario.cadastrar(cenario.pedido(), 503);
      assertEquals("Não foi possível concluir a operação no banco de dados.", problema.path("detail").asString());
      assertEquals(2, gatilho.marcador(), "Os dois INSERTs devem ter sido alcançados antes da falha.");
      cenario.conferirAusenciaDeGravacao();
      gatilho.retirar();
      cenario.cadastrar(cenario.pedido(), 201);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"cliente", "produto"})
  void chaveEstrangeiraRealAposValidacaoRetorna404SemGravacaoParcial(String entidade) throws Exception {
    try (var cenario = new CenarioFatura(); var gatilho = Gatilho.criar(cenario, "fatura", "BEFORE INSERT",
      "cliente".equals(entidade)
        ? "DELETE FROM public.cliente WHERE codigo = " + cenario.cliente + ";"
        : "DELETE FROM public.produto WHERE codigo = " + cenario.produtos.get(0) + ";")) {
      JsonNode problema = cenario.cadastrar(cenario.pedido(), 404);
      assertEquals("cliente".equals(entidade) ? "Cliente não encontrado." : "Produto não encontrado.",
        problema.path("detail").asString());
      cenario.conferirAusenciaDeGravacao();
      gatilho.retirar();
      cenario.cadastrar(cenario.pedido(), 201);
    }
  }

  @Test
  void falhaNaMaterializacaoDoRetornoTambemDesfazTodaATransacao() throws Exception {
    try (var cenario = new CenarioFatura(); var gatilho = Gatilho.criar(cenario, "item_fatura", "AFTER INSERT", """
      PERFORM nextval('public.{sequencia}');
      DELETE FROM public.item_fatura WHERE fatura_id = NEW.fatura_id;
      """)) {
      cenario.cadastrar(cenario.pedido(), 503);
      assertEquals(2, gatilho.marcador());
      cenario.conferirAusenciaDeGravacao();
      gatilho.retirar();
      cenario.cadastrar(cenario.pedido(), 201);
    }
  }

  private static ObjectNode desconto(String tipo, String valor) {
    return JSON.createObjectNode().put("tipo", tipo).put("valor", new java.math.BigDecimal(valor));
  }

  private static void conferirItens(Connection conexao, JsonNode resposta) throws SQLException {
    try (var consulta = conexao.prepareStatement("""
      SELECT produto_codigo, quantidade, posicao FROM public.item_fatura WHERE fatura_id = ? ORDER BY posicao
      """)) {
      consulta.setLong(1, resposta.path("codigo").asLong());
      try (var resultado = consulta.executeQuery()) {
        for (int posicao = 0; posicao < resposta.path("itens").size(); posicao++) {
          assertTrue(resultado.next());
          JsonNode item = resposta.path("itens").get(posicao);
          assertEquals(posicao, resultado.getInt("posicao"));
          assertEquals(item.path("codigoProduto").asLong(), resultado.getLong("produto_codigo"));
          assertEquals(0, item.path("quantidade").decimalValue().compareTo(resultado.getBigDecimal("quantidade")));
        }
        assertFalse(resultado.next());
      }
    }
  }

  private record Gatilho(PGSimpleDataSource banco, String funcao, String sequencia) implements AutoCloseable {

    static Gatilho criar(CenarioFatura cenario, String tabela, String evento, String corpo) throws SQLException {
      String sufixo = UUID.randomUUID().toString().replace("-", "");
      var gatilho = new Gatilho(cenario.banco, "falha_fatura_" + sufixo, "marcador_fatura_" + sufixo);
      try (var conexao = cenario.banco.getConnection(); var comando = conexao.createStatement()) {
        try {
          comando.execute("CREATE SEQUENCE public." + gatilho.sequencia);
          comando.execute("""
            CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
            BEGIN
              IF current_setting('application_name') = TG_ARGV[0] THEN
                %s
              END IF;
              RETURN NEW;
            END;
            $teste$
            """.formatted(gatilho.funcao, corpo.replace("{sequencia}", gatilho.sequencia)));
          comando.execute("CREATE TRIGGER gatilho_" + sufixo + " " + evento + " ON public." + tabela
            + " FOR EACH ROW EXECUTE FUNCTION public." + gatilho.funcao + "('"
            + cenario.banco.getApplicationName() + "')");
          return gatilho;
        } catch (SQLException falha) {
          try {
            gatilho.close();
          } catch (SQLException limpeza) {
            falha.addSuppressed(limpeza);
          }
          throw falha;
        }
      }
    }

    long marcador() throws SQLException {
      try (var conexao = this.banco.getConnection(); var consulta = conexao.createStatement();
        var resultado = consulta.executeQuery("SELECT CASE WHEN is_called THEN last_value ELSE 0 END FROM public."
          + this.sequencia)) {
        assertTrue(resultado.next());
        return resultado.getLong(1);
      }
    }

    void retirar() throws SQLException {
      try (var conexao = this.banco.getConnection(); var comando = conexao.createStatement()) {
        comando.execute("DROP FUNCTION IF EXISTS public." + this.funcao + "() CASCADE");
      }
    }

    @Override
    public void close() throws SQLException {
      try {
        this.retirar();
      } finally {
        try (var conexao = this.banco.getConnection(); var comando = conexao.createStatement()) {
          comando.execute("DROP SEQUENCE IF EXISTS public." + this.sequencia);
        }
      }
    }
  }


}
