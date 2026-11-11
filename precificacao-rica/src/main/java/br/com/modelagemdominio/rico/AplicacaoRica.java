package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.infraestrutura.banco.BancoPostgres;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.takes.facets.fallback.FbChain;
import org.takes.facets.fallback.FbFixed;
import org.takes.facets.fallback.FbStatus;
import org.takes.facets.fallback.TkFallback;
import org.takes.http.BkBasic;
import org.takes.http.Exit;
import org.takes.http.FtBasic;
import org.takes.rs.RsWithStatus;

public final class AplicacaoRica {

  private AplicacaoRica() {
  }

  public static void main(String[] args) throws IOException {
    var ambiente = System.getenv();
    int porta = Integer.parseInt(ambiente.getOrDefault("RICO_SERVER_PORT", "8081"));
    try (HikariDataSource banco = new BancoPostgres(ambiente).preparar();
      ServerSocket servidor = new ServerSocket(porta, 50, InetAddress.getByName("127.0.0.1"))) {
      var rotas = new RotasDaAplicacaoRica(banco).rotas();
      System.out.printf("API do modelo rico em http://127.0.0.1:%d%n", servidor.getLocalPort());
      TkFallback api = new TkFallback(rotas, new FbChain(
        new FbStatus(404, new RsWithStatus(404)),
        new FbFixed(new RsWithStatus(500))
      ));
      new FtBasic(new BkBasic(api), servidor).start(Exit.NEVER);
    }
  }
}
