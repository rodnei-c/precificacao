package br.com.modelagemdominio.rico.infraestrutura.banco;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

/** Prepara o banco da API antes de devolver seu pool de conexões. */
public final class BancoPostgres {

  private static final Logger LOG = Logger.getLogger(BancoPostgres.class.getName());

  private final ConfiguracaoPostgres configuracao;
  private final CriacaoBancoPostgres criacao;

  /** @param ambiente variáveis de configuração da aplicação, incluindo DB_PASSWORD */
  public BancoPostgres(Map<String, String> ambiente) {
    this(new ConfiguracaoPostgres(ambiente));
  }

  BancoPostgres(String url, String usuario, String senha, boolean criarSeAusente, String manutencao) {
    this(new ConfiguracaoPostgres(url, usuario, senha, criarSeAusente, manutencao));
  }

  private BancoPostgres(ConfiguracaoPostgres configuracao) {
    this(configuracao, new CriacaoBancoPostgres(configuracao.conexoes(), configuracao.endereco()));
  }

  private BancoPostgres(ConfiguracaoPostgres configuracao, CriacaoBancoPostgres criacao) {
    this.configuracao = configuracao;
    this.criacao = criacao;
  }

  /**
   * @return pool aberto, com o banco existente e as migrações aplicadas; quem recebe deve fechá-lo
   * @throws FalhaInicializacaoBanco quando a conexão, a criação ou uma migração falhar
   */
  public HikariDataSource preparar() {
    try {
      this.criacao.garantirExistencia(this.configuracao.criarSeAusente());
    } catch (SQLException falha) {
      String mensagem = switch (String.valueOf(falha.getSQLState())) {
        case "3D000" -> this.configuracao.criarSeAusente()
          ? "O banco de manutenção não existe. Confira RICO_DB_MANUTENCAO."
          : "O banco não existe e a criação automática está desativada em RICO_DB_CRIAR_SE_AUSENTE.";
        case "42501" -> "Não foi possível preparar o banco. Para criar um banco ausente, o usuário precisa de CREATEDB.";
        case "28P01", "28000" -> "Não foi possível autenticar no PostgreSQL. Confira o usuário e DB_PASSWORD.";
        default -> "Não foi possível acessar o PostgreSQL. Confira a conexão e o banco de manutenção.";
      };
      LOG.severe("Falha na preparação do banco (SQLState: " + falha.getSQLState() + "). " + mensagem);
      throw new FalhaInicializacaoBanco(mensagem, falha);
    }
    HikariDataSource banco = this.configuracao.conexoes().agrupar(this.configuracao.endereco().url());
    try {
      Flyway.configure().dataSource(banco).locations("classpath:db/migration")
        .schemas("public").cleanDisabled(true).load().migrate();
      return banco;
    } catch (FlywayException falha) {
      banco.close();
      LOG.severe("Não foi possível aplicar as migrações; a API não será iniciada.");
      throw new FalhaInicializacaoBanco("Não foi possível aplicar as migrações do banco da API.", falha);
    } catch (RuntimeException falha) {
      banco.close();
      throw falha;
    }
  }
}
