package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.Cidade;
import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.http.TkListaCidades;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import br.com.modelagemdominio.rico.testes.SqlContado;
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
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaCidadesTest {

  private final JsonMapper json = new JsonMapper();

  @Test
  void respondeComArrayVazioQuandoNaoHaCidades() throws Exception {
    Cidades vazias = new Cidades() {
      @Override
      public Cidade cadastrar(NomeCidade nome, UnidadeFederativa uf) {
        throw new AssertionError("A listagem não deve cadastrar uma cidade.");
      }

      @Override
      public List<Cidade> listar(FiltrosCidades filtros) {
        return List.of();
      }

      @Override
      public boolean existe(CodigoCidade codigo) {
        throw new AssertionError("A listagem não deve consultar uma cidade por código.");
      }
    };
    Take api = new TkFork(new FkRegex("/api/cidades", new TkListaCidades(vazias)));

    var resposta = consultar(api);

    assertTrue(resposta.printHead().lines().anyMatch("Content-Type: application/json"::equalsIgnoreCase));
    assertEquals(this.json.readTree("[]"), this.corpo(200, resposta));
  }

  @Test
  @Tag("integracao")
  void listaPorCodigoEAplicaFiltroDeNome() throws Exception {
    var banco = bancoDeTeste();
    UUID sufixo = UUID.randomUUID();
    String primeiroNome = "São João d'Aliança " + sufixo + " %_ +&=50%";
    String inicioSegundoNome = "Água Branca " + sufixo + " XY";
    String segundoNome = inicioSegundoNome + "x".repeat(200 - inicioSegundoNome.length());
    var sql = new SqlContado(banco);
    Take api = new RotasDaAplicacaoRica(sql.banco()).rotas();
    assertEquals(0, sql.conexoes());
    assertEquals(0, sql.comandos());

    try (Connection conexao = banco.getConnection()) {
      long primeiro = reservarCodigo(conexao);
      long segundo = reservarCodigo(conexao);
      try {
        inserir(conexao, segundo, segundoNome, "AL");
        inserir(conexao, primeiro, primeiroNome, "PB");

        JsonNode cidades = this.corpo(200, consultar(api));
        assertEquals(1, sql.conexoes());
        assertEquals(1, sql.comandos());

        assertTrue(cidades.isArray());
        List<JsonNode> cidadesDoTeste = new ArrayList<>();
        for (JsonNode cidade : cidades) {
          long codigo = cidade.path("codigo").asLong();
          if (codigo == primeiro || codigo == segundo) {
            cidadesDoTeste.add(cidade);
          }
        }
        JsonNode primeiraCidade = this.json.readTree("""
          {"codigo":%d,"nome":"%s","uf":"PB"}
          """.formatted(primeiro, primeiroNome));
        JsonNode segundaCidade = this.json.readTree("""
          {"codigo":%d,"nome":"%s","uf":"AL"}
          """.formatted(segundo, segundoNome));
        assertEquals(List.of(primeiraCidade, segundaCidade), cidadesDoTeste);

        sql.zerar();
        assertEquals(this.json.createArrayNode().add(primeiraCidade).add(segundaCidade),
          this.corpo(200, consultarPorNome(api, sufixo.toString().toUpperCase(Locale.ROOT))));
        assertEquals(1, sql.conexoes());
        assertEquals(1, sql.comandos());

        this.conferirFiltroPorNome(api, cidades, sufixo, primeiraCidade, segundaCidade);
      } finally {
        try (var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE codigo IN (?, ?)")) {
          exclusao.setLong(1, primeiro);
          exclusao.setLong(2, segundo);
          exclusao.executeUpdate();
        }
      }
    }
  }

  @Test
  @Tag("integracao")
  void combinaNomeEUfMantendoOsCodigosECamposDasCidades() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = new RotasDaAplicacaoRica(sql.banco()).rotas();
    UUID sufixo = UUID.randomUUID();
    String nomeComum = "São Tomé " + sufixo;
    String outroNome = "Outra cidade " + sufixo;
    try {
      JsonNode paraiba = this.cadastrar(api, nomeComum, "PB");
      JsonNode alagoas = this.cadastrar(api, nomeComum, "AL");
      this.cadastrar(api, outroNome, "PB");
      JsonNode semFiltro = this.consultarEmUmaConsulta(api, sql, "");
      var esperadasNaParaiba = this.json.createArrayNode();
      for (JsonNode cidade : semFiltro) {
        if ("PB".equals(cidade.path("uf").asString())) {
          esperadasNaParaiba.add(cidade);
        }
      }
      var mesmoNome = this.json.createArrayNode().add(paraiba).add(alagoas);
      String nomeCodificado = URLEncoder.encode(nomeComum, StandardCharsets.UTF_8);
      String nomeComEspacos = URLEncoder.encode("  " + nomeComum + "  ", StandardCharsets.UTF_8);
      String outroNomeCodificado = URLEncoder.encode(outroNome, StandardCharsets.UTF_8);

      assertEquals(esperadasNaParaiba, this.consultarEmUmaConsulta(api, sql, "?uf=+pb+"));
      assertEquals(mesmoNome, this.consultarEmUmaConsulta(api, sql, "?nome=" + nomeCodificado));
      assertEquals(this.json.createArrayNode().add(alagoas),
        this.consultarEmUmaConsulta(api, sql, "?uf=+al+&nome=" + nomeComEspacos));
      assertEquals(this.json.createArrayNode().add(paraiba),
        this.consultarEmUmaConsulta(api, sql, "?nome=" + nomeCodificado + "&uf=pb"));
      assertEquals(this.json.createArrayNode(),
        this.consultarEmUmaConsulta(api, sql, "?nome=" + outroNomeCodificado + "&uf=AL"));
      assertEquals(esperadasNaParaiba, this.consultarEmUmaConsulta(api, sql, "?nome=++&uf=pb"));
      assertEquals(mesmoNome, this.consultarEmUmaConsulta(api, sql, "?uf=++&nome=" + nomeCodificado));
      assertEquals(semFiltro, this.consultarEmUmaConsulta(api, sql, "?uf="));
      assertEquals(mesmoNome, this.consultarEmUmaConsulta(api, sql, "?uf=++&uf=PB&nome=" + nomeCodificado));
    } finally {
      try (var conexao = banco.getConnection();
        var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE nome IN (?, ?)")) {
        exclusao.setString(1, nomeComum);
        exclusao.setString(2, outroNome);
        exclusao.executeUpdate();
      }
    }
  }

  @Test
  @Tag("integracao")
  void leiturasDaCidadeConservamValoresDepoisDeAlterarORegistro() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    var cidades = new PgCidades(sql.banco());
    String nome = "Cidade da leitura " + UUID.randomUUID();
    String novoNome = nome + " alterada";
    var filtro = new FiltrosCidades(Optional.of(new NomeCidade(nome)), Optional.empty());
    try {
      Cidade cadastrada = cidades.cadastrar(new NomeCidade(nome), UnidadeFederativa.PR);
      long codigo = cadastrada.codigo();
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());

      sql.zerar();
      List<Cidade> leitura = cidades.listar(filtro);
      assertEquals(1, leitura.size());

      try (var conexao = banco.getConnection(); var alterar = conexao.prepareStatement(
        "UPDATE public.cidade SET nome = ?, uf = 'GO' WHERE codigo = ?")) {
        alterar.setString(1, novoNome);
        alterar.setLong(2, codigo);
        assertEquals(1, alterar.executeUpdate());
      }

      for (Cidade anterior : List.of(cadastrada, leitura.get(0))) {
        assertEquals(codigo, anterior.codigo());
        assertEquals(new NomeCidade(nome), anterior.nome());
        assertEquals(UnidadeFederativa.PR, anterior.uf());
      }
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());

      sql.zerar();
      List<Cidade> atual = cidades.listar(filtro);
      assertEquals(1, atual.size());
      assertEquals(codigo, atual.get(0).codigo());
      assertEquals(new NomeCidade(novoNome), atual.get(0).nome());
      assertEquals(UnidadeFederativa.GO, atual.get(0).uf());
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());
    } finally {
      try (var conexao = banco.getConnection(); var excluir = conexao.prepareStatement(
        "DELETE FROM public.cidade WHERE nome IN (?, ?)")) {
        excluir.setString(1, nome);
        excluir.setString(2, novoNome);
        excluir.executeUpdate();
      }
    }
  }

  @Test
  void informaIndisponibilidadeEmVezDeResponderComListaVazia() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    JsonNode problema = this.corpo(503, consultarPorNome(new RotasDaAplicacaoRica(banco).rotas(), "Curitiba"));

    assertEquals("Persistência indisponível", problema.path("title").asString());
    assertEquals("/api/cidades", problema.path("instance").asString());
  }

  @Test
  void recusaFiltroAcimaDoLimiteAntesDeAcessarOBanco() throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();

    var problema = this.corpo(400, consultarPorNome(api, "x".repeat(201)));

    assertEquals("O nome da cidade deve ter no máximo 200 caracteres.", problema.path("detail").asString());
  }

  @Test
  void recusaCaractereNuloNoFiltroAntesDeAcessarOBanco() throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();

    var problema = this.corpo(400, consultarPorNome(api, "Cidade\0inválida"));

    assertEquals("Informe um nome de cidade válido.", problema.path("detail").asString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"?uf=XX", "?nome=Curitiba&uf=XX"})
  void recusaUfInvalidaAntesDeAcessarOBanco(String consulta) throws Exception {
    Take api = new RotasDaAplicacaoRica(new DataSourceSemConexao()).rotas();

    var problema = this.corpo(400, consultar(api, consulta));

    assertEquals("Informe uma sigla de UF brasileira válida.", problema.path("detail").asString());
  }

  private void conferirFiltroPorNome(Take api, JsonNode semFiltro, UUID sufixo, JsonNode primeira, JsonNode segunda)
    throws Exception {
    String trecho = sufixo.toString();
    assertEquals(this.json.createArrayNode().add(segunda),
      this.corpo(200, consultarPorNome(api, "branca " + trecho)));
    assertEquals(this.json.createArrayNode().add(primeira),
      this.corpo(200, consultarPorNome(api, "D'ALIANÇA " + trecho)));
    assertEquals(this.json.createArrayNode().add(segunda), this.corpo(200,
      consultarPorNome(api, "  " + segunda.path("nome").asString().toUpperCase(Locale.ROOT) + "  ")));
    assertEquals(this.json.createArrayNode().add(primeira),
      this.corpo(200, consultarPorNome(api, "  " + trecho + " %_  ")));
    assertEquals(this.json.createArrayNode().add(primeira),
      this.corpo(200, consultarPorNome(api, trecho + " %_ +&=50%")));
    assertEquals(this.json.createArrayNode(), this.corpo(200, consultarPorNome(api, trecho + " _")));
    assertEquals(this.json.createArrayNode(), this.corpo(200, consultarPorNome(api, trecho + "' OR TRUE --")));
    assertEquals(semFiltro, this.corpo(200, consultarPorNome(api, "  ")));
    assertEquals(semFiltro, this.corpo(200, consultarPorNome(api, "")));
  }

  private JsonNode consultarEmUmaConsulta(Take api, SqlContado sql, String parametros) throws Exception {
    sql.zerar();
    JsonNode cidades = this.corpo(200, consultar(api, parametros));
    assertEquals(1, sql.conexoes());
    assertEquals(1, sql.comandos());
    return cidades;
  }

  private JsonNode cadastrar(Take api, String nome, String uf) throws Exception {
    byte[] corpo = this.json.writeValueAsBytes(this.json.createObjectNode().put("nome", nome).put("uf", uf));
    return this.corpo(201, new RsPrint(api.act(new RqFake(List.of(
      "POST /api/cidades HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + corpo.length
    ), corpo))));
  }

  private static PGSimpleDataSource bancoDeTeste() {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    return banco;
  }

  private static RsPrint consultarPorNome(Take api, String nome) throws Exception {
    String termo = URLEncoder.encode(nome, StandardCharsets.UTF_8);
    return consultar(api, "?nome=" + termo);
  }

  private static RsPrint consultar(Take api) throws Exception {
    return consultar(api, "");
  }

  private static RsPrint consultar(Take api, String parametros) throws Exception {
    return new RsPrint(api.act(new RqFake("GET", "/api/cidades" + parametros)));
  }

  private JsonNode corpo(int status, RsPrint resposta) throws Exception {
    assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
    return this.json.readTree(resposta.printBody());
  }

  private static long reservarCodigo(Connection conexao) throws SQLException {
    try (var consulta = conexao.prepareStatement(
      "SELECT nextval(pg_get_serial_sequence('public.cidade', 'codigo'))"
    ); var resultado = consulta.executeQuery()) {
      assertTrue(resultado.next());
      return resultado.getLong(1);
    }
  }

  private static void inserir(Connection conexao, long codigo, String nome, String uf) throws SQLException {
    try (var insercao = conexao.prepareStatement("INSERT INTO public.cidade (codigo, nome, uf) VALUES (?, ?, ?)")) {
      insercao.setLong(1, codigo);
      insercao.setString(2, nome);
      insercao.setString(3, uf);
      insercao.executeUpdate();
    }
  }
}
