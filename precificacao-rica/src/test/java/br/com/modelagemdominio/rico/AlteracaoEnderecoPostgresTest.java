package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class AlteracaoEnderecoPostgresTest {

  private static final JsonMapper JSON = new JsonMapper();

  @Test
  void substituiConjuntoRemoveERecriaEnderecoSemAlterarIdentidade() throws Exception {
    try (var cenario = new Cenario()) {
      var novo = endereco(cenario.cidade(), "  Rua d'Ávila  ").put("cep", "80000-123");
      ObjectNode esperado = cenario.original.deepCopy();
      esperado.set("endereco", novo.deepCopy().put("logradouro", "Rua d'Ávila")
        .put("cep", "80000123").putNull("complemento"));
      cenario.conferir(JSON.createObjectNode().set("endereco", novo), esperado);

      esperado.put("email", "outro-" + UUID.randomUUID() + "@example.com");
      cenario.conferir(JSON.createObjectNode().put("email", esperado.path("email").asString()), esperado);

      esperado.putNull("endereco");
      cenario.conferir(JSON.createObjectNode().putNull("endereco"), esperado);
      try (var conexao = cenario.banco.getConnection(); var consulta = conexao.prepareStatement("""
        SELECT num_nonnulls(cep, logradouro, numero, bairro, complemento, cidade_codigo)
        FROM public.cliente WHERE codigo = ?
        """)) {
        consulta.setLong(1, cenario.codigo());
        try (var resultado = consulta.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(0, resultado.getInt(1), "Todas as seis colunas devem ser removidas.");
        }
      }
      cenario.conferir(JSON.createObjectNode().putNull("endereco"), esperado);

      novo = endereco(cenario.cidade(), "Novo endereço").put("complemento", "  Sala 2  ");
      String email = "conjunto-" + UUID.randomUUID() + "@example.com";
      var entrada = JSON.createObjectNode().put("email", email).putNull("telefone");
      entrada.set("endereco", novo);
      esperado.put("email", email).putNull("telefone");
      esperado.set("endereco", novo.deepCopy().put("complemento", "Sala 2"));
      cenario.conferir(entrada, esperado);

      // A remoção deve ser atômica mesmo quando os três campos são enviados.
      entrada.putNull("endereco").put("telefone", "(41) 3333-4444");
      esperado.putNull("endereco").put("telefone", "(41) 3333-4444");
      cenario.conferir(entrada, esperado);
    }
  }

  @Test
  void cidadeAusenteOuEmailDuplicadoPreservaTodoOCadastro() throws Exception {
    try (var cenario = new Cenario(); var ocupado = new Cenario("52998224725")) {
      var entrada = JSON.createObjectNode().put("email", "novo-" + UUID.randomUUID() + "@example.com")
        .put("telefone", "(41) 3333-4444");
      entrada.set("endereco", endereco(Long.MAX_VALUE, "Rua inexistente"));
      assertEquals("Cidade não encontrada.", cenario.alterar(entrada, 404).path("detail").asString());
      assertEquals(cenario.original, cenario.consultar());

      entrada.put("email", ocupado.original.path("email").asString().toUpperCase(java.util.Locale.ROOT));
      entrada.set("endereco", endereco(cenario.cidade(), "Rua não gravada"));
      assertEquals("Já existe um cliente com o e-mail informado.",
        cenario.alterar(entrada, 400).path("detail").asString());
      assertEquals(cenario.original, cenario.consultar());
      assertEquals(ocupado.original, ocupado.consultar());

      try (var conexao = cenario.banco.getConnection();
        var exclusao = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ?")) {
        exclusao.setLong(1, cenario.codigo());
        exclusao.executeUpdate();
      }
      cenario.alterar(JSON.createObjectNode().putNull("endereco"), 404);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void falhaDeMaterializacaoDesfazEnderecoEContatos(boolean remover) throws Exception {
    try (var cenario = new Cenario(); var conexao = cenario.banco.getConnection();
      var comando = conexao.createStatement()) {
      String sufixo = UUID.randomUUID().toString().replace("-", "");
      String funcao = "falha_endereco_" + sufixo;
      String sequencia = "marcador_endereco_" + sufixo;
      String antes = "antes_endereco_" + sufixo;
      String depois = "depois_endereco_" + sufixo;
      String email = "rollback-" + sufixo + "@example.com";
      var entrada = JSON.createObjectNode().put("email", email).putNull("telefone");
      entrada.set("endereco", remover ? JSON.nullNode() : endereco(cenario.cidade(), "Rua nova"));
      try {
        comando.execute("CREATE SEQUENCE public." + sequencia);
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF OLD.codigo = TG_ARGV[0]::BIGINT AND NEW.email = TG_ARGV[1] THEN
              IF TG_WHEN = 'BEFORE' THEN
                NEW.cpf := '11111111111';
              ELSE
                PERFORM nextval('public.%s');
              END IF;
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao, sequencia));
        String acao = " ON public.cliente FOR EACH ROW EXECUTE FUNCTION public." + funcao
          + "('" + cenario.codigo() + "', '" + email + "')";
        comando.execute("CREATE TRIGGER " + antes + " BEFORE UPDATE" + acao);
        comando.execute("CREATE TRIGGER " + depois + " AFTER UPDATE" + acao);
        assertEquals("Não foi possível concluir a operação no banco de dados.",
          cenario.alterar(entrada, 503).path("detail").asString());
        conferirMarcador(conexao, sequencia, 1);
        assertEquals(cenario.original, cenario.consultar());

        comando.execute("DROP TRIGGER " + antes + " ON public.cliente");
        var esperado = cenario.original.deepCopy().put("email", email).putNull("telefone");
        esperado.set("endereco", entrada.path("endereco").deepCopy());
        if (!remover) {
          ((ObjectNode) esperado.path("endereco")).putNull("complemento");
        }
        cenario.conferir(entrada, esperado);
        conferirMarcador(conexao, sequencia, 2);
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          comando.execute("DROP SEQUENCE IF EXISTS public." + sequencia);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void distingueCidadeRemovidaDuranteAlteracaoDeOutraChaveEstrangeira(boolean cidadeRemovida) throws Exception {
    try (var cenario = new Cenario(); var conexao = cenario.banco.getConnection();
      var comando = conexao.createStatement()) {
      long cidade = cenario.cidade();
      String sufixo = UUID.randomUUID().toString().replace("-", "");
      String funcao = "restricao_endereco_" + sufixo;
      String gatilho = "gatilho_endereco_" + sufixo;
      var entrada = JSON.createObjectNode().put("email", "falha-" + sufixo + "@example.com");
      entrada.set("endereco", endereco(cidade, "Rua nova"));
      try {
        String falha = cidadeRemovida
          ? "DELETE FROM public.cidade WHERE codigo = NEW.cidade_codigo;"
          : "RAISE EXCEPTION 'falha de teste' USING ERRCODE = '23503', CONSTRAINT = 'outra_restricao';";
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF OLD.codigo = TG_ARGV[0]::BIGINT THEN
              %s
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao, falha));
        comando.execute("CREATE TRIGGER " + gatilho + " BEFORE UPDATE ON public.cliente "
          + "FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + cenario.codigo() + "')");
        JsonNode problema = cenario.alterar(entrada, cidadeRemovida ? 404 : 503);
        assertEquals(cidadeRemovida ? "Cidade não encontrada."
          : "Não foi possível concluir a operação no banco de dados.", problema.path("detail").asString());
        assertEquals(cenario.original, cenario.consultar());
        comando.execute("DROP FUNCTION public." + funcao + "() CASCADE");
        var esperado = cenario.original.deepCopy().put("email", entrada.path("email").asString());
        esperado.set("endereco", entrada.path("endereco").deepCopy());
        ((ObjectNode) esperado.path("endereco")).putNull("complemento");
        cenario.conferir(entrada, esperado);
      } finally {
        comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
      }
    }
  }

  private static ObjectNode endereco(long cidade, String logradouro) {
    return JSON.createObjectNode().put("cep", "80000000").put("logradouro", logradouro)
      .put("numero", "12A").put("bairro", "Centro").put("codigoCidade", cidade);
  }

  private static void conferirMarcador(Connection conexao, String sequencia, long valor) throws SQLException {
    try (var consulta = conexao.createStatement();
      var resultado = consulta.executeQuery("SELECT last_value, is_called FROM public." + sequencia)) {
      assertTrue(resultado.next());
      assertTrue(resultado.getBoolean("is_called"));
      assertEquals(valor, resultado.getLong("last_value"));
    }
  }

  private static final class Cenario implements AutoCloseable {

    private final PGSimpleDataSource banco = new PGSimpleDataSource();
    private final SqlContado sql;
    private final Take api;
    private final List<Long> cidades = new ArrayList<>();
    private final ObjectNode original;

    Cenario() throws Exception {
      this("11144477735");
    }

    Cenario(String cpf) throws Exception {
      this.banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
      this.banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
      this.banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
      this.sql = new SqlContado(this.banco);
      this.api = AplicacaoRica.rotas(this.sql.banco());
      String email = "endereco-" + UUID.randomUUID() + "@example.com";
      try {
        var entrada = JSON.createObjectNode().put("nome", "Cliente endereço").put("cpf", cpf)
          .put("email", email).put("telefone", "(41) 99999-0000");
        entrada.set("endereco", endereco(this.cidade(), "Rua original").put("complemento", "Bloco A"));
        this.original = (ObjectNode) corpo(201, enviar(this.api, "POST", "/api/clientes", entrada));
      } catch (Exception | AssertionError falha) {
        try {
          this.limpar(email);
        } catch (SQLException limpeza) {
          falha.addSuppressed(limpeza);
        }
        throw falha;
      }
    }

    long cidade() throws Exception {
      long codigo = new PgCidades(this.banco).cadastrar(
        new NomeCidade("Cidade endereço " + UUID.randomUUID()), UnidadeFederativa.PR).codigo();
      this.cidades.add(codigo);
      return codigo;
    }

    long codigo() {
      return this.original.path("codigo").asLong();
    }

    JsonNode alterar(JsonNode entrada, int status) throws Exception {
      this.sql.zerar();
      JsonNode resposta = corpo(status, enviar(this.api, "PATCH", "/api/clientes/" + this.codigo(), entrada));
      assertEquals(1, this.sql.conexoes(), "Validação da cidade e gravação devem compartilhar a conexão.");
      return resposta;
    }

    JsonNode consultar() throws Exception {
      return corpo(200, new RsPrint(this.api.act(new RqFake("GET", "/api/clientes/" + this.codigo()))));
    }

    void conferir(JsonNode entrada, JsonNode esperado) throws Exception {
      // Normaliza os nós numéricos para comparar o JSON enviado pela API.
      JsonNode resposta = JSON.readTree(JSON.writeValueAsString(esperado));
      assertEquals(resposta, this.alterar(entrada, 200));
      assertEquals(resposta, this.consultar());
    }

    @Override
    public void close() throws SQLException {
      this.limpar(null);
    }

    private void limpar(String email) throws SQLException {
      try (var conexao = this.banco.getConnection();
        var cliente = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ? OR email = ?");
        var cidade = conexao.prepareStatement("DELETE FROM public.cidade WHERE codigo = ?")) {
        cliente.setLong(1, this.original == null ? 0 : this.codigo());
        cliente.setString(2, email);
        cliente.executeUpdate();
        for (long codigo : this.cidades) {
          cidade.setLong(1, codigo);
          cidade.executeUpdate();
        }
      }
    }
  }

  private static RsPrint enviar(Take api, String metodo, String caminho, JsonNode entrada) throws Exception {
    byte[] bytes = JSON.writeValueAsBytes(entrada);
    return new RsPrint(api.act(new RqFake(List.of(
      metodo + " " + caminho + " HTTP/1.1", "Content-Type: application/json", "Content-Length: " + bytes.length
    ), bytes)));
  }

  private static JsonNode corpo(int status, RsPrint resposta) throws Exception {
    String linha = resposta.head().iterator().next();
    assertEquals(Integer.toString(status), linha.split(" ", 3)[1], linha);
    return JSON.readTree(resposta.printBody());
  }
}
