package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroProdutosUtf8Test {

  @Test
  void decodificaUtf8MesmoEmJvmWindows1252() throws Exception {
    String executavel = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    var java = Path.of(System.getProperty("java.home"), "bin", executavel);
    String classes = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    var processo = new ProcessBuilder(java.toString(), "-Dfile.encoding=windows-1252", "-cp", classes,
      ProvaWindows1252.class.getName()).redirectErrorStream(true).start();
    boolean concluiu = processo.waitFor(20, TimeUnit.SECONDS);
    if (!concluiu) {
      processo.destroyForcibly();
      processo.waitFor(5, TimeUnit.SECONDS);
    }
    assertTrue(concluiu, "A verificação da codificação deve encerrar no prazo.");
    String saida = new String(processo.getInputStream().readAllBytes(), Charset.forName("windows-1252"));
    assertEquals(0, processo.exitValue(), saida);
  }

  /** Executada em outro processo para controlar a codificação padrão de fato. */
  public static final class ProvaWindows1252 {

    public static void main(String[] argumentos) throws Exception {
      assertEquals(Charset.forName("windows-1252"), Charset.defaultCharset());
      var recebidos = new AtomicReference<FiltrosProdutos>();
      var api = new TkFork(new FkRegex("/api/produtos", new TkListaProdutos(new ProdutosObservados(recebidos))));
      var resposta = new RsPrint(api.act(new RqFake("GET", "/api/produtos?"
        + "%6Eome=Caf%C3%A9%20%2B%20%C3%81gua%20%25_%20%26%20%3D"
        + "&modalidadePrecificacao=ACRESCIMO_SOBRE_CUSTO&categoria=+7+&categoria=invalida")));
      assertEquals("200", resposta.head().iterator().next().split(" ", 3)[1]);
      assertEquals(Optional.of("Café + Água %_ & ="), recebidos.get().nome());
      assertEquals(Optional.of(ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO), recebidos.get().modalidade());
      assertEquals(Optional.of(new CodigoCategoria(7)), recebidos.get().categoria());

      var filtros = recebidos.get();
      var invalida = new RsPrint(api.act(new RqFake("GET", "/api/produtos?categoria=++&categoria=7")));
      assertEquals("400", invalida.head().iterator().next().split(" ", 3)[1]);
      assertSame(filtros, recebidos.get(), "A listagem não deve executar com o primeiro filtro inválido.");
    }
  }

  private record ProdutosObservados(AtomicReference<FiltrosProdutos> recebidos) implements Produtos {

    @Override
    public List<Produto> listar(FiltrosProdutos filtros) {
      this.recebidos.set(filtros);
      return List.of();
    }

    @Override
    public Produto produto(long codigo) {
      throw new AssertionError("Ler filtros não deve consultar um produto individual.");
    }

    @Override
    public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao, Optional<CodigoCategoria> categoria) {
      throw new AssertionError("Ler filtros não deve cadastrar produtos.");
    }

    @Override
    public Produto vincularCategoria(long codigo, CodigoCategoria categoria) {
      throw new AssertionError("Ler filtros não deve alterar categoria.");
    }

    @Override
    public Produto alterarPrecificacao(long codigo, ValoresParaPrecificar valores) {
      throw new AssertionError("Ler filtros não deve alterar preços.");
    }
  }
}
