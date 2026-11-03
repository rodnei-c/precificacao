package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.produtos.http.TkCadastroProduto;
import br.com.modelagemdominio.rico.produtos.http.TkConsultaProduto;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
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
            var produtos = new PgProdutosConstantes(banco);
            var consulta = new TkConsultaProduto(produtos);
            var cadastro = new TkCadastroProduto(produtos);
            TkFork rotas = new TkFork(
                new FkRegex("/api/produtos/([^/]+)", (TkRegex) requisicao ->
                    new RqMethod.Base(requisicao).method().equals("GET")
                        ? consulta.act(requisicao)
                        : new RsWithHeader(new RsWithStatus(405), "Allow", "GET")),
                new FkRegex("/api/produtos", (TkRegex) requisicao ->
                    new RqMethod.Base(requisicao).method().equals("POST")
                        ? cadastro.act(requisicao)
                        : new RsWithHeader(new RsWithStatus(405), "Allow", "POST"))
            );
            System.out.printf("API do modelo rico em http://127.0.0.1:%d%n", servidor.getLocalPort());
            TkFallback api = new TkFallback(rotas, new FbChain(
                new FbStatus(404, new RsWithStatus(404)),
                new FbFixed(new RsWithStatus(500))
            ));
            new FtBasic(new BkBasic(api), servidor).start(Exit.NEVER);
        }
    }
}
