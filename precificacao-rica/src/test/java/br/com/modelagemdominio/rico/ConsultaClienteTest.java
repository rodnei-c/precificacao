package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import br.com.modelagemdominio.rico.testes.SqlContado;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaClienteTest {

  private final JsonMapper json = new JsonMapper();

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808"})
  void rejeitaCodigoInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
    Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

    JsonNode problema = this.problema(api, codigo, 400);

    assertEquals("Informe um código de cliente inteiro positivo válido.", problema.path("detail").asString());
  }

  @Test
  void informaIndisponibilidadeAoConsultarCliente() throws Exception {
    var banco = new HikariDataSource();
    banco.close();

    JsonNode problema = this.problema(AplicacaoRica.rotas(banco), "1", 503);

    assertEquals("Persistência indisponível", problema.path("title").asString());
  }

  @Test
  @Tag("integracao")
  void consultaPorCodigoComESemEnderecoERefleteAlteracoesDoBanco() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String sufixo = UUID.randomUUID().toString();
    String nomeCidade = "Cidade para consulta " + sufixo;
    String emailCompleto = "Completo-" + sufixo + "@example.com";
    String emailSimples = "Simples-" + sufixo + "@example.com";
    String novoEmail = "Atualizado-" + sufixo + "@example.com";
    try {
      var cidade = new PgCidades(banco).cadastrar(new NomeCidade(nomeCidade), UnidadeFederativa.PR);
      ObjectNode pedido = this.cliente("Cliente d'Ávila", "123.456.789-09", emailCompleto)
        .put("telefone", "(41) 99999-0000");
      pedido.set("endereco", this.json.createObjectNode().put("cep", "80000-000")
        .put("logradouro", "Rua A").put("numero", "12A").put("bairro", "Centro")
        .put("complemento", "Apto 12").put("codigoCidade", cidade.codigo()));
      JsonNode completo = this.cadastrar(api, pedido);
      JsonNode simples = this.cadastrar(api,
        this.cliente("Cliente simples", "52998224725", emailSimples).putNull("telefone").putNull("endereco"));
      long codigoCompleto = completo.path("codigo").asLong();
      long codigoSimples = simples.path("codigo").asLong();
      String caminhoCompleto = "/api/clientes/" + codigoCompleto;
      String caminhoSimples = "/api/clientes/" + codigoSimples;

      assertEquals(completo, this.consultarEmUmaConsulta(api, sql, caminhoCompleto, 200));
      assertEquals(simples, this.consultarEmUmaConsulta(api, sql, caminhoSimples, 200));

      Cliente leituraAnterior = new PgClientes(banco, new PgCidades(banco)).cliente(codigoSimples);
      String telefone = "(41) 3333-4444";
      try (var conexao = banco.getConnection();
        var atualizacao = conexao.prepareStatement("UPDATE public.cliente SET email = ?, telefone = ? WHERE codigo = ?")) {
        atualizacao.setString(1, novoEmail);
        atualizacao.setString(2, telefone);
        atualizacao.setLong(3, codigoSimples);
        assertEquals(1, atualizacao.executeUpdate());
      }
      ObjectNode atualizado = ((ObjectNode) simples).deepCopy().put("email", novoEmail).put("telefone", telefone);
      assertEquals(atualizado, this.consultarEmUmaConsulta(api, sql, caminhoSimples, 200));
      assertEquals(emailSimples, leituraAnterior.contato().email().valor());
      assertTrue(leituraAnterior.contato().telefone().isEmpty());

      try (var conexao = banco.getConnection();
        var exclusao = conexao.prepareStatement("DELETE FROM public.cliente WHERE codigo = ?")) {
        exclusao.setLong(1, codigoSimples);
        assertEquals(1, exclusao.executeUpdate());
      }
      assertEquals(this.json.readTree("""
        {"title":"Registro não encontrado","status":404,
         "detail":"Cliente não encontrado.","instance":"%s"}
        """.formatted(caminhoSimples)), this.consultarEmUmaConsulta(api, sql, caminhoSimples, 404));

      JsonNode listagem = this.consultarEmUmaConsulta(api, sql, "/api/clientes", 200);
      var restantes = this.json.createArrayNode();
      for (JsonNode cliente : listagem) {
        long codigo = cliente.path("codigo").asLong();
        if (codigo == codigoCompleto || codigo == codigoSimples) {
          restantes.add(cliente);
        }
      }
      assertEquals(this.json.createArrayNode().add(completo), restantes);
    } finally {
      excluirClientes(banco, List.of(emailCompleto, emailSimples, novoEmail));
      try (var conexao = banco.getConnection();
        var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE nome = ?")) {
        exclusao.setString(1, nomeCidade);
        exclusao.executeUpdate();
      }
    }
  }

  @Test
  @Tag("integracao")
  void registroInvalidoRetorna503SemAlterarDadosPersistidos() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    Take api = AplicacaoRica.rotas(sql.banco());
    String email = "registro-invalido-" + UUID.randomUUID() + "@example.com";
    String telefoneInvalido = "telefone inválido";
    try {
      JsonNode cadastrado = this.cadastrar(api, this.cliente("Cliente existente", "12345678909", email));
      long codigo = cadastrado.path("codigo").asLong();
      String caminho = "/api/clientes/" + codigo;
      try (var conexao = banco.getConnection();
        var atualizacao = conexao.prepareStatement("UPDATE public.cliente SET telefone = ? WHERE codigo = ?")) {
        atualizacao.setString(1, telefoneInvalido);
        atualizacao.setLong(2, codigo);
        assertEquals(1, atualizacao.executeUpdate());
      }

      assertEquals(this.json.readTree("""
        {"title":"Persistência indisponível","status":503,
         "detail":"Não foi possível concluir a operação no banco de dados.","instance":"%s"}
        """.formatted(caminho)), this.consultarEmUmaConsulta(api, sql, caminho, 503));
      try (var conexao = banco.getConnection();
        var consulta = conexao.prepareStatement("SELECT telefone, email FROM public.cliente WHERE codigo = ?")) {
        consulta.setLong(1, codigo);
        try (var resultado = consulta.executeQuery()) {
          assertTrue(resultado.next());
          assertEquals(telefoneInvalido, resultado.getString("telefone"));
          assertEquals(email, resultado.getString("email"));
          assertFalse(resultado.next());
        }
      }
    } finally {
      excluirClientes(banco, List.of(email));
    }
  }

  private JsonNode consultarEmUmaConsulta(Take api, SqlContado sql, String caminho, int status) throws Exception {
    sql.zerar();
    JsonNode resposta = this.resposta(status, new RsPrint(api.act(new RqFake("GET", caminho))));
    assertEquals(1, sql.conexoes());
    assertEquals(1, sql.comandos());
    return resposta;
  }

  private ObjectNode cliente(String nome, String cpf, String email) {
    return this.json.createObjectNode().put("nome", nome).put("cpf", cpf).put("email", email);
  }

  private JsonNode cadastrar(Take api, ObjectNode pedido) throws Exception {
    byte[] corpo = this.json.writeValueAsBytes(pedido);
    return this.resposta(201, new RsPrint(api.act(new RqFake(List.of(
      "POST /api/clientes HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + corpo.length
    ), corpo))));
  }

  private JsonNode resposta(int status, RsPrint resposta) throws Exception {
    String primeiraLinha = resposta.head().iterator().next();
    assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], primeiraLinha);
    return this.json.readTree(resposta.printBody());
  }

  private JsonNode problema(Take api, String codigo, int status) throws Exception {
    String caminho = "/api/clientes/" + codigo;
    var resposta = new RsPrint(api.act(new RqFake("GET", caminho)));
    JsonNode problema = this.resposta(status, resposta);
    assertEquals(caminho, problema.path("instance").asString());
    assertEquals(status, problema.path("status").asInt());
    return problema;
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
