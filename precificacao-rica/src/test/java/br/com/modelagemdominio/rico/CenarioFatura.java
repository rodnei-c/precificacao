package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.SqlContado;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dados isolados e limpeza dos cenários de fatura. */
final class CenarioFatura implements AutoCloseable {

  private static final JsonMapper JSON = new JsonMapper();

  final PGSimpleDataSource banco = new PGSimpleDataSource();
  final SqlContado sql;
  final Take api;
  final List<Long> produtos = new ArrayList<>();
  private final List<Long> faturas = new ArrayList<>();
  private final List<Long> clientes = new ArrayList<>();
  long cliente;

  CenarioFatura() throws Exception {
    this("1.01", "1.05");
  }

  CenarioFatura(String... precos) throws Exception {
    this.banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    this.banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    this.banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    this.banco.setApplicationName("teste-fatura-" + UUID.randomUUID());
    this.sql = new SqlContado(this.banco);
    this.api = AplicacaoRica.rotas(this.sql.banco());
    try {
      this.cliente = this.novoCliente("52998224725");
      for (String preco : List.of(precos)) {
        var pedido = JSON.createObjectNode().put("nome", "Produto fatura " + UUID.randomUUID())
          .put("custo", 1).put("precoVenda", new java.math.BigDecimal(preco))
          .put("modalidadePrecificacao", "ACRESCIMO_SOBRE_CUSTO");
        this.produtos.add(corpo(201, enviar(this.api, "/api/produtos", pedido)).path("codigo").asLong());
      }
    } catch (Exception | AssertionError falha) {
      try {
        this.close();
      } catch (SQLException limpeza) {
        falha.addSuppressed(limpeza);
      }
      throw falha;
    }
  }

  ObjectNode pedido() {
    var pedido = JSON.createObjectNode().put("codigoCliente", this.cliente);
    var itens = pedido.putArray("itens");
    for (int indice = this.produtos.size() - 1; indice >= 0; indice--) {
      itens.addObject().put("codigoProduto", this.produtos.get(indice))
        .put("quantidade", new java.math.BigDecimal("0.5"));
    }
    return pedido;
  }

  long novoCliente(String cpf) throws Exception {
    var pedido = JSON.createObjectNode().put("nome", "Cliente da fatura").put("cpf", cpf)
      .put("email", "fatura-" + UUID.randomUUID() + "@example.com");
    long codigo = corpo(201, enviar(this.api, "/api/clientes", pedido)).path("codigo").asLong();
    this.clientes.add(codigo);
    return codigo;
  }

  PGSimpleDataSource bancoSomenteLeitura() {
    var leitura = new PGSimpleDataSource();
    leitura.setURL(this.banco.getURL());
    leitura.setUser(this.banco.getUser());
    leitura.setPassword(this.banco.getPassword());
    leitura.setApplicationName(this.banco.getApplicationName() + "-leitura");
    leitura.setOptions("-c default_transaction_read_only=on");
    return leitura;
  }

  JsonNode cadastrar(JsonNode pedido, int status) throws Exception {
    this.sql.zerar();
    var resposta = enviar(this.api, "/api/faturas", pedido);
    JsonNode dados = JSON.readTree(resposta.printBody());
    if (dados.path("codigo").asLong() > 0) {
      this.faturas.add(dados.path("codigo").asLong());
    }
    assertEquals(Integer.toString(status), resposta.head().iterator().next().split(" ", 3)[1], dados.toString());
    assertEquals(1, this.sql.conexoes(), "Toda a operação deve usar a mesma conexão, inclusive o retorno HTTP.");
    return dados;
  }

  void conferirAusenciaDeGravacao() throws SQLException {
    try (var conexao = this.banco.getConnection(); var consulta = conexao.prepareStatement("""
      SELECT (SELECT count(*) FROM public.fatura WHERE cliente_codigo = ?) AS faturas,
        (SELECT count(*) FROM public.item_fatura WHERE produto_codigo = ANY (?)) AS itens
      """)) {
      var codigos = conexao.createArrayOf("bigint", this.produtos.toArray(Long[]::new));
      try {
        consulta.setLong(1, this.cliente);
        consulta.setArray(2, codigos);
        try (var resultado = consulta.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(0, resultado.getInt("faturas"));
          assertEquals(0, resultado.getInt("itens"));
        }
      } finally {
        codigos.free();
      }
    }
  }

  @Override
  public void close() throws SQLException {
    try (var conexao = this.banco.getConnection()) {
      var idsProdutos = conexao.createArrayOf("bigint", this.produtos.toArray(Long[]::new));
      var idsFaturas = conexao.createArrayOf("bigint", this.faturas.toArray(Long[]::new));
      try (var itens = conexao.prepareStatement("""
        DELETE FROM public.item_fatura WHERE produto_codigo = ANY (?) OR fatura_id IN
          (SELECT id FROM public.fatura WHERE cliente_codigo = ? OR id = ANY (?))
        """); var faturas = conexao.prepareStatement("DELETE FROM public.fatura WHERE cliente_codigo = ? OR id = ANY (?)");
        var historico = conexao.prepareStatement("DELETE FROM public.historico_precificacao WHERE produto_codigo = ANY (?)");
        var produtos = conexao.prepareStatement("DELETE FROM public.produto WHERE codigo = ANY (?)");
        var cliente = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ?")) {
        itens.setArray(1, idsProdutos);
        itens.setLong(2, this.cliente);
        itens.setArray(3, idsFaturas);
        itens.executeUpdate();
        faturas.setLong(1, this.cliente);
        faturas.setArray(2, idsFaturas);
        faturas.executeUpdate();
        historico.setArray(1, idsProdutos);
        historico.executeUpdate();
        produtos.setArray(1, idsProdutos);
        produtos.executeUpdate();
        for (long codigo : this.clientes) {
          cliente.setLong(1, codigo);
          cliente.executeUpdate();
        }
      } finally {
        try {
          idsProdutos.free();
        } finally {
          idsFaturas.free();
        }
      }
    }
  }

  private static RsPrint enviar(Take api, String caminho, JsonNode pedido) throws Exception {
    byte[] corpo = JSON.writeValueAsBytes(pedido);
    return new RsPrint(api.act(new RqFake(List.of("POST " + caminho + " HTTP/1.1",
      "Content-Type: application/json", "Content-Length: " + corpo.length), corpo)));
  }

  private static JsonNode corpo(int status, RsPrint resposta) throws Exception {
    JsonNode dados = JSON.readTree(resposta.printBody());
    assertEquals(Integer.toString(status), resposta.head().iterator().next().split(" ", 3)[1], dados.toString());
    return dados;
  }
}
