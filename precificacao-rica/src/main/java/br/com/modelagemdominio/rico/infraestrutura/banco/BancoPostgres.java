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

  private final ConexoesPostgres conexoes;
  private final EnderecoPostgres endereco;
  private final boolean criarSeAusente;

  /** @param ambiente variáveis de configuração da aplicação, incluindo DB_PASSWORD */
  public BancoPostgres(Map<String, String> ambiente) {
    this(
      ambiente.getOrDefault("RICO_DB_URL", "jdbc:postgresql://localhost:5432/precificacao_rica"),
      ambiente.getOrDefault("RICO_DB_USER", "postgres"), ambiente.get("DB_PASSWORD"),
      opcaoCriacao(ambiente.getOrDefault("RICO_DB_CRIAR_SE_AUSENTE", "true")),
      ambiente.getOrDefault("RICO_DB_MANUTENCAO", "postgres")
    );
  }

  BancoPostgres(String url, String usuario, String senha, boolean criarSeAusente, String manutencao) {
    this(new ConexoesPostgres(usuario, senha), EnderecoPostgres.daUrl(url, manutencao), criarSeAusente);
  }

  private BancoPostgres(ConexoesPostgres conexoes, EnderecoPostgres endereco, boolean criarSeAusente) {
    this.conexoes = conexoes;
    this.endereco = endereco;
    this.criarSeAusente = criarSeAusente;
  }

  /**
   * @return pool aberto, com o banco existente e as migrações aplicadas; quem recebe deve fechá-lo
   * @throws FalhaInicializacaoBanco quando a conexão, a criação ou uma migração falhar
   */
  public HikariDataSource preparar() {
    try {
      if (this.criarSeAusente) {
        new CriacaoBancoPostgres(this.conexoes, this.endereco).garantirExistencia();
      } else {
        try (var conexao = this.conexoes.abrir(this.endereco.url())) {
          // Verifica o banco sem tentar criá-lo.
        }
      }
    } catch (SQLException falha) {
      String mensagem = switch (String.valueOf(falha.getSQLState())) {
        case "3D000" -> this.criarSeAusente
          ? "O banco de manutenção não existe. Confira RICO_DB_MANUTENCAO."
          : "O banco não existe e a criação automática está desativada em RICO_DB_CRIAR_SE_AUSENTE.";
        case "42501" -> "Não foi possível preparar o banco. Para criar um banco ausente, o usuário precisa de CREATEDB.";
        case "28P01", "28000" -> "Não foi possível autenticar no PostgreSQL. Confira o usuário e DB_PASSWORD.";
        default -> "Não foi possível acessar o PostgreSQL. Confira a conexão e o banco de manutenção.";
      };
      LOG.severe("Falha na preparação do banco (SQLState: " + falha.getSQLState() + "). " + mensagem);
      throw new FalhaInicializacaoBanco(mensagem, falha);
    }
    HikariDataSource banco = this.conexoes.agrupar(this.endereco.url());
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

  private static boolean opcaoCriacao(String valor) {
    if (valor != null && ("true".equalsIgnoreCase(valor.strip()) || "false".equalsIgnoreCase(valor.strip()))) {
      return Boolean.parseBoolean(valor.strip());
    }
    throw new FalhaInicializacaoBanco("Defina RICO_DB_CRIAR_SE_AUSENTE como true ou false.");
  }
}
