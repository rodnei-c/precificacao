package br.com.modelagemdominio.rico.infraestrutura.banco;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/** Cria o banco somente após a falha específica de banco inexistente. */
final class CriacaoBancoPostgres {

  private static final Logger LOG = Logger.getLogger(CriacaoBancoPostgres.class.getName());

  private final ConexoesPostgres conexoes;
  private final EnderecoPostgres endereco;

  CriacaoBancoPostgres(ConexoesPostgres conexoes, EnderecoPostgres endereco) {
    this.conexoes = conexoes;
    this.endereco = endereco;
  }

  void garantirExistencia() throws SQLException {
    try (var conexao = this.conexoes.abrir(this.endereco.url())) {
      return;
    } catch (SQLException falha) {
      if (!"3D000".equals(falha.getSQLState())) {
        throw falha;
      }
    }
    try (var conexao = this.conexoes.abrir(this.endereco.manutencao())) {
      if (this.existe(conexao)) {
        return;
      }
      try (var comando = conexao.createStatement()) {
        comando.setQueryTimeout(30);
        comando.executeUpdate("CREATE DATABASE " + this.endereco.identificadorSql());
        LOG.info("Banco da aplicação criado; iniciando as migrações.");
      } catch (SQLException falha) {
        if (("42P04".equals(falha.getSQLState()) || "23505".equals(falha.getSQLState())) && this.existe(conexao)) {
          LOG.info("Banco criado por outra inicialização; continuando as migrações.");
          return;
        }
        throw falha;
      }
    }
  }

  private boolean existe(Connection conexao) throws SQLException {
    try (var consulta = conexao.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
      consulta.setQueryTimeout(5);
      consulta.setString(1, this.endereco.nome());
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }
}
