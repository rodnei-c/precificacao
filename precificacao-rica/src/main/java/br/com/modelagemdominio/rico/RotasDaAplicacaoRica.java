package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.RotaPorMetodo.Metodo;
import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.http.TkCadastroCategoria;
import br.com.modelagemdominio.rico.categorias.http.TkListaCategorias;
import br.com.modelagemdominio.rico.categorias.http.TkRenomeacaoCategoria;
import br.com.modelagemdominio.rico.categorias.pg.PgCategorias;
import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.http.TkCadastroCidade;
import br.com.modelagemdominio.rico.cidades.http.TkListaCidades;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.http.TkAlteracaoCliente;
import br.com.modelagemdominio.rico.clientes.http.TkCadastroCliente;
import br.com.modelagemdominio.rico.clientes.http.TkConsultaCliente;
import br.com.modelagemdominio.rico.clientes.http.TkListaClientes;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.faturas.http.TkCadastroFatura;
import br.com.modelagemdominio.rico.faturas.http.TkConsultaFatura;
import br.com.modelagemdominio.rico.faturas.http.TkListaFaturas;
import br.com.modelagemdominio.rico.faturas.http.TkSimulacaoFatura;
import br.com.modelagemdominio.rico.faturas.pg.PgFaturas;
import br.com.modelagemdominio.rico.produtos.HistoricoDePrecificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.http.TkAlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.http.TkCadastroProduto;
import br.com.modelagemdominio.rico.produtos.http.TkConsultaProduto;
import br.com.modelagemdominio.rico.produtos.http.TkHistoricoPrecificacao;
import br.com.modelagemdominio.rico.produtos.http.TkListaProdutos;
import br.com.modelagemdominio.rico.produtos.http.TkVinculoCategoria;
import br.com.modelagemdominio.rico.produtos.pg.PgHistoricoDePrecificacao;
import br.com.modelagemdominio.rico.produtos.pg.PgProduto;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutos;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import java.util.function.LongFunction;
import javax.sql.DataSource;
import org.takes.facets.fork.FkChain;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.Fork;
import org.takes.facets.fork.TkFork;

/** Compõe as dependências e rotas HTTP sem abrir conexões. */
final class RotasDaAplicacaoRica {

  private final DataSource banco;

  RotasDaAplicacaoRica(DataSource banco) {
    this.banco = banco;
  }

  TkFork rotas() {
    var categorias = new PgCategorias(this.banco);
    var cidades = new PgCidades(this.banco);
    var clientes = new PgClientes(this.banco, cidades);
    LongFunction<Produto> produtoPorCodigo = codigo -> new PgProduto(this.banco, codigo);
    var origem = new PgProdutos(this.banco, produtoPorCodigo, categorias);
    var produtos = new PgProdutosConstantes(this.banco, produtoPorCodigo, origem);
    var historico = new PgHistoricoDePrecificacao(this.banco);
    var faturas = new PgFaturas(this.banco, produtos, clientes);
    return new TkFork(this.faturas(faturas, produtos), this.produtos(produtos, historico),
      this.categorias(categorias), this.cidades(cidades), this.clientes(clientes));
  }

  private Fork faturas(Faturas faturas, Produtos produtos) {
    return new FkChain(
      new FkRegex("/api/faturas/simulacao", new RotaPorMetodo(
        new Metodo("POST", new TkSimulacaoFatura(faturas, produtos)))),
      new FkRegex("/api/faturas/([^/]+)", new RotaPorMetodo(
        new Metodo("GET", new TkConsultaFatura(faturas)))),
      new FkRegex("/api/faturas", new RotaPorMetodo(
        new Metodo("GET", new TkListaFaturas(faturas)), new Metodo("POST", new TkCadastroFatura(faturas, produtos))))
    );
  }

  private Fork produtos(Produtos produtos, HistoricoDePrecificacao historico) {
    return new FkChain(
      new FkRegex("/api/produtos/([^/]+)/historico-precificacao", new RotaPorMetodo(
        new Metodo("GET", new TkHistoricoPrecificacao(historico)))),
      new FkRegex("/api/produtos/([^/]+)/precificacao", new RotaPorMetodo(
        new Metodo("PATCH", new TkAlteracaoPrecificacao(produtos)))),
      new FkRegex("/api/produtos/([^/]+)/categoria", new RotaPorMetodo(
        new Metodo("PATCH", new TkVinculoCategoria(produtos)))),
      new FkRegex("/api/produtos/([^/]+)", new RotaPorMetodo(
        new Metodo("GET", new TkConsultaProduto(produtos)))),
      new FkRegex("/api/produtos", new RotaPorMetodo(
        new Metodo("GET", new TkListaProdutos(produtos)), new Metodo("POST", new TkCadastroProduto(produtos))))
    );
  }

  private Fork categorias(Categorias categorias) {
    return new FkChain(
      new FkRegex("/api/categorias/([^/]+)", new RotaPorMetodo(
        new Metodo("PATCH", new TkRenomeacaoCategoria(categorias)))),
      new FkRegex("/api/categorias", new RotaPorMetodo(
        new Metodo("GET", new TkListaCategorias(categorias)), new Metodo("POST", new TkCadastroCategoria(categorias))))
    );
  }

  private Fork cidades(Cidades cidades) {
    return new FkRegex("/api/cidades", new RotaPorMetodo(
      new Metodo("GET", new TkListaCidades(cidades)), new Metodo("POST", new TkCadastroCidade(cidades))));
  }

  private Fork clientes(Clientes clientes) {
    return new FkChain(
      new FkRegex("/api/clientes/([^/]+)", new RotaPorMetodo(
        new Metodo("GET", new TkConsultaCliente(clientes)), new Metodo("PATCH", new TkAlteracaoCliente(clientes)))),
      new FkRegex("/api/clientes", new RotaPorMetodo(
        new Metodo("GET", new TkListaClientes(clientes)), new Metodo("POST", new TkCadastroCliente(clientes))))
    );
  }
}
