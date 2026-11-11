package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.produtos.Dinheiro;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutos;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class ListaProdutosTest {

  private final JsonMapper json = new JsonMapper();

  @Test
  void ordenaMaterializaEmUmaConsultaEAtualizaNaProximaLeitura() throws Exception {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    String sufixo = UUID.randomUUID().toString();
    String primeiroNome = "Zinco d'água %_ " + sufixo;
    String segundoNome = "Arroz " + sufixo;
    try (var conexao = banco.getConnection()) {
      long categoria;
      try (var cadastro = conexao.prepareStatement("INSERT INTO public.categoria (nome) VALUES (?) RETURNING codigo")) {
        cadastro.setString(1, "Categoria leitura " + sufixo);
        try (var resultado = cadastro.executeQuery()) {
          assertTrue(resultado.next());
          categoria = resultado.getLong(1);
        }
      }
      try {
        long primeiro = reservarCodigo(conexao);
        long segundo = reservarCodigo(conexao);
        inserir(conexao, segundo, segundoNome, categoria);
        inserir(conexao, primeiro, primeiroNome, null);
        var contagem = new SqlContado(banco);
        var fonte = contagem.banco();
        Take api = new RotasDaAplicacaoRica(fonte).rotas();

        JsonNode resposta = this.consultar(api, "nome=" + sufixo);
        assertEquals(this.json.readTree("""
          [
            {"codigo":%d,"nome":"%s","custo":100.00,"precoVenda":120.00,"percentual":20.0000,
              "modalidadePrecificacao":"ACRESCIMO_SOBRE_CUSTO"},
            {"codigo":%d,"nome":"%s","custo":100.00,"precoVenda":120.00,"percentual":20.0000,
              "modalidadePrecificacao":"ACRESCIMO_SOBRE_CUSTO","codigoCategoria":%d}
          ]
          """.formatted(primeiro, primeiroNome, segundo, segundoNome, categoria)), resposta);
        assertEquals(1, contagem.comandos(), "A serialização não deve consultar cada atributo ou categoria.");
        assertEquals(1, contagem.conexoes());

        var filtro = new FiltrosProdutos(Optional.of(sufixo), Optional.empty(), Optional.empty());
        contagem.zerar();
        var materializado = new PgProdutosConstantes(fonte).listar(filtro).get(0);
        var sobDemanda = new PgProdutos(fonte).listar(filtro).get(0);
        assertEquals(primeiro, materializado.codigo());
        assertEquals(primeiro, sobDemanda.codigo());
        assertEquals(2, contagem.comandos());

        try (var atualizacao = conexao.prepareStatement(
          "UPDATE public.produto SET preco_venda = 130, percentual = 30 WHERE codigo = ?"
        )) {
          atualizacao.setLong(1, primeiro);
          assertEquals(1, atualizacao.executeUpdate());
        }
        contagem.zerar();
        assertEquals(new Dinheiro(120), materializado.precificacao().precoVenda());
        assertEquals(0, contagem.comandos());
        assertEquals(new Dinheiro(130), sobDemanda.precificacao().precoVenda());
        assertEquals(1, contagem.comandos());

        contagem.zerar();
        JsonNode atualizada = this.consultar(api, "nome=" + sufixo);
        assertEquals(0, new BigDecimal("130.00").compareTo(atualizada.get(0).path("precoVenda").decimalValue()));
        assertEquals(0, new BigDecimal("30.0000").compareTo(atualizada.get(0).path("percentual").decimalValue()));
        assertEquals(resposta.get(1), atualizada.get(1));
        assertEquals(1, contagem.comandos());

        contagem.zerar();
        assertEquals(this.json.createArrayNode(), this.consultar(api, "nome=ausente-" + sufixo));
        assertEquals(1, contagem.comandos());
      } finally {
        try (var produtos = conexao.prepareStatement("DELETE FROM public.produto WHERE nome IN (?, ?)");
             var categorias = conexao.prepareStatement("DELETE FROM public.categoria WHERE codigo = ?")) {
          produtos.setString(1, primeiroNome);
          produtos.setString(2, segundoNome);
          produtos.executeUpdate();
          categorias.setLong(1, categoria);
          categorias.executeUpdate();
        }
      }
    }
  }

  private JsonNode consultar(Take api, String filtro) throws Exception {
    var resposta = new RsPrint(api.act(new RqFake("GET", "/api/produtos?" + filtro)));
    assertEquals("200", resposta.head().iterator().next().split(" ", 3)[1]);
    return this.json.readTree(resposta.printBody());
  }

  private static long reservarCodigo(Connection conexao) throws SQLException {
    try (var comando = conexao.createStatement();
         var resultado = comando.executeQuery("SELECT nextval(pg_get_serial_sequence('public.produto', 'codigo'))")) {
      resultado.next();
      return resultado.getLong(1);
    }
  }

  private static void inserir(Connection conexao, long codigo, String nome, Long categoria) throws SQLException {
    try (var cadastro = conexao.prepareStatement("""
      INSERT INTO public.produto
        (codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo)
      VALUES (?, ?, 100, 120, 20, 'ACRESCIMO_SOBRE_CUSTO', ?)
      """)) {
      cadastro.setLong(1, codigo);
      cadastro.setString(2, nome);
      cadastro.setObject(3, categoria, Types.BIGINT);
      cadastro.executeUpdate();
    }
  }
}
