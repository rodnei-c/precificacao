package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.exceptions.InicializacaoBancoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cria somente o banco ausente, usando a mesma conexão configurada para as migrações. */
public class CriadorBancoPostgres {

  private static final Logger LOG = LoggerFactory.getLogger(CriadorBancoPostgres.class);

  private final EnderecoBancoPostgres endereco;
  private final Properties credenciais;

  /**
   * @param url conexão do banco da aplicação
   * @param usuario usuário que também será o proprietário do novo banco
   * @param senha senha da conexão
   * @param manutencao banco existente usado para criar o banco ausente
   */
  public CriadorBancoPostgres(String url, String usuario, String senha, String manutencao) {
    this.endereco = EnderecoBancoPostgres.daUrl(url, manutencao);
    this.credenciais = new Properties();
    this.credenciais.setProperty("user", usuario);
    this.credenciais.setProperty("password", senha);
    this.credenciais.setProperty("connectTimeout", "5");
    this.credenciais.setProperty("loginTimeout", "5");
  }

  /**
   * Mantém bancos existentes e só tenta criar após o erro PostgreSQL de banco inexistente.
   * Falhas de autenticação ou indisponibilidade não iniciam uma tentativa de criação.
   *
   * @throws InicializacaoBancoException quando a conexão ou a criação falhar
   */
  public void garantirExistencia() {
    try (Connection conexao = DriverManager.getConnection(this.endereco.url(), this.credenciais)) {
      return;
    } catch (SQLException falha) {
      if (!"3D000".equals(falha.getSQLState())) {
        throw erro("Não foi possível acessar o PostgreSQL. Verifique a conexão e as credenciais.", falha);
      }
    }

    try (Connection conexao = DriverManager.getConnection(this.endereco.urlManutencao(), this.credenciais)) {
      if (existe(conexao)) {
        return;
      }
      try (Statement comando = conexao.createStatement()) {
        comando.setQueryTimeout(30);
        comando.executeUpdate("CREATE DATABASE " + this.endereco.identificadorSql());
        LOG.info("Banco da aplicação criado; iniciando as migrações.");
      } catch (SQLException falha) {
        if (("42P04".equals(falha.getSQLState()) || "23505".equals(falha.getSQLState())) && existe(conexao)) {
          LOG.info("Banco criado por outra inicialização; continuando as migrações.");
          return;
        }
        throw falha;
      }
    } catch (SQLException falha) {
      String mensagem = "42501".equals(falha.getSQLState())
        ? "O banco não existe e o usuário configurado precisa da permissão CREATEDB para criá-lo."
        : "Não foi possível criar o banco. Verifique a conexão com o banco de manutenção e as permissões.";
      throw erro(mensagem, falha);
    }
  }

  private boolean existe(Connection conexao) throws SQLException {
    try (PreparedStatement consulta = conexao.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
      consulta.setString(1, this.endereco.nome());
      consulta.setQueryTimeout(5);
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }

  private InicializacaoBancoException erro(String mensagem, SQLException falha) {
    LOG.error("Falha na inicialização do banco (SQLState: {}). {}", falha.getSQLState(), mensagem);
    return new InicializacaoBancoException(mensagem, falha);
  }
}
