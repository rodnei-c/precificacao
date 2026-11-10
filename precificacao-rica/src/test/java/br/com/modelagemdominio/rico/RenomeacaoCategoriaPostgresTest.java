package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.categorias.pg.PgCategorias;
import br.com.modelagemdominio.rico.testes.SqlContado;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integracao")
class RenomeacaoCategoriaPostgresTest {

  @Test
  void cadastroListaERenomeacaoConservamOsValoresDeCadaLeitura() throws Exception {
    var banco = bancoDeTeste();
    var sql = new SqlContado(banco);
    var categorias = new PgCategorias(sql.banco());
    String sufixo = UUID.randomUUID().toString();
    String original = "Categoria original " + sufixo;
    String primeiroNome = "Categoria primeira " + sufixo;
    String segundoNome = "Categoria segunda " + sufixo;
    try {
      Categoria cadastrada = categorias.cadastrar(new NomeCategoria(original));
      long codigo = cadastrada.codigo();
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());

      sql.zerar();
      Categoria listada = categorias.listar().stream()
        .filter(categoria -> categoria.codigo() == codigo).findFirst().orElseThrow();
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());

      sql.zerar();
      Categoria renomeada = categorias.renomear(new CodigoCategoria(codigo), new NomeCategoria(primeiroNome));
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());

      sql.zerar();
      Categoria atual = categorias.renomear(new CodigoCategoria(codigo), new NomeCategoria(segundoNome));
      for (var leitura : List.of(Map.entry(cadastrada, original), Map.entry(listada, original),
        Map.entry(renomeada, primeiroNome), Map.entry(atual, segundoNome))) {
        assertEquals(codigo, leitura.getKey().codigo());
        assertEquals(new NomeCategoria(leitura.getValue()), leitura.getKey().nome());
      }
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());

      sql.zerar();
      Categoria novaLeitura = categorias.listar().stream()
        .filter(categoria -> categoria.codigo() == codigo).findFirst().orElseThrow();
      assertEquals(codigo, novaLeitura.codigo());
      assertEquals(new NomeCategoria(segundoNome), novaLeitura.nome());
      assertEquals(1, sql.conexoes());
      assertEquals(1, sql.comandos());
    } finally {
      try (var conexao = banco.getConnection(); var excluir = conexao.prepareStatement(
        "DELETE FROM public.categoria WHERE nome IN (?, ?, ?)")) {
        excluir.setString(1, original);
        excluir.setString(2, primeiroNome);
        excluir.setString(3, segundoNome);
        excluir.executeUpdate();
      }
    }
  }

  @Test
  void falhaDuranteAtualizacaoNaoViraNomeDuplicadoENaoAlteraCadastro() throws Exception {
    var banco = bancoDeTeste();
    var categorias = new PgCategorias(banco);
    Take api = AplicacaoRica.rotas(banco);
    var json = new JsonMapper();
    String sufixo = UUID.randomUUID().toString().replace("-", "");
    String funcao = "falha_renomeacao_" + sufixo;
    String gatilho = "gatilho_renomeacao_" + sufixo;
    String nomeOriginal = "Categoria preservada " + sufixo;
    String novoNome = "Categoria renomeada " + sufixo;
    try (var conexao = banco.getConnection(); var comando = conexao.createStatement()) {
      try {
        Categoria categoria = categorias.cadastrar(new NomeCategoria(nomeOriginal));
        List<Categoria> antes = categorias.listar();
        String caminho = "/api/categorias/" + categoria.codigo();
        comando.execute("""
          CREATE FUNCTION public.%s() RETURNS trigger LANGUAGE plpgsql AS $teste$
          BEGIN
            IF NEW.codigo = TG_ARGV[0]::BIGINT THEN
              RAISE EXCEPTION USING ERRCODE = '23505',
                MESSAGE = 'Falha intencional na renomeacao', CONSTRAINT = 'outra_restricao';
            END IF;
            RETURN NEW;
          END;
          $teste$
          """.formatted(funcao));
        comando.execute("CREATE TRIGGER " + gatilho + " AFTER UPDATE ON public.categoria"
          + " FOR EACH ROW EXECUTE FUNCTION public." + funcao + "('" + categoria.codigo() + "')");

        RsPrint falha = renomear(api, caminho, novoNome);
        assertStatus(503, falha);
        assertEquals(json.readTree("""
          {"title":"Persistência indisponível","status":503,
           "detail":"Não foi possível concluir a operação no banco de dados.","instance":"%s"}
          """.formatted(caminho)), json.readTree(falha.printBody()));
        assertEquals(antes, categorias.listar());

        comando.execute("DROP FUNCTION public." + funcao + "() CASCADE");
        RsPrint repeticao = renomear(api, caminho, novoNome);
        assertStatus(200, repeticao);
        assertEquals(json.readTree("""
          {"codigo":%d,"nome":"%s"}
          """.formatted(categoria.codigo(), novoNome)), json.readTree(repeticao.printBody()));
        assertEquals(new NomeCategoria(novoNome), categorias.listar().stream()
          .filter(atual -> atual.codigo() == categoria.codigo()).findFirst().orElseThrow().nome());
      } finally {
        try {
          comando.execute("DROP FUNCTION IF EXISTS public." + funcao + "() CASCADE");
        } finally {
          try (var exclusao = conexao.prepareStatement("DELETE FROM public.categoria WHERE nome IN (?, ?)")) {
            exclusao.setString(1, nomeOriginal);
            exclusao.setString(2, novoNome);
            exclusao.executeUpdate();
          }
        }
      }
    }
  }

  private static RsPrint renomear(Take api, String caminho, String nome) throws Exception {
    byte[] corpo = new JsonMapper().writeValueAsBytes(Map.of("nome", nome));
    return new RsPrint(api.act(new RqFake(List.of(
      "PATCH " + caminho + " HTTP/1.1",
      "Content-Type: application/json",
      "Content-Length: " + corpo.length
    ), corpo)));
  }

  private static void assertStatus(int esperado, RsPrint resposta) throws Exception {
    String primeiraLinha = resposta.head().iterator().next();
    assertEquals(Integer.toString(esperado), primeiraLinha.split(" ", 3)[1], primeiraLinha);
  }

  private static PGSimpleDataSource bancoDeTeste() {
    var banco = new PGSimpleDataSource();
    banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
    banco.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
    banco.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
    return banco;
  }
}
