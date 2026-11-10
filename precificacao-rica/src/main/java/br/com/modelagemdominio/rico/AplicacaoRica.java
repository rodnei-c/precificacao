package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.categorias.http.TkCadastroCategoria;
import br.com.modelagemdominio.rico.categorias.http.TkListaCategorias;
import br.com.modelagemdominio.rico.categorias.http.TkRenomeacaoCategoria;
import br.com.modelagemdominio.rico.categorias.pg.PgCategorias;
import br.com.modelagemdominio.rico.cidades.http.TkCadastroCidade;
import br.com.modelagemdominio.rico.cidades.http.TkListaCidades;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.http.TkAlteracaoCliente;
import br.com.modelagemdominio.rico.clientes.http.TkCadastroCliente;
import br.com.modelagemdominio.rico.clientes.http.TkConsultaCliente;
import br.com.modelagemdominio.rico.clientes.http.TkListaClientes;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.produtos.http.TkAlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.http.TkCadastroProduto;
import br.com.modelagemdominio.rico.produtos.http.TkConsultaProduto;
import br.com.modelagemdominio.rico.produtos.http.TkHistoricoPrecificacao;
import br.com.modelagemdominio.rico.produtos.http.TkListaProdutos;
import br.com.modelagemdominio.rico.produtos.http.TkVinculoCategoria;
import br.com.modelagemdominio.rico.produtos.pg.PgHistoricoDePrecificacao;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.sql.DataSource;
import org.takes.facets.fallback.FbChain;
import org.takes.facets.fallback.FbFixed;
import org.takes.facets.fallback.FbStatus;
import org.takes.facets.fallback.TkFallback;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.facets.fork.TkRegex;
import org.takes.rq.RqMethod;
import org.takes.http.BkBasic;
import org.takes.http.Exit;
import org.takes.http.FtBasic;
import org.takes.rs.RsWithHeader;
import org.takes.rs.RsWithStatus;

public final class AplicacaoRica {

    private AplicacaoRica() {
    }

    public static void main(String[] args) throws IOException {
        var ambiente = System.getenv();
        String senha = ambiente.get("DB_PASSWORD");
        if (senha == null) {
            throw new IllegalStateException("Configure DB_PASSWORD antes de iniciar a API.");
        }
        HikariConfig conexoes = new HikariConfig();
        conexoes.setJdbcUrl(ambiente.getOrDefault(
            "RICO_DB_URL", "jdbc:postgresql://localhost:5432/precificacao_rica"
        ));
        conexoes.setUsername(ambiente.getOrDefault("RICO_DB_USER", "postgres"));
        conexoes.setPassword(senha);
        conexoes.setConnectionTimeout(5000);
        conexoes.setInitializationFailTimeout(-1);

        int porta = Integer.parseInt(ambiente.getOrDefault("RICO_SERVER_PORT", "8081"));
        try (HikariDataSource banco = new HikariDataSource(conexoes);
             ServerSocket servidor = new ServerSocket(porta, 50, InetAddress.getByName("127.0.0.1"))) {
            TkFork rotas = rotas(banco);
            System.out.printf("API do modelo rico em http://127.0.0.1:%d%n", servidor.getLocalPort());
            TkFallback api = new TkFallback(rotas, new FbChain(
                new FbStatus(404, new RsWithStatus(404)),
                new FbFixed(new RsWithStatus(500))
            ));
            new FtBasic(new BkBasic(api), servidor).start(Exit.NEVER);
        }
    }

