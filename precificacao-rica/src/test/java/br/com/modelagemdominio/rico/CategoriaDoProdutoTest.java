package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import br.com.modelagemdominio.rico.produtos.pg.PgProduto;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaDoProdutoTest {

  private final JsonMapper json = new JsonMapper();

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "1.5", "9223372036854775808"})
  void rejeitaCodigoInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    RsPrint resposta = cadastrarProduto(api, "Produto inválido", codigo);

    JsonNode problema = this.corpo(400, resposta);
    assertEquals("Informe um código de categoria inteiro positivo válido.", problema.path("detail").asString());
  }

  @Test
  @Tag("integracao")
  void persisteCategoriaOpcionalEConsultaOProduto() throws Exception {
    PGSimpleDataSource banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    UUID sufixo = UUID.randomUUID();
    String categoria = "Categoria " + sufixo;
    String comCategoria = "Produto vinculado " + sufixo;
    String semCategoria = "Produto sem vínculo " + sufixo;
    String categoriaOmitida = "Produto sem campo de categoria " + sufixo;
    try {
      RsPrint cadastro = enviar(api, "/api/categorias", """
        {"nome":"%s"}
        """.formatted(categoria));
      long codigo = this.corpo(201, cadastro).path("codigo").asLong();

      JsonNode vinculado = this.corpo(201, cadastrarProduto(api, comCategoria, codigo + ".0"));
      assertEquals(codigo, vinculado.path("codigoCategoria").asLong());
      assertEquals(vinculado, this.consultarProduto(api, vinculado.path("codigo").asLong()));

      JsonNode semVinculo = this.corpo(201, cadastrarProduto(api, semCategoria, "null"));
      assertFalse(semVinculo.has("codigoCategoria"));
      assertEquals(semVinculo, this.consultarProduto(api, semVinculo.path("codigo").asLong()));

      var entrada = this.json.createObjectNode().put("nome", categoriaOmitida)
        .put("custo", 100).put("percentual", 20).put("modalidadePrecificacao", "ACRESCIMO_SOBRE_CUSTO");
      JsonNode omitido = this.corpo(201, enviar(api, "/api/produtos", entrada.toString()));
      assertFalse(omitido.has("codigoCategoria"));
      assertEquals(omitido, this.consultarProduto(api, omitido.path("codigo").asLong()));

      List<JsonNode> produtosDoTeste = new ArrayList<>();
      for (JsonNode item : this.corpo(200, consultarLista(api))) {
        String nome = item.path("nome").asString();
        if (nome.equals(comCategoria) || nome.equals(semCategoria) || nome.equals(categoriaOmitida)) {
          produtosDoTeste.add(item);
        }
      }
      assertEquals(List.of(vinculado, semVinculo, omitido), produtosDoTeste);
    } finally {
      try (Connection conexao = banco.getConnection()) {
        excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", comCategoria);
        excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", semCategoria);
        excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", categoriaOmitida);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", categoria);
      }
    }
  }

  @Test
  @Tag("integracao")
  void categoriaInexistenteRetorna404SemGravarProduto() throws Exception {
    PGSimpleDataSource banco = bancoDeTeste();
    String nome = "Produto recusado " + UUID.randomUUID();
    try (Connection conexao = banco.getConnection()) {
      try {
        RsPrint resposta = cadastrarProduto(AplicacaoRica.rotas(banco), nome, Long.toString(Long.MAX_VALUE));

        assertEquals("Categoria não encontrada.", this.corpo(404, resposta).path("detail").asString());
        try (var consulta = conexao.prepareStatement("SELECT 1 FROM public.produto WHERE nome = ?")) {
          consulta.setString(1, nome);
          try (var resultado = consulta.executeQuery()) {
            assertFalse(resultado.next());
          }
        }
      } finally {
        excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", nome);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"codigoCategoria\":null}", "{\"codigoCategoria\":1.5}"})
  void patchExigeCodigoDeCategoriaInteiroESemAcessarOBanco(String alteracao) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    JsonNode problema = this.corpo(400, alterarCategoria(api, "1", alteracao));

    assertEquals("Informe um código de categoria inteiro positivo válido.", problema.path("detail").asString());
  }

  @Test
  void patchRecusaCodigoDeProdutoInvalidoSemAcessarOBanco() throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    JsonNode problema = this.corpo(400, alterarCategoria(api, "0", "{\"codigoCategoria\":1}"));

    assertEquals("O código do produto deve ser maior que zero.", problema.path("detail").asString());
  }

  @Test
  @Tag("integracao")
  void trocaSomenteACategoriaEConservaOVinculoSeAProximaTrocaFalhar() throws Exception {
    PGSimpleDataSource banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    UUID sufixo = UUID.randomUUID();
    String categoriaAnterior = "Categoria anterior " + sufixo;
    String categoriaNova = "Categoria nova " + sufixo;
    String nomeProduto = "Produto para troca " + sufixo;
    try {
      long anterior = this.cadastrarCategoria(api, categoriaAnterior);
      long nova = this.cadastrarCategoria(api, categoriaNova);
      JsonNode produto = this.corpo(201, cadastrarProduto(api, nomeProduto, Long.toString(anterior)));
      long codigoProduto = produto.path("codigo").asLong();
      var produtos = new PgProdutosConstantes(banco);
      var leituraAnterior = produtos.produto(codigoProduto);
      var leituraAtual = new PgProduto(banco, codigoProduto);

      JsonNode alterado = this.corpo(200, alterarCategoria(api, Long.toString(codigoProduto),
        "{\"codigoCategoria\":" + nova + "}"));

      var esperado = ((ObjectNode) produto).deepCopy();
      esperado.set("codigoCategoria", this.json.readTree(Long.toString(nova)));
      assertEquals(esperado, alterado);
      assertEquals(anterior, leituraAnterior.codigoCategoria().orElseThrow().valor());
      assertEquals(nova, leituraAtual.codigoCategoria().orElseThrow().valor());
      assertEquals(nova, produtos.produto(codigoProduto).codigoCategoria().orElseThrow().valor());
      assertEquals(esperado, this.consultarProduto(api, codigoProduto));

      JsonNode categoriaAusente = this.corpo(404, alterarCategoria(api, Long.toString(codigoProduto),
        "{\"codigoCategoria\":" + Long.MAX_VALUE + "}"));
      assertEquals("Categoria não encontrada.", categoriaAusente.path("detail").asString());
      assertEquals(esperado, this.consultarProduto(api, codigoProduto));

      JsonNode produtoAusente = this.corpo(404, alterarCategoria(api, Long.toString(Long.MAX_VALUE),
        "{\"codigoCategoria\":" + nova + "}"));
      assertEquals("Produto não encontrado.", produtoAusente.path("detail").asString());
    } finally {
      try (Connection conexao = banco.getConnection()) {
        excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", nomeProduto);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", categoriaAnterior);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", categoriaNova);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PATCH"})
  void indisponibilidadeAoValidarCategoriaRetorna503(String metodo) throws Exception {
    var banco = new HikariDataSource();
    banco.close();
    Take api = AplicacaoRica.rotas(banco);
    RsPrint resposta = "POST".equals(metodo)
      ? cadastrarProduto(api, "Produto indisponível", "1")
      : alterarCategoria(api, "1", "{\"codigoCategoria\":1}");

    JsonNode problema = this.corpo(503, resposta);
    assertEquals("Não foi possível concluir a operação no banco de dados.", problema.path("detail").asString());
  }

  @ParameterizedTest
  @Tag("integracao")
  @ValueSource(strings = {"POST", "PATCH"})
  void falhaDeOutraChaveEstrangeiraNaoInformaCategoriaAusente(String metodo) throws Exception {
    var banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String categoria = "Categoria preservada " + sufixo;
    String produtoAnterior = "Produto preservado " + sufixo;
    String produtoNovo = "Produto rejeitado " + sufixo;
    String funcao = "falha_vinculo_" + sufixo;
    String gatilho = "gatilho_vinculo_" + sufixo;
    String nomeEmFalha = "POST".equals(metodo) ? produtoNovo : produtoAnterior;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        long codigoCategoria = this.cadastrarCategoria(api, categoria);
        JsonNode original = this.corpo(201, cadastrarProduto(api, produtoAnterior, "null"));
        long codigoProduto = original.path("codigo").asLong();
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
           IF NEW.nome = TG_ARGV[0] THEN
            RAISE EXCEPTION USING ERRCODE = '23503',
             MESSAGE = 'Falha intencional de outra referencia', CONSTRAINT = 'outra_referencia';
           END IF;
           RETURN NEW;
          END;
          $teste$
          """.formatted(funcao));
        comando.execute("CREATE TRIGGER " + gatilho + " BEFORE INSERT OR UPDATE ON public.produto"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + nomeEmFalha + "')");

        RsPrint resposta = "POST".equals(metodo)
          ? cadastrarProduto(api, produtoNovo, Long.toString(codigoCategoria))
          : alterarCategoria(api, Long.toString(codigoProduto), "{\"codigoCategoria\":" + codigoCategoria + "}");
        JsonNode problema = this.corpo(503, resposta);
        assertEquals("Não foi possível concluir a operação no banco de dados.", problema.path("detail").asString());
        assertEquals(original, this.consultarProduto(api, codigoProduto));
        try (var consulta = conexao.prepareStatement("SELECT count(*) FROM public.produto WHERE nome = ?")) {
          consulta.setString(1, produtoNovo);
          try (var resultado = consulta.executeQuery()) {
            assertTrue(resultado.next());
            assertEquals(0, resultado.getInt(1));
          }
        }
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", produtoNovo);
          excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", produtoAnterior);
          excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", categoria);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "categoria=0", "categoria=1.5", "categoria=9223372036854775808",
    "modalidadePrecificacao=DESCONHECIDA", "modalidadePrecificacao=++", "categoria=++"
  })
  void recusaFiltrosInvalidosAntesDeAcessarOBanco(String filtros) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    JsonNode problema = this.corpo(400, consultarLista(api, "?" + filtros));

    assertEquals("/api/produtos", problema.path("instance").asString());
  }

  @Test
  @Tag("integracao")
  void combinaFiltrosSemTratarCaracteresDoNomeComoCuringas() throws Exception {
    PGSimpleDataSource banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    UUID sufixo = UUID.randomUUID();
    String categoria = "Categoria filtros " + sufixo;
    String prefixo = "Cafe " + sufixo + " ";
    String literal = prefixo + "%_ margem";
    String outroNome = prefixo + "XY margem";
    String outraModalidade = prefixo + "%_ acrescimo";
    String semVinculo = prefixo + "%_ sem categoria";
    String margem = "MARGEM_SOBRE_VENDA";
    try {
      String codigoCategoria = Long.toString(this.cadastrarCategoria(api, categoria));
      long codigoLiteral = this.corpo(201, cadastrarProduto(api, literal, codigoCategoria, margem))
        .path("codigo").asLong();
      long codigoOutroNome = this.corpo(201, cadastrarProduto(api, outroNome, codigoCategoria, margem))
        .path("codigo").asLong();
      long codigoOutraModalidade = this.corpo(201, cadastrarProduto(api, outraModalidade, codigoCategoria))
        .path("codigo").asLong();
      long codigoSemVinculo = this.corpo(201, cadastrarProduto(api, semVinculo, "null", margem))
        .path("codigo").asLong();

      String nome = "nome=" + URLEncoder.encode("  cAFE " + sufixo + " %_  ", StandardCharsets.UTF_8);
      String filtroCategoria = "categoria=" + codigoCategoria;
      String modalidade = "modalidadePrecificacao=" + margem;

      assertEquals(List.of(codigoLiteral, codigoOutraModalidade, codigoSemVinculo),
        this.codigosDaLista(api, nome));
      assertEquals(List.of(codigoLiteral, codigoOutroNome, codigoOutraModalidade),
        this.codigosDaLista(api, filtroCategoria));
      List<Long> codigosDoTeste = List.of(codigoLiteral, codigoOutroNome, codigoOutraModalidade, codigoSemVinculo);
      assertEquals(codigosDoTeste, new PgProdutosConstantes(banco).listar(
        new FiltrosProdutos(Optional.of("  " + sufixo + "  "), Optional.empty(), Optional.empty())
      ).stream().map(produto -> produto.codigo()).toList());
      assertEquals(this.corpo(200, consultarLista(api)).size(), new PgProdutosConstantes(banco).listar(
        new FiltrosProdutos(Optional.of("  "), Optional.empty(), Optional.empty())
      ).size());
      assertEquals(List.of(codigoLiteral, codigoOutroNome, codigoSemVinculo),
        this.codigosDaLista(api, modalidade).stream().filter(codigosDoTeste::contains).toList());
      assertEquals(List.of(codigoLiteral), this.codigosDaLista(api, nome + "&" + modalidade + "&" + filtroCategoria));
      assertEquals(List.of(), this.codigosDaLista(api, nome + "&categoria=" + Long.MAX_VALUE));
      assertEquals(this.corpo(200, consultarLista(api)),
        this.corpo(200, consultarLista(api, "?nome=++&modalidadePrecificacao=&categoria=")));
    } finally {
      try (Connection conexao = banco.getConnection()) {
        for (String nome : List.of(literal, outroNome, outraModalidade, semVinculo)) {
          excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", nome);
        }
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", categoria);
      }
    }
  }

  private List<Long> codigosDaLista(Take api, String filtros) throws Exception {
    List<Long> codigos = new ArrayList<>();
    for (JsonNode produto : this.corpo(200, consultarLista(api, "?" + filtros))) {
      codigos.add(produto.path("codigo").asLong());
    }
    return codigos;
  }

  @Test
  void listaDeProdutosRetorna503QuandoOBancoEstaIndisponivel() throws Exception {
    HikariDataSource banco = new HikariDataSource();
    banco.close();

    JsonNode problema = this.corpo(503, consultarLista(AplicacaoRica.rotas(banco)));

    assertEquals("Persistência indisponível", problema.path("title").asString());
    assertEquals("/api/produtos", problema.path("instance").asString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "1.5", "9223372036854775808"})
  void renomeacaoRecusaCodigoDeCategoriaInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    this.corpo(400, renomearCategoria(api, codigo, """
      {"nome":"Alimentos"}
      """));
  }

  @Test
  void renomeacaoReutilizaValidacaoDeNomeERejeitaCamposExtras() throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    JsonNode nomeEmBranco = this.corpo(400, renomearCategoria(api, "1", """
      {"nome":"   "}
      """));
    assertEquals("Informe o nome da categoria.", nomeEmBranco.path("detail").asString());

    JsonNode campoExtra = this.corpo(400, renomearCategoria(api, "1", """
      {"nome":"Alimentos","codigo":2}
      """));
    assertEquals("Formato inválido", campoExtra.path("title").asString());
  }

  @Test
  void renomeacaoInformaBancoIndisponivel() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    JsonNode problema = this.corpo(503, renomearCategoria(AplicacaoRica.rotas(banco), "1", """
      {"nome":"Alimentos"}
      """));

    assertEquals("Persistência indisponível", problema.path("title").asString());
  }

  @Test
  @Tag("integracao")
  void renomeiaCategoriaVinculadaPreservandoProdutoEHistorico() throws Exception {
    PGSimpleDataSource banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    UUID sufixo = UUID.randomUUID();
    String nomeOriginal = "Categoria original " + sufixo;
    String novoNome = "Cafés d'avó " + sufixo;
    String nomeEmMaiusculas = novoNome.toUpperCase(Locale.ROOT);
    String nomeOcupado = "Bebidas " + sufixo;
    String nomeProduto = "Produto para renomeação " + sufixo;
    try {
      long categoria = this.cadastrarCategoria(api, nomeOriginal);
      long outraCategoria = this.cadastrarCategoria(api, nomeOcupado);
      JsonNode cadastro = this.corpo(201, cadastrarProduto(api, nomeProduto, Long.toString(categoria)));
      long codigoProduto = cadastro.path("codigo").asLong();
      String caminhoProduto = "/api/produtos/" + codigoProduto;
      JsonNode produto = this.corpo(200, enviar(api, "PATCH", caminhoProduto + "/precificacao", """
        {"custo":120,"percentual":25}
        """));
      String caminhoHistorico = caminhoProduto + "/historico-precificacao";
      JsonNode historico = this.corpo(200, new RsPrint(api.act(new RqFake("GET", caminhoHistorico))));
      assertEquals(1, historico.size());

      String codigo = Long.toString(categoria);
      JsonNode renomeada = this.corpo(200, renomearCategoria(api, codigo,
        "{\"nome\":\"  " + novoNome + "  \"}"));
      assertEquals(this.json.readTree("""
        {"codigo":%d,"nome":"%s"}
        """.formatted(categoria, novoNome)), renomeada);
      JsonNode maiusculas = this.corpo(200, renomearCategoria(api, codigo,
        "{\"nome\":\"" + nomeEmMaiusculas + "\"}"));
      assertEquals(this.json.readTree("""
        {"codigo":%d,"nome":"%s"}
        """.formatted(categoria, nomeEmMaiusculas)), maiusculas);
      assertEquals(renomeada, this.corpo(200, renomearCategoria(api, codigo,
        "{\"nome\":\"" + novoNome + "\"}")));
      assertEquals(renomeada, this.corpo(200, renomearCategoria(api, codigo,
        "{\"nome\":\"" + novoNome + "\"}")));

      JsonNode conflito = this.corpo(400, renomearCategoria(api, codigo,
        "{\"nome\":\"bebidas " + sufixo + "\"}"));
      assertEquals("Já existe uma categoria com esse nome.", conflito.path("detail").asString());
      JsonNode ausente = this.corpo(404, renomearCategoria(api, Long.toString(Long.MAX_VALUE),
        "{\"nome\":\"Categoria inexistente\"}"));
      assertEquals("Categoria não encontrada.", ausente.path("detail").asString());

      JsonNode categorias = this.corpo(200, new RsPrint(api.act(new RqFake("GET", "/api/categorias"))));
      assertEquals(renomeada, categoriaNaLista(categorias, categoria));
      assertEquals(nomeOcupado, categoriaNaLista(categorias, outraCategoria).path("nome").asString());
      assertEquals(produto, this.consultarProduto(api, codigoProduto));
      assertEquals(historico, this.corpo(200, new RsPrint(api.act(new RqFake("GET", caminhoHistorico)))));
    } finally {
      try (Connection conexao = banco.getConnection()) {
        excluirPorNome(conexao, """
          DELETE FROM public.historico_precificacao
          WHERE produto_codigo IN (SELECT codigo FROM public.produto WHERE nome = ?)
          """, nomeProduto);
        excluirPorNome(conexao, "DELETE FROM public.produto WHERE nome = ?", nomeProduto);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", nomeOriginal);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", novoNome);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", nomeEmMaiusculas);
        excluirPorNome(conexao, "DELETE FROM public.categoria WHERE nome = ?", nomeOcupado);
      }
    }
  }

  private static RsPrint renomearCategoria(Take api, String codigo, String corpo) throws Exception {
    return enviar(api, "PATCH", "/api/categorias/" + codigo, corpo);
  }

  private static JsonNode categoriaNaLista(JsonNode categorias, long codigo) {
    for (JsonNode categoria : categorias) {
      if (categoria.path("codigo").asLong() == codigo) {
        return categoria;
      }
    }
    throw new AssertionError("Categoria ausente na listagem: " + codigo);
  }

  private static RsPrint consultarLista(Take api) throws Exception {
    return consultarLista(api, "");
  }

  private static RsPrint consultarLista(Take api, String consulta) throws Exception {
    return new RsPrint(api.act(new RqFake("GET", "/api/produtos" + consulta)));
  }

  private long cadastrarCategoria(Take api, String nome) throws Exception {
    RsPrint cadastro = enviar(api, "/api/categorias", """
      {"nome":"%s"}
      """.formatted(nome));
    return this.corpo(201, cadastro).path("codigo").asLong();
  }

  private static RsPrint alterarCategoria(Take api, String codigo, String alteracao) throws Exception {
    return enviar(api, "PATCH", "/api/produtos/" + codigo + "/categoria", alteracao);
  }

  private JsonNode consultarProduto(Take api, long codigo) throws Exception {
    return this.corpo(200, new RsPrint(api.act(new RqFake("GET", "/api/produtos/" + codigo))));
  }

  private JsonNode corpo(int status, RsPrint resposta) throws Exception {
    String primeiraLinha = resposta.head().iterator().next();
    assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], primeiraLinha);
    return this.json.readTree(resposta.printBody());
  }

  private static RsPrint cadastrarProduto(Take api, String nome, String codigoCategoria) throws Exception {
    return cadastrarProduto(api, nome, codigoCategoria, "ACRESCIMO_SOBRE_CUSTO");
  }

  private static RsPrint cadastrarProduto(Take api, String nome, String codigoCategoria, String modalidade)
    throws Exception {
    return enviar(api, "/api/produtos", """
      {
        "nome":"%s",
        "custo":100,
        "percentual":20,
        "modalidadePrecificacao":"%s",
        "codigoCategoria":%s
      }
      """.formatted(nome, modalidade, codigoCategoria));
  }

  private static RsPrint enviar(Take api, String caminho, String corpo) throws Exception {
    return enviar(api, "POST", caminho, corpo);
  }

  private static RsPrint enviar(Take api, String metodo, String caminho, String corpo) throws Exception {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    return new RsPrint(api.act(new RqFake(List.of(
      metodo + " " + caminho + " HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + bytes.length
    ), bytes)));
  }

  private static PGSimpleDataSource bancoDeTeste() {
    PGSimpleDataSource banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
    banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
    return banco;
  }

  private static void excluirPorNome(Connection conexao, String sql, String nome) throws SQLException {
    try (var exclusao = conexao.prepareStatement(sql)) {
      exclusao.setString(1, nome);
      exclusao.executeUpdate();
    }
  }
}
