package br.com.precificacao.anemico;

import br.com.precificacao.anemico.testes.ConcorrenciaPostgres;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercita os cadastros por HTTP real, exclusivamente no PostgreSQL de teste informado. */
@Tag("integracao")
@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CadastrosPostgresTest {

  private static final AtomicLong CPF_BASE = new AtomicLong(
    java.util.concurrent.ThreadLocalRandom.current().nextLong(100_000_000L, 999_000_000L)
  );
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private ConfigurableApplicationContext contexto;
  private JsonMapper json;
  private JdbcTemplate jdbc;
  private String baseUrl;

  @BeforeAll
  void iniciar() {
    SpringApplication aplicacao = new SpringApplication(AplicacaoAnemica.class);
    aplicacao.addInitializers(atual -> atual.getEnvironment().getPropertySources()
      .addFirst(new MapPropertySource("conexao-exclusiva-dos-cadastros", Map.of(
        "spring.datasource.url", System.getenv("BANCO_TESTE_URL"),
        "spring.datasource.username", System.getenv("BANCO_TESTE_USUARIO"),
        "spring.datasource.password", System.getenv("BANCO_TESTE_SENHA"),
        "spring.datasource.hikari.maximum-pool-size", 4,
        "precificacao.banco.criar-automaticamente", false,
        "server.port", "0",
        "server.address", "127.0.0.1"
      ))));
    contexto = aplicacao.run();
    json = contexto.getBean(JsonMapper.class);
    jdbc = contexto.getBean(JdbcTemplate.class);
    baseUrl = "http://127.0.0.1:" + contexto.getEnvironment().getProperty("local.server.port");
    assertEquals(8, jdbc.queryForObject(
      "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class
    ));
  }

  @AfterAll
  void encerrar() {
    if (contexto != null) {
      contexto.close();
    }
  }

  @Test
  void consultaClienteComESemEnderecoEMantemListagem() throws Exception {
    JsonNode cidade = cidade(unico("Curitiba"), "pr");
    JsonNode comEndereco = cliente(endereco(cidade.get("codigo").longValue()));
    JsonNode semEndereco = cliente(null);

    assertEquals(comEndereco, getCliente(comEndereco));
    assertEquals(semEndereco, getCliente(semEndereco));
    assertTrue(semEndereco.get("endereco").isNull());
    assertEquals("PR", cidade.get("uf").stringValue());
    JsonNode lista = resposta("GET", "/api/clientes", null, 200);
    assertTrue(contemCodigo(lista, comEndereco.get("codigo").longValue()));
    assertTrue(contemCodigo(lista, semEndereco.get("codigo").longValue()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808"})
  void rejeitaCodigoInvalidoDeCliente(String codigo) throws Exception {
    resposta("GET", "/api/clientes/" + codigo, null, 400);
    resposta("PATCH", "/api/clientes/" + codigo, "{\"email\":\"novo@example.com\"}", 400);
  }

  @Test
  void clienteAusenteRetorna404() throws Exception {
    resposta("GET", "/api/clientes/9223372036854775807", null, 404);
    resposta("PATCH", "/api/clientes/9223372036854775807", "{\"email\":\"novo@example.com\"}", 404);
  }

  @Test
  void atualizaEmailSemAlterarOsDemaisDados() throws Exception {
    JsonNode atual = cliente(endereco(cidade(unico("Florianópolis"), "SC").get("codigo").longValue()));
    String email = unico("novo") + "@example.com";
    JsonNode alterado = patchCliente(atual, Map.of("email", " " + email + " "), 200);

    assertEquals(email, alterado.get("email").stringValue());
    for (String campo : new String[]{"codigo", "nome", "cpf", "telefone", "endereco"}) {
      assertEquals(atual.get(campo), alterado.get(campo), campo);
    }
    assertEquals(alterado, getCliente(atual));
    assertEquals(atual.get("cpf").stringValue(), getCliente(atual).get("cpf").stringValue());
  }

  @Test
  void atualizacaoSomenteDoEmailNaoIncluiOutrasColunasNoComando() throws Exception {
    JsonNode atual = cliente(endereco(cidade(unico("Cidade"), "SC").get("codigo").longValue()));
    long codigo = atual.get("codigo").longValue();
    String identificador = UUID.randomUUID().toString().replace("-", "");
    String funcao = "recusar_colunas_omitidas_" + identificador;
    String gatilho = "conferir_colunas_" + identificador;
    jdbc.execute("CREATE FUNCTION " + funcao + "() RETURNS TRIGGER LANGUAGE plpgsql AS $$"
      + " BEGIN RAISE EXCEPTION 'A atualização incluiu uma coluna omitida.'; END; $$");
    try {
      jdbc.execute("CREATE TRIGGER " + gatilho
        + " BEFORE UPDATE OF nome, cpf, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo"
        + " ON cliente FOR EACH ROW WHEN (OLD.codigo = " + codigo + ") EXECUTE FUNCTION " + funcao + "()");
      String email = unico("somenteemail") + "@example.com";
      JsonNode alterado = patchCliente(atual, Map.of("email", email), 200);
      assertEquals(email, alterado.get("email").stringValue());
      assertEquals(alterado, getCliente(atual));
      assertEquals(atual.get("telefone"), alterado.get("telefone"));
      assertEquals(atual.get("endereco"), alterado.get("endereco"));
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS " + gatilho + " ON cliente");
      jdbc.execute("DROP FUNCTION " + funcao + "()");
    }
  }

  @Test
  void emailDuplicadoIgnoraCapitalizacaoEPreservaTodosOsCampos() throws Exception {
    JsonNode atual = cliente(null);
    JsonNode outro = cliente(null);
    patchCliente(atual, Map.of(
      "email", outro.get("email").stringValue().toUpperCase(java.util.Locale.ROOT),
      "telefone", "(11) 98888-0000"
    ), 400);
    assertEquals(atual, getCliente(atual));
    assertEquals(outro, getCliente(outro));
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "\"\"", "\"invalido\"", "\"a@@example.com\"", "\"a@b\""})
  void rejeitaEmailInvalidoSemGravar(String email) throws Exception {
    JsonNode atual = cliente(null);
    resposta("PATCH", caminhoCliente(atual), "{\"email\":" + email + "}", 400);
    assertEquals(atual, getCliente(atual));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{}", "null", "{\"campoDesconhecido\":1}", "{\"nome\":\"Outro\"}",
    "{\"cpf\":\"12345678909\"}", "{\"emailInformado\":true}",
    "{\"telefoneInformado\":true}", "{\"enderecoInformado\":true}",
    "{\"endereco\":{\"campoDesconhecido\":1}}", "[]"
  })
  void rejeitaCorpoVazioOuCamposNaoPermitidos(String corpo) throws Exception {
    JsonNode atual = cliente(null);
    resposta("PATCH", caminhoCliente(atual), corpo, 400);
    assertEquals(atual, getCliente(atual));
  }

  @Test
  void atualizaERemoveTelefoneEDistingueCampoOmitido() throws Exception {
    JsonNode atual = cliente(null);
    JsonNode telefoneAlterado = patchCliente(atual, Map.of("telefone", "+55 (41) 98888-1234"), 200);
    assertEquals("+55 (41) 98888-1234", telefoneAlterado.get("telefone").stringValue());
    assertEquals(atual.get("email"), telefoneAlterado.get("email"));

    JsonNode removido = resposta("PATCH", caminhoCliente(atual), "{\"telefone\":null}", 200);
    assertTrue(removido.get("telefone").isNull());
    JsonNode emailAlterado = patchCliente(atual, Map.of("email", unico("email") + "@example.com"), 200);
    assertTrue(emailAlterado.get("telefone").isNull());
    assertEquals(emailAlterado, getCliente(atual));

    JsonNode juntos = patchCliente(atual, Map.of(
      "email", unico("outro") + "@example.com", "telefone", "(48) 3222-0000"
    ), 200);
    assertEquals("(48) 3222-0000", juntos.get("telefone").stringValue());
    assertEquals(juntos, getCliente(atual));
  }

  @ParameterizedTest
  @ValueSource(strings = {"99999", "abc", "(41) 9999-00", "+1 555 1234"})
  void telefoneInvalidoImpedeTambemAAlteracaoDoEmail(String telefone) throws Exception {
    JsonNode atual = cliente(null);
    patchCliente(atual, Map.of("email", unico("email") + "@example.com", "telefone", telefone), 400);
    assertEquals(atual, getCliente(atual));
  }

  @Test
  void trocaERemoveEnderecoSemPerderOVinculoDaFatura() throws Exception {
    long primeiraCidade = cidade(unico("Curitiba"), "PR").get("codigo").longValue();
    long segundaCidade = cidade(unico("Tubarão"), "SC").get("codigo").longValue();
    JsonNode atual = cliente(endereco(primeiraCidade));
    JsonNode produto = produto(null);
    JsonNode fatura = resposta("POST", "/api/faturas", json.writeValueAsString(Map.of(
      "itens", new Object[]{Map.of("codigoProduto", produto.get("codigo").longValue(), "quantidade", 1)},
      "codigoCliente", atual.get("codigo").longValue()
    )), 201);

    JsonNode alterado = patchCliente(atual, Map.of(
      "email", unico("endereco") + "@example.com", "telefone", "(48) 99999-0000",
      "endereco", endereco(segundaCidade)
    ), 200);
    assertEquals(segundaCidade, alterado.get("endereco").get("codigoCidade").longValue());
    assertEquals("80000000", alterado.get("endereco").get("cep").stringValue());
    assertEquals("12A", alterado.get("endereco").get("numero").stringValue());
    assertEquals(alterado, getCliente(atual));

    JsonNode removido = resposta("PATCH", caminhoCliente(atual), "{\"endereco\":null}", 200);
    assertTrue(removido.get("endereco").isNull());
    assertEquals(alterado.get("email"), removido.get("email"));
    assertEquals(alterado.get("telefone"), removido.get("telefone"));
    assertEquals(0, jdbc.queryForObject("""
      SELECT num_nonnulls(cep, logradouro, numero, bairro, complemento, cidade_codigo)
      FROM cliente WHERE codigo = ?
      """, Integer.class, atual.get("codigo").longValue()));

    JsonNode lista = resposta("GET", "/api/faturas?codigoCliente=" + atual.get("codigo").longValue(), null, 200);
    assertTrue(contemCodigo(lista, fatura.get("codigo").longValue()));
    JsonNode consultada = resposta("GET", "/api/faturas/" + fatura.get("codigo").longValue(), null, 200);
    assertEquals(atual.get("codigo"), consultada.get("codigoCliente"));
    assertEquals(removido, getCliente(atual));
  }

  @ParameterizedTest
  @ValueSource(strings = {"cep", "logradouro", "numero", "bairro", "codigoCidade"})
  void enderecoIncompletoImpedeTodaAAtualizacao(String campoAusente) throws Exception {
    JsonNode atual = cliente(null);
    var novoEndereco = new java.util.HashMap<>(endereco(cidade(unico("Cidade"), "PR").get("codigo").longValue()));
    novoEndereco.remove(campoAusente);
    patchCliente(atual, Map.of("email", unico("email") + "@example.com", "endereco", novoEndereco), 400);
    assertEquals(atual, getCliente(atual));
  }

  @Test
  void cidadeInexistenteOuInvalidaImpedeTodaAAtualizacao() throws Exception {
    JsonNode atual = cliente(null);
    patchCliente(atual, Map.of(
      "email", unico("email") + "@example.com", "endereco", endereco(Long.MAX_VALUE)
    ), 404);
    assertEquals(atual, getCliente(atual));
    for (Object codigo : new Object[]{0, -1, new BigDecimal("1.5")}) {
      var novoEndereco = new java.util.HashMap<>(endereco(1));
      novoEndereco.put("codigoCidade", codigo);
      patchCliente(atual, Map.of("endereco", novoEndereco), 400);
      assertEquals(atual, getCliente(atual));
    }
  }

  @Test
  void atualizacoesConcorrentesPreservamOsCamposOmitidos() throws Exception {
    JsonNode atual = cliente(null);
    String email = unico("concorrente") + "@example.com";
    CompletableFuture<HttpResponse<String>> emailAlterado = null;
    CompletableFuture<HttpResponse<String>> telefoneAlterado = null;
    try (var bloqueio = jdbc.getDataSource().getConnection();
         var observador = jdbc.getDataSource().getConnection()) {
      bloqueio.setAutoCommit(false);
      try {
        try (var consulta = bloqueio.prepareStatement("SELECT codigo FROM cliente WHERE codigo = ? FOR UPDATE")) {
          consulta.setLong(1, atual.get("codigo").longValue());
          consulta.setQueryTimeout(5);
          try (var resultado = consulta.executeQuery()) {
            assertTrue(resultado.next());
          }
        }
        int codigoSessao = ConcorrenciaPostgres.codigoDaSessao(bloqueio);
        emailAlterado = http.sendAsync(requisicao("PATCH", caminhoCliente(atual),
          json.writeValueAsString(Map.of("email", email))), HttpResponse.BodyHandlers.ofString());
        telefoneAlterado = http.sendAsync(requisicao("PATCH", caminhoCliente(atual),
          "{\"telefone\":\"(41) 98888-2222\"}"), HttpResponse.BodyHandlers.ofString());

        ConcorrenciaPostgres.aguardarConsultasBloqueadas(observador, codigoSessao, 2);
        assertFalse(emailAlterado.isDone(), "A alteração deve aguardar a liberação do cliente.");
        assertFalse(telefoneAlterado.isDone(), "A alteração deve aguardar a liberação do cliente.");
      } finally {
        try {
          bloqueio.rollback();
        } finally {
          ConcorrenciaPostgres.aguardarConclusao(emailAlterado, telefoneAlterado);
        }
      }
    }
    assertEquals(200, emailAlterado.join().statusCode(), emailAlterado.join().body());
    assertEquals(200, telefoneAlterado.join().statusCode(), telefoneAlterado.join().body());
    JsonNode consultado = getCliente(atual);
    assertEquals(email, consultado.get("email").stringValue());
    assertEquals("(41) 98888-2222", consultado.get("telefone").stringValue());
    for (String campo : List.of("codigo", "nome", "cpf", "endereco")) {
      assertEquals(atual.get(campo), consultado.get(campo), campo);
    }
  }

  @Test
  void filtraCidadesPorTrechoSemDistinguirCapitalizacao() throws Exception {
    String prefixo = unico("Localidade");
    JsonNode primeira = cidade(prefixo + " Curitiba", "PR");
    JsonNode segunda = cidade(prefixo + " Curitiba", "SC");
    JsonNode outra = cidade(prefixo + " Outra", "PR");
    for (String nome : new String[]{prefixo + " curi", " " + prefixo.toUpperCase(java.util.Locale.ROOT) + " CURI "}) {
      JsonNode lista = resposta("GET", "/api/cidades?nome=" + parametro(nome), null, 200);
      assertEquals(2, lista.size());
      assertEquals(primeira.get("codigo"), lista.get(0).get("codigo"));
      assertEquals(segunda.get("codigo"), lista.get(1).get("codigo"));
      assertFalse(contemCodigo(lista, outra.get("codigo").longValue()));
    }
    assertEquals(0, resposta("GET", "/api/cidades?nome=" + unico("ausente"), null, 200).size());
    JsonNode todas = resposta("GET", "/api/cidades", null, 200);
    assertEquals(todas, resposta("GET", "/api/cidades?nome=%20%20", null, 200));
    long anterior = 0;
    for (JsonNode cidade : todas) {
      assertTrue(cidade.get("codigo").longValue() > anterior);
      anterior = cidade.get("codigo").longValue();
    }
  }

  @Test
  void filtrosDeNomeTratamCoringasEConteudoSqlComoTextoLiteral() throws Exception {
    String prefixo = unico("Texto");
    JsonNode especial = cidade(prefixo + " % _", "PR");
    JsonNode comum = cidade(prefixo + " comum", "PR");
    for (String trecho : new String[]{"%", "_"}) {
      JsonNode lista = resposta("GET", "/api/cidades?nome=" + parametro(trecho), null, 200);
      assertTrue(contemCodigo(lista, especial.get("codigo").longValue()));
      assertFalse(contemCodigo(lista, comum.get("codigo").longValue()));
      for (JsonNode cidade : lista) {
        assertTrue(cidade.get("nome").stringValue().contains(trecho));
      }
    }
    assertEquals(0, resposta("GET", "/api/cidades?nome=" + parametro("' OR 1=1 --"), null, 200).size());
  }

  @Test
  void combinaNomeEUfEUsaOCodigoRetornadoNoEndereco() throws Exception {
    String nome = unico("Cidade combinada");
    JsonNode parana = cidade(nome, "PR");
    cidade(nome, "SC");
    JsonNode lista = resposta("GET", "/api/cidades?nome=" + parametro(nome) + "&uf=pr", null, 200);
    assertEquals(1, lista.size());
    assertEquals(parana, lista.get(0));
    JsonNode cadastrada = cliente(endereco(lista.get(0).get("codigo").longValue()));
    assertEquals(parana.get("codigo"), cadastrada.get("endereco").get("codigoCidade"));
    for (JsonNode cidade : resposta("GET", "/api/cidades?uf=%20pr%20", null, 200)) {
      assertEquals("PR", cidade.get("uf").stringValue());
    }
    assertEquals(resposta("GET", "/api/cidades", null, 200),
      resposta("GET", "/api/cidades?uf=%20", null, 200));
  }

  @ParameterizedTest
  @ValueSource(strings = {"XX", "Brasil", "1", "P"})
  void rejeitaFiltroDeUfInvalido(String uf) throws Exception {
    resposta("GET", "/api/cidades?uf=" + uf, null, 400);
  }

  @Test
  void renomeiaCategoriaEPreservaProdutoEHistorico() throws Exception {
    JsonNode categoria = categoria(unico("Categoria"));
    long codigo = categoria.get("codigo").longValue();
    JsonNode produto = produto(codigo);
    String caminho = "/api/produtos/" + produto.get("codigo").longValue();
    resposta("PATCH", caminho + "/precificacao", "{\"custo\":120,\"percentual\":20}", 200);
    JsonNode historico = resposta("GET", caminho + "/historico-precificacao", null, 200);
    assertEquals(1, historico.size());

    String nome = unico("Acessórios");
    JsonNode renomeada = resposta("PATCH", "/api/categorias/" + codigo,
      json.writeValueAsString(Map.of("nome", " " + nome + " ")), 200);
    assertEquals(categoria.get("codigo"), renomeada.get("codigo"));
    assertEquals(nome, renomeada.get("nome").stringValue());
    JsonNode categoriasListadas = resposta("GET", "/api/categorias", null, 200);
    assertTrue(contemCodigo(categoriasListadas, codigo));
    for (JsonNode categoriaListada : categoriasListadas) {
      if (categoriaListada.get("codigo").longValue() == codigo) {
        assertEquals(renomeada, categoriaListada);
      }
    }
    assertTrue(contemCodigo(resposta("GET", "/api/produtos?categoria=" + codigo, null, 200),
      produto.get("codigo").longValue()));
    assertEquals(codigo, resposta("GET", caminho, null, 200).get("codigoCategoria").longValue());
    assertEquals(historico, resposta("GET", caminho + "/historico-precificacao", null, 200));

    resposta("PATCH", caminho + "/precificacao", "{\"custo\":120,\"percentual\":20}", 200);
    assertEquals(historico, resposta("GET", caminho + "/historico-precificacao", null, 200));
    JsonNode novoPreco = resposta("PATCH", caminho + "/precificacao", "{\"custo\":130,\"percentual\":20}", 200);
    assertEquals(codigo, novoPreco.get("codigoCategoria").longValue());
    assertEquals(2, resposta("GET", caminho + "/historico-precificacao", null, 200).size());

    JsonNode outra = categoria(unico("Outra categoria"));
    resposta("PATCH", "/api/categorias/" + codigo, json.writeValueAsString(Map.of(
      "nome", outra.get("nome").stringValue().toUpperCase(java.util.Locale.ROOT)
    )), 400);
    assertEquals(nome, jdbc.queryForObject("SELECT nome FROM categoria WHERE codigo = ?", String.class, codigo));
    assertEquals(2, resposta("GET", caminho + "/historico-precificacao", null, 200).size());
    resposta("PATCH", "/api/categorias/" + codigo, json.writeValueAsString(Map.of("nome", nome)), 200);
  }

  @Test
  void falhaAoRegistrarHistoricoDesfazAlteracaoDoProduto() throws Exception {
    JsonNode produto = produto(null);
    long codigo = produto.get("codigo").longValue();
    String caminho = "/api/produtos/" + codigo;
    JsonNode anterior = resposta("PATCH", caminho + "/precificacao", "{\"custo\":110,\"percentual\":20}", 200);
    JsonNode historicoAnterior = resposta("GET", caminho + "/historico-precificacao", null, 200);
    assertEquals(1, historicoAnterior.size());
    String consultaValores = "SELECT custo, preco_venda, percentual FROM produto WHERE codigo = ?";
    Map<String, Object> valoresAnteriores = jdbc.queryForMap(consultaValores, codigo);
    String identificador = UUID.randomUUID().toString().replace("-", "");
    String sequencia = "tentativa_historico_" + identificador;
    String funcao = "recusar_historico_" + identificador;
    String gatilho = "falhar_historico_" + identificador;
    String alteracao = "{\"custo\":120,\"percentual\":25}";

    try {
      // A sequência confirma a execução do gatilho mesmo quando a transação é desfeita.
      jdbc.execute("CREATE SEQUENCE public." + sequencia);
      jdbc.execute("""
        CREATE FUNCTION public.%s() RETURNS TRIGGER LANGUAGE plpgsql AS $$
        BEGIN
          PERFORM nextval('public.%s');
          RAISE EXCEPTION 'Falha de histórico simulada pelo teste.' USING ERRCODE = 'P0001';
        END;
        $$
        """.formatted(funcao, sequencia));
      jdbc.execute("CREATE TRIGGER " + gatilho
        + " AFTER INSERT ON public.historico_precificacao FOR EACH ROW"
        + " WHEN (NEW.produto_codigo = " + codigo + ") EXECUTE FUNCTION public." + funcao + "()");

      JsonNode falha = resposta("PATCH", caminho + "/precificacao", alteracao, 503);

      assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT is_called FROM public." + sequencia, Boolean.class),
        "A falha deve ter ocorrido durante a inserção do histórico.");
      assertEquals("Não foi possível concluir a operação no banco de dados.", falha.get("detail").stringValue());
      assertEquals(valoresAnteriores, jdbc.queryForMap(consultaValores, codigo),
        "A falha no histórico deve desfazer a alteração do custo, preço e percentual.");
      assertEquals(anterior, resposta("GET", caminho, null, 200));
      assertEquals(historicoAnterior, resposta("GET", caminho + "/historico-precificacao", null, 200));
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS " + gatilho + " ON public.historico_precificacao");
      jdbc.execute("DROP FUNCTION IF EXISTS public." + funcao + "()");
      jdbc.execute("DROP SEQUENCE IF EXISTS public." + sequencia);
    }

    JsonNode atualizado = resposta("PATCH", caminho + "/precificacao", alteracao, 200);
    assertEquals(0, new BigDecimal("150.00").compareTo(atualizado.get("precoVenda").decimalValue()));
    JsonNode historicoAtual = resposta("GET", caminho + "/historico-precificacao", null, 200);
    assertEquals(2, historicoAtual.size());
    assertEquals(historicoAnterior.get(0), historicoAtual.get(1));
    for (String campo : List.of("custo", "precoVenda", "percentual")) {
      assertEquals(anterior.get(campo), historicoAtual.get(0).get("anteriores").get(campo), campo);
      assertEquals(atualizado.get(campo), historicoAtual.get(0).get("novos").get(campo), campo);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "null", "{\"nome\":null}", "{\"nome\":\" \"}", "{\"outro\":1}"})
  void rejeitaRenomeacaoInvalidaSemAlterarCategoria(String corpo) throws Exception {
    JsonNode categoria = categoria(unico("Original"));
    long codigo = categoria.get("codigo").longValue();
    resposta("PATCH", "/api/categorias/" + codigo, corpo, 400);
    assertEquals(categoria.get("nome").stringValue(),
      jdbc.queryForObject("SELECT nome FROM categoria WHERE codigo = ?", String.class, codigo));
  }

  @Test
  void categoriaAusenteECodigoInvalidoTemRespostasDistintas() throws Exception {
    resposta("PATCH", "/api/categorias/9223372036854775807", "{\"nome\":\"Nome\"}", 404);
    resposta("PATCH", "/api/categorias/0", "{\"nome\":\"Nome\"}", 400);
    resposta("PATCH", "/api/categorias/abc", "{\"nome\":\"Nome\"}", 400);
    JsonNode categoria = categoria(unico("Limite"));
    resposta("PATCH", "/api/categorias/" + categoria.get("codigo").longValue(),
      json.writeValueAsString(Map.of("nome", "x".repeat(201))), 400);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PIX", "DINHEIRO", "CARTAO_DEBITO", "CARTAO_CREDITO", "BOLETO"})
  void criaConsultaFiltraESimulaFaturaComClienteEPagamento(String formaPagamento) throws Exception {
    JsonNode cliente = cliente(null);
    JsonNode produto = produto(null);
    String corpo = json.writeValueAsString(Map.of(
      "itens", new Object[]{Map.of("codigoProduto", produto.get("codigo").longValue(), "quantidade", new BigDecimal("1.25"))},
      "codigoCliente", cliente.get("codigo").longValue(),
      "formaPagamento", formaPagamento,
      "desconto", Map.of("tipo", "PERCENTUAL", "valor", 10)
    ));
    Integer antes = jdbc.queryForObject("SELECT COUNT(*) FROM fatura", Integer.class);
    JsonNode simulacao = resposta("POST", "/api/faturas/simulacao", corpo, 200);
    assertEquals(antes, jdbc.queryForObject("SELECT COUNT(*) FROM fatura", Integer.class));
    JsonNode fatura = resposta("POST", "/api/faturas", corpo, 201);
    for (String campo : new String[]{"itens", "subtotal", "descontoAplicado", "total"}) {
      assertEquals(simulacao.get(campo), fatura.get(campo), campo);
    }
    assertEquals(0, new BigDecimal("135.00").compareTo(fatura.get("total").decimalValue()));
    assertEquals(cliente.get("codigo"), fatura.get("codigoCliente"));
    assertEquals(formaPagamento, fatura.get("formaPagamento").stringValue());
    assertEquals(fatura, resposta("GET", "/api/faturas/" + fatura.get("codigo").longValue(), null, 200));
    JsonNode lista = resposta("GET", "/api/faturas?codigoCliente=" + cliente.get("codigo").longValue(), null, 200);
    assertEquals(1, lista.size());
    assertEquals(fatura.get("codigo"), lista.get(0).get("codigo"));
    assertEquals(formaPagamento, lista.get(0).get("formaPagamento").stringValue());
    assertEquals(0, resposta("GET", "/api/faturas?codigoCliente=9223372036854775807", null, 200).size());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
    "Itens ausentes|{}",
    "Lista de itens vazia|{\"itens\":[]}",
    "Item nulo|{\"itens\":[null]}",
    "Produto repetido|{\"itens\":[{\"codigoProduto\":%1$d,\"quantidade\":1},"
      + "{\"codigoProduto\":%1$d,\"quantidade\":2}]}",
    "Quantidade com casas excedentes no segundo item|{\"itens\":[{\"codigoProduto\":%1$d,\"quantidade\":1},"
      + "{\"codigoProduto\":%2$d,\"quantidade\":0.00001}]}",
    "Desconto fixo maior que o subtotal|{\"itens\":[{\"codigoProduto\":%1$d,\"quantidade\":1}],"
      + "\"desconto\":{\"tipo\":\"VALOR_FIXO\",\"valor\":120.01}}",
    "Desconto percentual acima de cem|{\"itens\":[{\"codigoProduto\":%1$d,\"quantidade\":1}],"
      + "\"desconto\":{\"tipo\":\"PERCENTUAL\",\"valor\":100.0001}}",
    "Tipo de desconto desconhecido|{\"itens\":[{\"codigoProduto\":%1$d,\"quantidade\":1}],"
      + "\"desconto\":{\"tipo\":\"OUTRO\",\"valor\":10}}"
  })
  void rejeitaFaturaInvalidaSemGravarOuAlterarFaturaExistente(String cenario, String modelo) throws Exception {
    JsonNode primeiroProduto = produto(null);
    JsonNode segundoProduto = produto(null);
    long primeiroCodigo = primeiroProduto.get("codigo").longValue();
    long segundoCodigo = segundoProduto.get("codigo").longValue();
    JsonNode existente = resposta("POST", "/api/faturas", """
      {"itens":[{"codigoProduto":%d,"quantidade":1}]}
      """.formatted(primeiroCodigo), 201);
    Map<String, Object> registrosAntes = quantidadeDeRegistrosDeFatura();
    String corpo = modelo.formatted(primeiroCodigo, segundoCodigo);

    for (String caminho : List.of("/api/faturas", "/api/faturas/simulacao")) {
      resposta("POST", caminho, corpo, 400);
      assertEquals(registrosAntes, quantidadeDeRegistrosDeFatura(), cenario + ": " + caminho);
      assertEquals(existente, resposta("GET", "/api/faturas/" + existente.get("codigo").longValue(), null, 200),
        cenario + ": a fatura existente deve permanecer igual.");
    }
  }

  @Test
  void mantemPrecisaoDaFaturaNaSimulacaoNoCadastroENaConsulta() throws Exception {
    JsonNode produto = resposta("POST", "/api/produtos", json.writeValueAsString(Map.of(
      "nome", unico("Produto fracionado"), "custo", new BigDecimal("1.23"), "percentual", 0,
      "modalidadePrecificacao", "ACRESCIMO_SOBRE_CUSTO"
    )), 201);
    long codigoProduto = produto.get("codigo").longValue();
    String corpo = """
      {
        "itens": [{"codigoProduto": %d, "quantidade": 0.1234}],
        "desconto": {"tipo": "PERCENTUAL", "valor": 12.3456}
      }
      """.formatted(codigoProduto);
    JsonNode esperado = json.readTree("""
      {
        "itens": [{"codigoProduto": %d, "quantidade": 0.1234,
          "precoVenda": 1.23, "subtotal": 0.151782}],
        "subtotal": 0.151782,
        "descontoAplicado": 0.018738398592,
        "total": 0.13
      }
      """.formatted(codigoProduto));

    JsonNode simulacao = resposta("POST", "/api/faturas/simulacao", corpo, 200);
    JsonNode cadastro = resposta("POST", "/api/faturas", corpo, 201);
    JsonNode consulta = resposta("GET", "/api/faturas/" + cadastro.get("codigo").longValue(), null, 200);

    for (JsonNode fatura : List.of(simulacao, cadastro, consulta)) {
      for (String campo : new String[]{"itens", "subtotal", "descontoAplicado", "total"}) {
        assertEquals(esperado.get(campo), fatura.get(campo), campo);
      }
    }
  }

  @Test
  void faturaSemCamposOpcionaisEReducaoDePrecoPreservamDescontoOriginal() throws Exception {
    JsonNode produto = produto(null);
    String itens = json.writeValueAsString(Map.of(
      "itens", new Object[]{Map.of("codigoProduto", produto.get("codigo").longValue(), "quantidade", 1)}
    ));
    Integer faturasAntesDaSimulacao = jdbc.queryForObject("SELECT COUNT(*) FROM fatura", Integer.class);
    JsonNode simulacao = resposta("POST", "/api/faturas/simulacao", itens, 200);
    assertEquals(faturasAntesDaSimulacao, jdbc.queryForObject("SELECT COUNT(*) FROM fatura", Integer.class));
    JsonNode simples = resposta("POST", "/api/faturas", itens, 201);
    for (String campo : new String[]{"itens", "subtotal", "descontoAplicado", "total"}) {
      assertEquals(simulacao.get(campo), simples.get(campo), campo);
    }
    assertFalse(simples.has("codigoCliente"));
    assertFalse(simples.has("formaPagamento"));
    assertEquals(simples, resposta("GET", "/api/faturas/" + simples.get("codigo").longValue(), null, 200));
    JsonNode faturasListadas = resposta("GET", "/api/faturas", null, 200);
    assertTrue(contemCodigo(faturasListadas, simples.get("codigo").longValue()));
    for (JsonNode faturaListada : faturasListadas) {
      if (faturaListada.get("codigo").equals(simples.get("codigo"))) {
        assertFalse(faturaListada.has("codigoCliente"));
        assertFalse(faturaListada.has("formaPagamento"));
      }
    }

    String corpo = json.writeValueAsString(Map.of(
      "itens", new Object[]{Map.of("codigoProduto", produto.get("codigo").longValue(), "quantidade", 1)},
      "desconto", Map.of("tipo", "VALOR_FIXO", "valor", 100)
    ));
    JsonNode comDesconto = resposta("POST", "/api/faturas", corpo, 201);
    String caminho = "/api/faturas/" + comDesconto.get("codigo").longValue();
    resposta("PATCH", "/api/produtos/" + produto.get("codigo").longValue() + "/precificacao",
      "{\"custo\":50,\"percentual\":20}", 200);
    JsonNode reduzida = resposta("GET", caminho, null, 200);
    assertEquals(0, BigDecimal.ZERO.compareTo(reduzida.get("total").decimalValue()));
    assertEquals(0, new BigDecimal("60").compareTo(reduzida.get("descontoAplicado").decimalValue()));
    resposta("PATCH", "/api/produtos/" + produto.get("codigo").longValue() + "/precificacao",
      "{\"custo\":100,\"percentual\":20}", 200);
    JsonNode restaurada = resposta("GET", caminho, null, 200);
    assertEquals(0, new BigDecimal("100").compareTo(restaurada.get("descontoAplicado").decimalValue()));
  }

  private Map<String, Object> quantidadeDeRegistrosDeFatura() {
    return jdbc.queryForMap("""
      SELECT (SELECT COUNT(*) FROM fatura) AS faturas,
             (SELECT COUNT(*) FROM item_fatura) AS itens
      """);
  }

  private JsonNode cidade(String nome, String uf) throws Exception {
    return resposta("POST", "/api/cidades", json.writeValueAsString(Map.of("nome", nome, "uf", uf)), 201);
  }

  private JsonNode categoria(String nome) throws Exception {
    return resposta("POST", "/api/categorias", json.writeValueAsString(Map.of("nome", nome)), 201);
  }

  private JsonNode cliente(Map<String, Object> endereco) throws Exception {
    var dados = new java.util.HashMap<String, Object>();
    dados.put("nome", "Cliente de teste");
    dados.put("cpf", cpf());
    dados.put("email", unico("cliente") + "@example.com");
    dados.put("telefone", "(41) 99999-0000");
    if (endereco != null) {
      dados.put("endereco", endereco);
    }
    return resposta("POST", "/api/clientes", json.writeValueAsString(dados), 201);
  }

  private JsonNode produto(Long categoria) throws Exception {
    var dados = new java.util.HashMap<String, Object>(Map.of(
      "nome", unico("Produto"), "custo", 100, "percentual", 20,
      "modalidadePrecificacao", "ACRESCIMO_SOBRE_CUSTO"
    ));
    if (categoria != null) {
      dados.put("codigoCategoria", categoria);
    }
    return resposta("POST", "/api/produtos", json.writeValueAsString(dados), 201);
  }

  private Map<String, Object> endereco(long cidade) {
    return Map.of(
      "cep", "80000-000", "logradouro", "Rua de exemplo", "numero", "12A",
      "bairro", "Centro", "complemento", "Sala 2", "codigoCidade", cidade
    );
  }

  private JsonNode getCliente(JsonNode cliente) throws Exception {
    return resposta("GET", caminhoCliente(cliente), null, 200);
  }

  private String caminhoCliente(JsonNode cliente) {
    return "/api/clientes/" + cliente.get("codigo").longValue();
  }

  private JsonNode patchCliente(JsonNode cliente, Map<String, ?> corpo, int status) throws Exception {
    return resposta("PATCH", caminhoCliente(cliente), json.writeValueAsString(corpo), status);
  }

  private JsonNode resposta(String metodo, String caminho, String corpo, int status) throws Exception {
    HttpResponse<String> resposta = http.send(requisicao(metodo, caminho, corpo),
      HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertEquals(status, resposta.statusCode(), metodo + " " + caminho + ": " + resposta.body());
    return json.readTree(resposta.body());
  }

  private HttpRequest requisicao(String metodo, String caminho, String corpo) {
    return HttpRequest.newBuilder(URI.create(baseUrl + caminho)).timeout(Duration.ofSeconds(10))
      .header("Content-Type", "application/json")
      .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build();
  }

  private boolean contemCodigo(JsonNode lista, long codigo) {
    for (JsonNode registro : lista) {
      if (registro.get("codigo").longValue() == codigo) {
        return true;
      }
    }
    return false;
  }

  private String parametro(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }

  private String unico(String prefixo) {
    return prefixo + UUID.randomUUID().toString().replace("-", "");
  }

  private String cpf() {
    StringBuilder digitos = new StringBuilder(Long.toString(CPF_BASE.getAndIncrement()));
    for (int tamanho = 9; tamanho <= 10; tamanho++) {
      int soma = 0;
      for (int indice = 0; indice < tamanho; indice++) {
        soma += (digitos.charAt(indice) - '0') * (tamanho + 1 - indice);
      }
      int resto = soma % 11;
      digitos.append(resto < 2 ? 0 : 11 - resto);
    }
    return digitos.toString();
  }
}
