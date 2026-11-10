package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.Cidade;
import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.Cep;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Cpf;
import br.com.modelagemdominio.rico.clientes.Email;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.LocalizacaoEndereco;
import br.com.modelagemdominio.rico.clientes.NomeCliente;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class CadastroClientePostgresTest {

  private static final String CPF = "12345678909";
  private static final String OUTRO_CPF = "52998224725";

  private final JsonMapper json = new JsonMapper();

  @Test
  void aceitaLimitesDoCadastroENormalizaDadosQueSaoLidosDoBanco() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String nomeCidade = "Cidade dos limites " + sufixo;
    String inicioEmail = "Cliente-" + sufixo;
    String email = inicioEmail + "x".repeat(64 - inicioEmail.length())
      + "@" + "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(61);
    String inicioNome = "Ana d'Ávila ";
    String nome = inicioNome + "x".repeat(200 - inicioNome.length());
    String logradouro = "Rua " + "x".repeat(196);
    String numero = "12A" + "x".repeat(17);
    String bairro = "Bairro " + "x".repeat(93);
    String complemento = "Bloco " + "x".repeat(94);
    try {
      Cidade cidade = new PgCidades(banco).cadastrar(new NomeCidade(nomeCidade), UnidadeFederativa.PR);
      ObjectNode pedido = this.cliente(" 123.456.789-09 ", "  " + email + "  ")
        .put("nome", "  " + nome + "  ").put("telefone", "   ");
      pedido.set("endereco", this.json.createObjectNode()
        .put("cep", " 80000-000 ").put("logradouro", "  " + logradouro + "  ")
        .put("numero", "  " + numero + "  ").put("bairro", "  " + bairro + "  ")
        .put("complemento", "  " + complemento + "  ").put("codigoCidade", cidade.codigo()));

      JsonNode cadastrado = this.cadastrar(api, 201, pedido);
      long codigo = cadastrado.path("codigo").asLong();
      assertTrue(codigo > 0);
      ObjectNode esperado = this.json.createObjectNode().put("codigo", codigo)
        .put("nome", nome).put("cpf", CPF).put("email", email).putNull("telefone");
      esperado.set("endereco", this.json.createObjectNode().put("cep", "80000000")
        .put("logradouro", logradouro).put("numero", numero).put("bairro", bairro)
        .put("complemento", complemento).put("codigoCidade", cidade.codigo()));
      JsonNode esperadoJson = this.json.readTree(esperado.toString());
      assertEquals(esperadoJson, cadastrado);

      sql.zerar();
      JsonNode listagem = this.listar(api);
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());
      assertEquals(esperadoJson, clienteDaLista(listagem, codigo));
    } finally {
      excluirClientes(banco, List.of(email));
      try (var conexao = banco.getConnection();
        var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE nome = ?")) {
        exclusao.setString(1, nomeCidade);
        exclusao.executeUpdate();
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"23505, outra_unicidade", "23503, outra_referencia"})
  void falhaDeOutraRestricaoNaoViraDuplicidadeOuCidadeAusente(String estado, String restricao) throws Exception {
    var banco = bancoDeTeste();
    Take api = AplicacaoRica.rotas(banco);
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String emailAnterior = "anterior-" + sufixo + "@example.com";
    String emailNovo = "rejeitado-" + sufixo + "@example.com";
    String funcao = "falha_cliente_" + sufixo;
    String gatilho = "gatilho_cliente_" + sufixo;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        this.cadastrar(api, 201, this.cliente(OUTRO_CPF, emailAnterior));
        JsonNode antes = this.listar(api);
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF NEW.email = TG_ARGV[0] THEN
              RAISE EXCEPTION USING ERRCODE = '%s',
                MESSAGE = 'Falha intencional no cadastro', CONSTRAINT = '%s';
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao, estado, restricao));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER INSERT ON public.cliente"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + emailNovo + "')");
        ObjectNode pedido = this.cliente(CPF, emailNovo).putNull("telefone").putNull("endereco");

        JsonNode problema = this.cadastrar(api, 503, pedido);
        assertEquals(this.json.readTree("""
          {"title":"Persistência indisponível","status":503,
           "detail":"Não foi possível concluir a operação no banco de dados.","instance":"/api/clientes"}
          """), problema);
        assertEquals(antes, this.listar(api));

        comando.execute("DROP FUNCTION public." + funcao + "() CASCADE");
        JsonNode repeticao = this.cadastrar(api, 201, pedido);
        assertTrue(repeticao.path("telefone").isNull());
        assertTrue(repeticao.path("endereco").isNull());
        assertEquals(repeticao, clienteDaLista(this.listar(api), repeticao.path("codigo").asLong()));
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          excluirClientes(banco, List.of(emailNovo, emailAnterior));
        }
      }
    }
  }

  @Test
  void cidadeRemovidaAposValidacaoImpedeCadastroPelaChaveEstrangeiraReal() throws Exception {
    var banco = bancoDeTeste();
    var cidades = new PgCidades(banco);
    String sufixo = UUID.randomUUID().toString();
    String nomeCidade = "Cidade removida " + sufixo;
    String email = "cidade-removida-" + sufixo + "@example.com";
    try {
      Cidade cidade = cidades.cadastrar(new NomeCidade(nomeCidade), UnidadeFederativa.PR);
      Cidades cidadesComRemocao = new Cidades() {
        @Override
        public Cidade cadastrar(NomeCidade nome, UnidadeFederativa uf) {
          throw new AssertionError("O cadastro de cliente não deve cadastrar uma cidade.");
        }

        @Override
        public List<Cidade> listar(FiltrosCidades filtros) {
          throw new AssertionError("O cadastro de cliente não deve listar cidades.");
        }

        @Override
        public boolean existe(CodigoCidade codigo) throws FalhaCidade {
          boolean existia = cidades.existe(codigo);
          try (var conexao = banco.getConnection();
            var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE codigo = ?")) {
            exclusao.setLong(1, cidade.codigo());
            assertEquals(1, exclusao.executeUpdate());
          } catch (SQLException falha) {
            throw new FalhaCidade(FalhaCidade.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
          }
          assertTrue(existia);
          return existia;
        }
      };
      var clientes = new PgClientes(banco, cidadesComRemocao);
      var antes = new PgClientes(banco, cidades).listar();
      var identificacao = new IdentificacaoCliente(new NomeCliente("Cliente para vínculo"), new Cpf(CPF));
      var contato = new ContatoCliente(new Email(email), Optional.empty());
      var endereco = new Endereco(new Cep("80000000"),
        new LocalizacaoEndereco("Rua A", "12", "Centro", Optional.empty()), new CodigoCidade(cidade.codigo()));

      FalhaCliente falha = assertThrows(FalhaCliente.class,
        () -> clientes.cadastrar(identificacao, contato, Optional.of(endereco)));

      assertEquals(FalhaCliente.Motivo.CIDADE_AUSENTE, falha.motivo());
      PSQLException causa = assertInstanceOf(PSQLException.class, falha.getCause());
      assertEquals("23503", causa.getSQLState());
      assertNotNull(causa.getServerErrorMessage());
      assertEquals("cliente_cidade_codigo_fkey", causa.getServerErrorMessage().getConstraint());
      assertFalse(cidades.existe(new CodigoCidade(cidade.codigo())));
      assertEquals(antes, new PgClientes(banco, cidades).listar());
    } finally {
      excluirClientes(banco, List.of(email));
      try (var conexao = banco.getConnection();
        var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE nome = ?")) {
        exclusao.setString(1, nomeCidade);
        exclusao.executeUpdate();
      }
    }
  }

  private ObjectNode cliente(String cpf, String email) {
    return this.json.createObjectNode().put("nome", "Cliente de teste").put("cpf", cpf).put("email", email);
  }

  private JsonNode cadastrar(Take api, int status, ObjectNode pedido) throws Exception {
    byte[] corpo = this.json.writeValueAsBytes(pedido);
    return this.resposta(status, new RsPrint(api.act(new RqFake(List.of(
      "POST /api/clientes HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + corpo.length
    ), corpo))));
  }

  private JsonNode listar(Take api) throws Exception {
    return this.resposta(200, new RsPrint(api.act(new RqFake("GET", "/api/clientes"))));
  }

  private JsonNode resposta(int status, RsPrint resposta) throws Exception {
    String primeiraLinha = resposta.head().iterator().next();
    assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], primeiraLinha);
    return this.json.readTree(resposta.printBody());
  }

  private static JsonNode clienteDaLista(JsonNode clientes, long codigo) {
    for (JsonNode cliente : clientes) {
      if (cliente.path("codigo").asLong() == codigo) {
        return cliente;
      }
    }
    throw new AssertionError("Cliente ausente na listagem: " + codigo);
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
