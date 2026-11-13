package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.exceptions.InicializacaoBancoException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Separa o banco da URL JDBC, preservando o servidor e os parâmetros da conexão. */
record EnderecoBancoPostgres(String url, String urlManutencao, String nome) {

  private static final String PREFIXO = "jdbc:postgresql:";

  static EnderecoBancoPostgres daUrl(String url, String manutencao) {
    if (url == null || !url.startsWith(PREFIXO)) {
      throw new InicializacaoBancoException("A criação automática exige uma URL JDBC PostgreSQL.");
    }
    validarNome(manutencao);
    int inicioParametros = url.indexOf('?');
    String endereco = inicioParametros < 0 ? url : url.substring(0, inicioParametros);
    String parametros = inicioParametros < 0 ? "" : url.substring(inicioParametros);
    int inicioNome = endereco.startsWith(PREFIXO + "//")
      ? endereco.indexOf('/', PREFIXO.length() + 2) + 1 : PREFIXO.length();
    if (inicioNome <= 0 || inicioNome >= endereco.length()) {
      throw new InicializacaoBancoException("Informe explicitamente o nome do banco na URL JDBC.");
    }
    String nome;
    try {
      nome = URLDecoder.decode(endereco.substring(inicioNome).replace("+", "%2B"), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException falha) {
      throw new InicializacaoBancoException("O nome do banco na URL JDBC possui codificação inválida.");
    }
    validarNome(nome);
    String nomeManutencao = URLEncoder.encode(manutencao, StandardCharsets.UTF_8).replace("+", "%20");
    return new EnderecoBancoPostgres(
      url, endereco.substring(0, inicioNome) + nomeManutencao + parametros, nome
    );
  }

  String identificadorSql() {
    return "\"" + this.nome.replace("\"", "\"\"") + "\"";
  }

  private static void validarNome(String nome) {
    if (nome == null || nome.isBlank() || nome.indexOf('\0') >= 0
      || nome.getBytes(StandardCharsets.UTF_8).length > 63) {
      throw new InicializacaoBancoException("Informe um nome de banco não vazio com até 63 bytes.");
    }
  }
}
