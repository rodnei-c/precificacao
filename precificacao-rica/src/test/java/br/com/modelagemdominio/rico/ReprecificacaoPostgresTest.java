package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integracao")
class ReprecificacaoPostgresTest {

  private final JsonMapper json = new JsonMapper();

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "ACRESCIMO_SOBRE_CUSTO|{\"custo\":120,\"percentual\":25}|120.00|150.00|25.0000",
    "MARGEM_SOBRE_VENDA|{\"custo\":120,\"percentual\":25}|120.00|160.00|25.0000",
    "ACRESCIMO_SOBRE_CUSTO|{\"precoVenda\":180,\"percentual\":20}|150.00|180.00|20.0000",
    "MARGEM_SOBRE_VENDA|{\"precoVenda\":180,\"percentual\":20}|144.00|180.00|20.0000",
    "ACRESCIMO_SOBRE_CUSTO|{\"custo\":100,\"precoVenda\":140}|100.00|140.00|40.0000",
    "MARGEM_SOBRE_VENDA|{\"custo\":100,\"precoVenda\":140}|100.00|140.00|28.5714"
  })
  void persisteOsTresCalculosNaModalidadeOriginalSemHistoricoRepetido(
    ModalidadePrecificacao modalidade, String entrada, String custo, String venda, String percentual
  ) throws Exception {
    try (var produto = this.produto(modalidade)) {
      var contagem = new SqlContado(produto.banco());
      Take observada = AplicacaoRica.rotas(contagem.banco());
      JsonNode atualizado = this.alterar(observada, produto.codigo(), entrada, 200);
      assertEquals(1, contagem.conexoes(), "Leitura, atualização e histórico devem usar a mesma conexão.");
      assertEquals(3, contagem.comandos(), "A operação deve ler, atualizar o produto e inserir o histórico.");
      var esperado = (ObjectNode) produto.original().deepCopy();
      esperado.set("custo", this.json.readTree(custo));
      esperado.set("precoVenda", this.json.readTree(venda));
      esperado.set("percentual", this.json.readTree(percentual));
      assertEquals(esperado, atualizado);
      assertEquals(esperado, this.consultar(produto));
      conferirValoresGravados(produto, custo, venda, percentual);

      JsonNode historico = this.historico(produto);
      assertEquals(1, historico.size());
      assertEquals(valores(produto.original()), historico.get(0).path("anteriores"));
      assertEquals(valores(esperado), historico.get(0).path("novos"));
      assertEquals(produto.codigo(), historico.get(0).path("codigoProduto").asLong());

      contagem.zerar();
      assertEquals(esperado, this.alterar(observada, produto.codigo(), entrada, 200));
      assertEquals(1, contagem.conexoes());
      assertEquals(1, contagem.comandos(), "Uma repetição não deve executar UPDATE ou INSERT.");
      assertEquals(historico, this.historico(produto));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"custo\":120,\"precoVenda\":150,\"percentual\":20}",
    "{\"custo\":-1,\"percentual\":20}",
    "{\"custo\":100,\"percentual\":0.0040}",
    "{\"custo\":100,\"percentual\":100}"
  })
  void entradaInvalidaPreservaProdutoEHistoricoAnterior(String entrada) throws Exception {
    try (var produto = this.produto(ModalidadePrecificacao.MARGEM_SOBRE_VENDA)) {
      JsonNode anterior = this.alterar(produto.api(), produto.codigo(), "{\"custo\":120,\"percentual\":20}", 200);
      JsonNode historico = this.historico(produto);

      assertEquals("Dados inválidos", this.alterar(produto.api(), produto.codigo(), entrada, 400).path("title").asString());
      assertEquals(anterior, this.consultar(produto));
      assertEquals(historico, this.historico(produto));
    }
  }

  @Test
  void falhaAposInserirHistoricoDesfazPrecoEPreservaRegistrosAnteriores() throws Exception {
    try (var produto = this.produto(ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO);
         var conexao = produto.banco().getConnection(); var comando = conexao.createStatement()) {
      JsonNode anterior = this.alterar(produto.api(), produto.codigo(), "{\"custo\":110,\"percentual\":20}", 200);
      JsonNode historicoAnterior = this.historico(produto);
      String sufixo = UUID.randomUUID().toString().replace("-", "");
      String marcador = "marcador_historico_" + sufixo;
      String funcao = "falha_historico_" + sufixo;
      String gatilho = "gatilho_historico_" + sufixo;
      try {
        comando.execute("CREATE SEQUENCE public." + marcador);
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF NEW.produto_codigo = %d THEN
              IF NOT EXISTS (
                SELECT 1 FROM public.produto WHERE codigo = NEW.produto_codigo
                  AND custo = 200 AND preco_venda = 240 AND percentual = 20
              ) THEN
                RAISE EXCEPTION 'O produto deveria estar atualizado antes do historico';
              END IF;
              PERFORM nextval('public.%s');
              RAISE EXCEPTION 'Falha intencional depois de inserir o historico';
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao, produto.codigo(), marcador));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER INSERT ON public.historico_precificacao"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "()");

        JsonNode problema = this.alterar(produto.api(), produto.codigo(), "{\"custo\":200,\"percentual\":20}", 503);
        assertEquals("Não foi possível concluir a operação no banco de dados.", problema.path("detail").asString());
        try (var resultado = comando.executeQuery("SELECT last_value, is_called FROM public." + marcador)) {
          assertTrue(resultado.next());
          assertTrue(resultado.getBoolean("is_called"), "A falha deve ocorrer após as duas escritas.");
          assertEquals(1, resultado.getLong("last_value"));
        }
        assertEquals(anterior, this.consultar(produto));
        assertEquals(historicoAnterior, this.historico(produto));
        conferirValoresGravados(produto, "110.00", "132.00", "20.0000");
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          comando.execute("DROP SEQUENCE IF EXISTS public." + marcador);
        }
      }
      JsonNode retomada = this.alterar(produto.api(), produto.codigo(), "{\"custo\":200,\"percentual\":20}", 200);
      JsonNode historico = this.historico(produto);
      assertEquals(2, historico.size());
      assertEquals(historicoAnterior.get(0), historico.get(1));
      assertEquals(valores(anterior), historico.get(0).path("anteriores"));
      assertEquals(valores(retomada), historico.get(0).path("novos"));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void alteracoesSimultaneasPreservamCadeiaDoHistoricoSemDuplicarRepeticoes(boolean mesmosValores) throws Exception {
    try (var produto = this.produto(ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO)) {
      String aplicacao = "reprecificacao-" + UUID.randomUUID();
      produto.banco().setApplicationName(aplicacao);
      var executor = Executors.newFixedThreadPool(2);
      var operacoes = new ArrayList<Future<JsonNode>>();
      try {
        try (var bloqueio = produto.banco().getConnection(); var observador = produto.banco().getConnection()) {
          bloqueio.setAutoCommit(false);
          try {
            int sessao;
            try (var consulta = bloqueio.prepareStatement("SELECT codigo FROM public.produto WHERE codigo = ? FOR UPDATE")) {
              consulta.setLong(1, produto.codigo());
              consulta.setQueryTimeout(2);
              try (var resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
              }
            }
            try (var consulta = bloqueio.createStatement(); var resultado = consulta.executeQuery("SELECT pg_backend_pid()")) {
              assertTrue(resultado.next());
              sessao = resultado.getInt(1);
            }
            operacoes.add(executor.submit(() -> this.alterar(produto.api(), produto.codigo(),
              "{\"custo\":110,\"percentual\":20}", 200)));
            operacoes.add(executor.submit(() -> this.alterar(produto.api(), produto.codigo(),
              "{\"custo\":" + (mesmosValores ? "110" : "120") + ",\"percentual\":20}", 200)));
            aguardarBloqueios(observador, sessao, aplicacao);
          } finally {
            bloqueio.rollback();
          }
        }
        JsonNode primeira = operacoes.get(0).get(15, TimeUnit.SECONDS);
        JsonNode segunda = operacoes.get(1).get(15, TimeUnit.SECONDS);
        JsonNode historico = this.historico(produto);
        JsonNode atual = this.consultar(produto);
        assertEquals(mesmosValores ? 1 : 2, historico.size());
        assertEquals(valores(produto.original()), historico.get(historico.size() - 1).path("anteriores"));
        assertEquals(valores(atual), historico.get(0).path("novos"));
        if (mesmosValores) {
          assertEquals(primeira, segunda);
          assertEquals(primeira, atual);
        } else {
          assertEquals(historico.get(1).path("novos"), historico.get(0).path("anteriores"));
          assertTrue(historico.get(0).path("codigo").asLong() > historico.get(1).path("codigo").asLong());
          assertTrue(List.of(valores(primeira), valores(segunda)).contains(valores(atual)));
        }
        assertEquals(produto.original().path("nome"), atual.path("nome"));
        assertEquals(produto.original().path("codigoCategoria"), atual.path("codigoCategoria"));
        assertEquals(produto.original().path("modalidadePrecificacao"), atual.path("modalidadePrecificacao"));
      } finally {
        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
          executor.shutdownNow();
          assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "As operações de teste devem encerrar.");
        }
      }
    }
  }

  private static void aguardarBloqueios(Connection observador, int sessao, String aplicacao) throws Exception {
    long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    try (var consulta = observador.prepareStatement("""
      WITH RECURSIVE bloqueados(pid) AS (
        SELECT pid FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid))
        UNION
        SELECT atividade.pid FROM pg_stat_activity atividade
        JOIN bloqueados ON bloqueados.pid = ANY(pg_blocking_pids(atividade.pid))
      )
      SELECT atividade.query FROM bloqueados JOIN pg_stat_activity atividade USING (pid)
      WHERE atividade.datname = current_database() AND atividade.backend_type = 'client backend'
        AND atividade.application_name = ? ORDER BY atividade.pid
      """)) {
      consulta.setInt(1, sessao);
      consulta.setString(2, aplicacao);
      consulta.setQueryTimeout(2);
      while (System.nanoTime() < limite) {
        var consultas = new ArrayList<String>();
        try (var resultado = consulta.executeQuery()) {
          while (resultado.next()) {
            consultas.add(resultado.getString(1));
          }
        }
        if (consultas.size() == 2) {
          return;
        }
        Thread.sleep(25);
      }
    }
    fail("As duas operações deveriam disputar o bloqueio do mesmo produto.");
  }

  private ProdutoDeTeste produto(ModalidadePrecificacao modalidade) throws Exception {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    Take api = AplicacaoRica.rotas(banco);
    String sufixo = UUID.randomUUID().toString();
    String nome = "Produto reprecificado " + sufixo;
    long categoria = this.corpo(201, enviar(api, "POST", "/api/categorias", "{\"nome\":\"Categoria " + sufixo + "\"}"))
      .path("codigo").asLong();
    try {
      JsonNode original = this.corpo(201, enviar(api, "POST", "/api/produtos", """
        {"nome":"%s","custo":100,"percentual":20,"modalidadePrecificacao":"%s","codigoCategoria":%d}
        """.formatted(nome, modalidade, categoria)));
      assertEquals(nome, original.path("nome").asString());
      assertTrue(original.path("codigo").asLong() > 0);
      return new ProdutoDeTeste(banco, api, categoria, original);
    } catch (Exception | AssertionError falha) {
      limpar(banco, categoria, nome);
      throw falha;
    }
  }

  private record ProdutoDeTeste(PGSimpleDataSource banco, Take api, long categoria, JsonNode original)
    implements AutoCloseable {

    long codigo() {
      return this.original.path("codigo").asLong();
    }

    @Override
    public void close() throws SQLException {
      limpar(this.banco, this.categoria, this.original.path("nome").asString());
    }
  }

  private static void limpar(PGSimpleDataSource banco, long categoria, String nome) throws SQLException {
    try (var conexao = banco.getConnection();
         var historico = conexao.prepareStatement("""
           DELETE FROM public.historico_precificacao
           WHERE produto_codigo IN (SELECT codigo FROM public.produto WHERE nome = ?)
           """);
         var produto = conexao.prepareStatement("DELETE FROM public.produto WHERE nome = ?");
         var vinculo = conexao.prepareStatement("DELETE FROM public.categoria WHERE codigo = ?")) {
      historico.setString(1, nome);
      historico.executeUpdate();
      produto.setString(1, nome);
      produto.executeUpdate();
      vinculo.setLong(1, categoria);
      vinculo.executeUpdate();
    }
  }

  private static void conferirValoresGravados(ProdutoDeTeste produto, String custo, String venda, String percentual)
    throws SQLException {
    try (var conexao = produto.banco().getConnection(); var consulta = conexao.prepareStatement(
      "SELECT custo, preco_venda, percentual FROM public.produto WHERE codigo = ?"
    )) {
      consulta.setLong(1, produto.codigo());
      try (var resultado = consulta.executeQuery()) {
        assertTrue(resultado.next());
        assertEquals(custo, resultado.getBigDecimal("custo").toPlainString());
        assertEquals(venda, resultado.getBigDecimal("preco_venda").toPlainString());
        assertEquals(percentual, resultado.getBigDecimal("percentual").toPlainString());
      }
    }
  }

  private ObjectNode valores(JsonNode produto) {
    var valores = this.json.createObjectNode();
    for (String campo : List.of("custo", "precoVenda", "percentual")) {
      valores.set(campo, produto.path(campo));
    }
    return valores;
  }

  private JsonNode consultar(ProdutoDeTeste produto) throws Exception {
    return this.corpo(200, new RsPrint(produto.api().act(new RqFake("GET", "/api/produtos/" + produto.codigo()))));
  }

  private JsonNode historico(ProdutoDeTeste produto) throws Exception {
    return this.corpo(200, new RsPrint(produto.api().act(new RqFake(
      "GET", "/api/produtos/" + produto.codigo() + "/historico-precificacao"
    ))));
  }

  private JsonNode alterar(Take api, long codigo, String entrada, int status) throws Exception {
    return this.corpo(status, enviar(api, "PATCH", "/api/produtos/" + codigo + "/precificacao", entrada));
  }

  private JsonNode corpo(int status, RsPrint resposta) throws Exception {
    assertEquals(Integer.toString(status), resposta.head().iterator().next().split(" ", 3)[1]);
    return this.json.readTree(resposta.printBody());
  }

  private static RsPrint enviar(Take api, String metodo, String caminho, String corpo) throws Exception {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    return new RsPrint(api.act(new RqFake(List.of(
      metodo + " " + caminho + " HTTP/1.1", "Content-Type: application/json", "Content-Length: " + bytes.length
    ), bytes)));
  }
}
