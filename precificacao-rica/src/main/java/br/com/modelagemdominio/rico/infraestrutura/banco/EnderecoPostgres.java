package br.com.modelagemdominio.rico.infraestrutura.banco;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.postgresql.Driver;
import org.postgresql.PGProperty;

/** Endereço explícito do banco, preservando servidores e opções na conexão de manutenção. */
record EnderecoPostgres(String url, String manutencao, String nome) {

  private static final String PREFIXO = "jdbc:postgresql:";

  static EnderecoPostgres daUrl(String url, String bancoManutencao) {
    if (url == null || !url.startsWith(PREFIXO)) {
      throw new FalhaInicializacaoBanco("Configure uma URL JDBC PostgreSQL em RICO_DB_URL.");
    }
    validarNome(bancoManutencao);
    int parametros = url.indexOf('?');
    String endereco = parametros < 0 ? url : url.substring(0, parametros);
    String opcoes = parametros < 0 ? "" : url.substring(parametros);
    int inicioNome = endereco.startsWith(PREFIXO + "//")
      ? endereco.indexOf('/', PREFIXO.length() + 2) + 1 : PREFIXO.length();
    if (inicioNome <= 0 || inicioNome >= endereco.length()) {
      throw new FalhaInicializacaoBanco("Informe explicitamente o nome do banco em RICO_DB_URL.");
    }
    String nome;
    try {
      var propriedades = Driver.parseURL(url, new Properties());
      nome = propriedades == null ? null : PGProperty.PG_DBNAME.getOrDefault(propriedades);
    } catch (IllegalArgumentException falha) {
      throw new FalhaInicializacaoBanco("O nome do banco na URL JDBC possui codificação inválida.");
    }
    validarNome(nome);
    String manutencao = URLEncoder.encode(bancoManutencao, StandardCharsets.UTF_8).replace("+", "%20");
    String urlManutencao = endereco.substring(0, inicioNome) + manutencao + opcoes;
    var propriedadesManutencao = Driver.parseURL(urlManutencao, new Properties());
    if (propriedadesManutencao == null || !bancoManutencao.equals(PGProperty.PG_DBNAME.getOrDefault(propriedadesManutencao))) {
      throw new FalhaInicializacaoBanco("A URL JDBC não pode sobrescrever o nome do banco de manutenção.");
    }
    return new EnderecoPostgres(url, urlManutencao, nome);
  }

  String identificadorSql() {
    return "\"" + this.nome.replace("\"", "\"\"") + "\"";
  }

  private static void validarNome(String nome) {
    if (nome == null || nome.isBlank() || nome.indexOf('\0') >= 0
      || nome.getBytes(StandardCharsets.UTF_8).length > 63) {
      throw new FalhaInicializacaoBanco("Informe um nome de banco não vazio com até 63 bytes.");
    }
  }
}
