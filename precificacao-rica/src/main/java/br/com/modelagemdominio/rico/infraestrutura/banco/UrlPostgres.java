package br.com.modelagemdominio.rico.infraestrutura.banco;

import java.util.Properties;
import org.postgresql.Driver;
import org.postgresql.PGProperty;

/** Interpreta a URL do banco e compõe o endereço usado para manutenção. */
final class UrlPostgres {

  private static final String PREFIXO = "jdbc:postgresql:";

  private final String url;
  private final String manutencao;

  UrlPostgres(String url, String manutencao) {
    this.url = url;
    this.manutencao = manutencao;
  }

  EnderecoPostgres endereco() {
    if (this.url == null || !this.url.startsWith(PREFIXO)) {
      throw new FalhaInicializacaoBanco("Configure uma URL JDBC PostgreSQL em RICO_DB_URL.");
    }
    var bancoManutencao = new NomeBancoPostgres(this.manutencao);
    int parametros = this.url.indexOf('?');
    String endereco = parametros < 0 ? this.url : this.url.substring(0, parametros);
    String opcoes = parametros < 0 ? "" : this.url.substring(parametros);
    int inicioNome = endereco.startsWith(PREFIXO + "//")
      ? endereco.indexOf('/', PREFIXO.length() + 2) + 1 : PREFIXO.length();
    if (inicioNome <= 0 || inicioNome >= endereco.length()) {
      throw new FalhaInicializacaoBanco("Informe explicitamente o nome do banco em RICO_DB_URL.");
    }
    NomeBancoPostgres nome = new NomeBancoPostgres(this.nomeDaUrl());
    String urlManutencao = endereco.substring(0, inicioNome) + bancoManutencao.codificado() + opcoes;
    var propriedades = Driver.parseURL(urlManutencao, new Properties());
    if (propriedades == null || !bancoManutencao.valor().equals(PGProperty.PG_DBNAME.getOrDefault(propriedades))) {
      throw new FalhaInicializacaoBanco("A URL JDBC não pode sobrescrever o nome do banco de manutenção.");
    }
    return new EnderecoPostgres(this.url, urlManutencao, nome);
  }

  private String nomeDaUrl() {
    try {
      var propriedades = Driver.parseURL(this.url, new Properties());
      return propriedades == null ? null : PGProperty.PG_DBNAME.getOrDefault(propriedades);
    } catch (IllegalArgumentException falha) {
      throw new FalhaInicializacaoBanco("O nome do banco na URL JDBC possui codificação inválida.");
    }
  }
}
