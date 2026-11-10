package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
class AlteracaoEmailPostgresTest {

  private final JsonMapper json = new JsonMapper();

  @Test
  void alteraSomenteEmailPreservandoCadastroERecusandoConflito() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String nomeCidade = "Cidade do e-mail " + sufixo;
    String email = "anterior-" + sufixo + "@example.com";
    String ocupado = "ocupado-" + sufixo + "@example.com";
    String inicio = "Novo.d'Avila-" + sufixo;
    String novoEmail = inicio + "x".repeat(64 - inicio.length())
      + "@" + "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(61);
    String funcao = "falha_email_campos_" + sufixo;
    String gatilho = "gatilho_email_campos_" + sufixo;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        var cidade = new PgCidades(banco).cadastrar(new NomeCidade(nomeCidade), UnidadeFederativa.PR);
        ObjectNode pedido = this.cliente("123.456.789-09", email).put("telefone", "+55 (41) 99999-0000");
        pedido.set("endereco", this.json.createObjectNode().put("cep", "80000-000")
          .put("logradouro", "Rua das Flores").put("numero", "12A").put("bairro", "Centro")
          .put("complemento", "Apto 12").put("codigoCidade", cidade.codigo()));
        JsonNode original = this.cadastrar(api, pedido);
        JsonNode outro = this.cadastrar(api, this.cliente("52998224725", ocupado));
        long codigo = original.path("codigo").asLong();
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF OLD.codigo = TG_ARGV[0]::BIGINT THEN
              RAISE EXCEPTION 'Alteracao de coluna nao solicitada';
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao));
        comando.execute("CREATE TRIGGER " + gatilho
          + " BEFORE UPDATE OF codigo, nome, cpf, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo"
          + " ON public.cliente FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + codigo + "')");

        ObjectNode esperado = ((ObjectNode) original).deepCopy().put("email", novoEmail);
        assertEquals(esperado, this.alterar(api, sql, codigo, "  " + novoEmail + "  ", 200));
        assertEquals(esperado, this.consultar(api, codigo));
        esperado.put("email", novoEmail.toUpperCase(Locale.ROOT));
        assertEquals(esperado, this.alterar(api, sql, codigo, novoEmail.toUpperCase(Locale.ROOT), 200));

        JsonNode conflito = this.alterar(api, sql, codigo, ocupado.toUpperCase(Locale.ROOT), 400);
        assertEquals("Já existe um cliente com o e-mail informado.", conflito.path("detail").asString());
        assertEquals(esperado, this.consultar(api, codigo));
        assertEquals(outro, this.consultar(api, outro.path("codigo").asLong()));

        try (var exclusao = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ?")) {
          exclusao.setLong(1, codigo);
          assertEquals(1, exclusao.executeUpdate());
        }
        JsonNode ausente = this.alterar(api, sql, codigo, novoEmail, 404);
        assertEquals("Cliente não encontrado.", ausente.path("detail").asString());
        assertEquals(outro, this.consultar(api, outro.path("codigo").asLong()));
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          excluirClientes(banco, List.of(email, novoEmail, ocupado));
          try (var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE nome = ?")) {
            exclusao.setString(1, nomeCidade);
            exclusao.executeUpdate();
          }
        }
      }
    }
  }

  @Test
  void falhaDeOutraUnicidadeRetorna503EPreservaEmailAnterior() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String email = "original-" + sufixo + "@example.com";
    String novoEmail = "novo-" + sufixo + "@example.com";
    String funcao = "falha_email_unicidade_" + sufixo;
    String gatilho = "gatilho_email_unicidade_" + sufixo;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        JsonNode original = this.cadastrar(api, this.cliente("12345678909", email));
        long codigo = original.path("codigo").asLong();
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF NEW.codigo = TG_ARGV[0]::BIGINT THEN
              RAISE EXCEPTION USING ERRCODE = '23505',
                MESSAGE = 'Falha intencional na atualizacao', CONSTRAINT = 'outra_unicidade';
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER UPDATE ON public.cliente"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + codigo + "')");

        this.conferirIndisponibilidade(this.alterar(api, sql, codigo, novoEmail, 503), codigo);
        assertEquals(original, this.consultar(api, codigo));

        comando.execute("DROP FUNCTION public." + funcao + "() CASCADE");
        ObjectNode esperado = ((ObjectNode) original).deepCopy().put("email", novoEmail);
        assertEquals(esperado, this.alterar(api, sql, codigo, novoEmail, 200));
        assertEquals(esperado, this.consultar(api, codigo));
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          excluirClientes(banco, List.of(email, novoEmail));
        }
      }
    }
  }

  @Test
  void desfazEmailSeARespostaFalharDepoisDaAtualizacao() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String email = "anterior-" + sufixo + "@example.com";
    String novoEmail = "novo-" + sufixo + "@example.com";
    String funcao = "falha_email_marcador_" + sufixo;
    String gatilho = "gatilho_email_marcador_" + sufixo;
    String sequencia = "marcador_email_" + sufixo;
    String telefoneInvalido = "inválido";
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        JsonNode original = this.cadastrar(api,
          this.cliente("12345678909", email).put("telefone", "(41) 99999-0000"));
        long codigo = original.path("codigo").asLong();
        try (var atualizacao = conexao.prepareStatement("UPDATE public.cliente SET telefone = ? WHERE codigo = ?")) {
          atualizacao.setString(1, telefoneInvalido);
          atualizacao.setLong(2, codigo);
          assertEquals(1, atualizacao.executeUpdate());
        }
        comando.execute("CREATE SEQUENCE public." + sequencia);
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF NEW.codigo = TG_ARGV[0]::BIGINT AND NEW.email = TG_ARGV[1] THEN
              PERFORM nextval('public.%s');
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao, sequencia));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER UPDATE ON public.cliente"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + codigo + "', '" + novoEmail + "')");

        this.conferirIndisponibilidade(this.alterar(api, sql, codigo, novoEmail, 503), codigo);
        conferirMarcador(conexao, sequencia, 1);
        try (var consulta = conexao.prepareStatement("SELECT email, telefone FROM public.cliente WHERE codigo = ?")) {
          consulta.setLong(1, codigo);
          try (var resultado = consulta.executeQuery()) {
            assertTrue(resultado.next());
            assertEquals(email, resultado.getString("email"));
            assertEquals(telefoneInvalido, resultado.getString("telefone"));
          }
        }

        try (var restauracao = conexao.prepareStatement("UPDATE public.cliente SET telefone = ? WHERE codigo = ?")) {
          restauracao.setString(1, original.path("telefone").asString());
          restauracao.setLong(2, codigo);
          assertEquals(1, restauracao.executeUpdate());
        }
        ObjectNode esperado = ((ObjectNode) original).deepCopy().put("email", novoEmail);
        assertEquals(esperado, this.alterar(api, sql, codigo, novoEmail, 200));
        assertEquals(esperado, this.consultar(api, codigo));
        conferirMarcador(conexao, sequencia, 2);
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          try {
            comando.execute("DROP SEQUENCE IF EXISTS public." + sequencia);
          } finally {
            excluirClientes(banco, List.of(email, novoEmail));
          }
        }
      }
    }
  }

  private ObjectNode cliente(String cpf, String email) {
    return this.json.createObjectNode().put("nome", "Ana d'Ávila").put("cpf", cpf).put("email", email);
  }

  private JsonNode cadastrar(Take api, ObjectNode pedido) throws Exception {
    return this.resposta(201, enviar(api, "POST", "/api/clientes", this.json.writeValueAsBytes(pedido)));
  }

  private JsonNode alterar(Take api, SqlContado sql, long codigo, String email, int status) throws Exception {
    sql.zerar();
    byte[] corpo = this.json.writeValueAsBytes(this.json.createObjectNode().put("email", email));
    JsonNode resposta = this.resposta(status, enviar(api, "PATCH", "/api/clientes/" + codigo, corpo));
    assertEquals(1, sql.conexoes());
    return resposta;
  }

  private JsonNode consultar(Take api, long codigo) throws Exception {
    return this.resposta(200, new RsPrint(api.act(new RqFake("GET", "/api/clientes/" + codigo))));
  }

  private JsonNode resposta(int status, RsPrint resposta) throws Exception {
    String primeiraLinha = resposta.head().iterator().next();
    assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], primeiraLinha);
    return this.json.readTree(resposta.printBody());
  }

  private void conferirIndisponibilidade(JsonNode problema, long codigo) {
    assertEquals(this.json.readTree("""
      {"title":"Persistência indisponível","status":503,
       "detail":"Não foi possível concluir a operação no banco de dados.","instance":"/api/clientes/%d"}
      """.formatted(codigo)), problema);
  }

  private static RsPrint enviar(Take api, String metodo, String caminho, byte[] corpo) throws Exception {
    return new RsPrint(api.act(new RqFake(List.of(
      metodo + " " + caminho + " HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + corpo.length
    ), corpo)));
  }

  private static void conferirMarcador(Connection conexao, String sequencia, long esperado) throws SQLException {
    try (var consulta = conexao.createStatement();
      var resultado = consulta.executeQuery("SELECT last_value, is_called FROM public." + sequencia)) {
      assertTrue(resultado.next());
      assertTrue(resultado.getBoolean("is_called"));
      assertEquals(esperado, resultado.getLong("last_value"));
    }
  }

  private static void excluirClientes(PGSimpleDataSource banco, List<String> emails) throws SQLException {
    try (var conexao = banco.getConnection();
      var exclusao = conexao.prepareStatement("DELETE FROM public.cliente WHERE LOWER(email) = LOWER(?)")) {
      for (String email : emails) {
        exclusao.setString(1, email);
        exclusao.executeUpdate();
      }
    }
  }

  private static PGSimpleDataSource bancoDeTeste() {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    return banco;
  }
}
