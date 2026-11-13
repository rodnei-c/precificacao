package br.com.modelagemdominio.rico.infraestrutura.banco;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Mantém credenciais privadas; abre conexões somente por operações explícitas. */
final class ConexoesPostgres {

  private final String usuario;
  private final String senha;

  ConexoesPostgres(String usuario, String senha) {
    if (usuario == null || usuario.isBlank() || senha == null) {
      throw new FalhaInicializacaoBanco("Configure RICO_DB_USER e DB_PASSWORD antes de iniciar a API.");
    }
    this.usuario = usuario;
    this.senha = senha;
  }

  Connection abrir(String url) throws SQLException {
    var propriedades = new Properties();
    propriedades.setProperty("user", this.usuario);
    propriedades.setProperty("password", this.senha);
    propriedades.setProperty("connectTimeout", "5");
    propriedades.setProperty("loginTimeout", "5");
    return DriverManager.getConnection(url, propriedades);
  }

  HikariDataSource agrupar(String url) {
    var configuracao = new HikariConfig();
    configuracao.setJdbcUrl(url);
    configuracao.setUsername(this.usuario);
    configuracao.setPassword(this.senha);
    configuracao.setConnectionTimeout(5000);
    configuracao.setInitializationFailTimeout(-1);
    configuracao.addDataSourceProperty("connectTimeout", "5");
    configuracao.addDataSourceProperty("loginTimeout", "5");
    return new HikariDataSource(configuracao);
  }
}