    static TkFork rotas(DataSource banco) {
        var produtos = new PgProdutosConstantes(banco);
        var consultaProduto = new TkConsultaProduto(produtos);
        var listaProdutos = new TkListaProdutos(produtos);
        var cadastroProduto = new TkCadastroProduto(produtos);
        var vinculoCategoria = new TkVinculoCategoria(produtos);
        var alteracaoPrecificacao = new TkAlteracaoPrecificacao(produtos);
        var historicoPrecificacao = new TkHistoricoPrecificacao(new PgHistoricoDePrecificacao(banco));
        var categorias = new PgCategorias(banco);
        var cadastroCategoria = new TkCadastroCategoria(categorias);
        var listaCategorias = new TkListaCategorias(categorias);
        var renomeacaoCategoria = new TkRenomeacaoCategoria(categorias);
        var cidades = new PgCidades(banco);
        var cadastroCidade = new TkCadastroCidade(cidades);
        var listaCidades = new TkListaCidades(cidades);
        var clientes = new PgClientes(banco, cidades);
        var cadastroCliente = new TkCadastroCliente(clientes);
        var listaClientes = new TkListaClientes(clientes);
        var consultaCliente = new TkConsultaCliente(clientes);
        var alteracaoCliente = new TkAlteracaoCliente(clientes);
        return new TkFork(
            new FkRegex("/api/produtos/([^/]+)/historico-precificacao", (TkRegex) requisicao ->
                new RqMethod.Base(requisicao).method().equals("GET")
                    ? historicoPrecificacao.act(requisicao)
                    : new RsWithHeader(new RsWithStatus(405), "Allow", "GET")),
            new FkRegex("/api/produtos/([^/]+)/precificacao", (TkRegex) requisicao ->
                new RqMethod.Base(requisicao).method().equals("PATCH")
                    ? alteracaoPrecificacao.act(requisicao)
                    : new RsWithHeader(new RsWithStatus(405), "Allow", "PATCH")),
            new FkRegex("/api/produtos/([^/]+)/categoria", (TkRegex) requisicao ->
                new RqMethod.Base(requisicao).method().equals("PATCH")
                    ? vinculoCategoria.act(requisicao)
                    : new RsWithHeader(new RsWithStatus(405), "Allow", "PATCH")),
            new FkRegex("/api/produtos/([^/]+)", (TkRegex) requisicao ->
                new RqMethod.Base(requisicao).method().equals("GET")
                    ? consultaProduto.act(requisicao)
                    : new RsWithHeader(new RsWithStatus(405), "Allow", "GET")),
            new FkRegex("/api/produtos", (TkRegex) requisicao ->
                switch (new RqMethod.Base(requisicao).method()) {
                    case "GET" -> listaProdutos.act(requisicao);
                    case "POST" -> cadastroProduto.act(requisicao);
                    default -> new RsWithHeader(new RsWithStatus(405), "Allow", "GET, POST");
                }),
            new FkRegex("/api/categorias/([^/]+)", (TkRegex) requisicao ->
                new RqMethod.Base(requisicao).method().equals("PATCH")
                    ? renomeacaoCategoria.act(requisicao)
                    : new RsWithHeader(new RsWithStatus(405), "Allow", "PATCH")),
            new FkRegex("/api/cidades", (TkRegex) requisicao ->
                switch (new RqMethod.Base(requisicao).method()) {
                    case "GET" -> listaCidades.act(requisicao);
                    case "POST" -> cadastroCidade.act(requisicao);
                    default -> new RsWithHeader(new RsWithStatus(405), "Allow", "GET, POST");
                }),
            new FkRegex("/api/clientes/([^/]+)", (TkRegex) requisicao ->
                switch (new RqMethod.Base(requisicao).method()) {
                    case "GET" -> consultaCliente.act(requisicao);
                    case "PATCH" -> alteracaoCliente.act(requisicao);
                    default -> new RsWithHeader(new RsWithStatus(405), "Allow", "GET, PATCH");
                }),
            new FkRegex("/api/clientes", (TkRegex) requisicao ->
                switch (new RqMethod.Base(requisicao).method()) {
                    case "GET" -> listaClientes.act(requisicao);
                    case "POST" -> cadastroCliente.act(requisicao);
                    default -> new RsWithHeader(new RsWithStatus(405), "Allow", "GET, POST");
                }),
            new FkRegex("/api/categorias", (TkRegex) requisicao ->
                switch (new RqMethod.Base(requisicao).method()) {
                    case "GET" -> listaCategorias.act(requisicao);
                    case "POST" -> cadastroCategoria.act(requisicao);
                    default -> new RsWithHeader(new RsWithStatus(405), "Allow", "GET, POST");
                })
        );
    }
}
