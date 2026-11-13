package br.com.modelagemdominio.rico.testes;

import org.flywaydb.core.Flyway;

/** Aplica as migrações somente no banco explicitamente configurado para os testes. */
public final class PrepararBancoIntegracao {

  private PrepararBancoIntegracao() {
  }

  /** @param argumentos não são utilizados; a configuração vem de RICO_BANCO_TESTE_* */
  public static void main(String[] argumentos) {
    String url = variavel("RICO_BANCO_TESTE_URL");
    String usuario = variavel("RICO_BANCO_TESTE_USUARIO");
    String senha = variavel("RICO_BANCO_TESTE_SENHA");
    if (!url.matches("jdbc:postgresql:.+") || usuario.isBlank()) {
      throw new IllegalArgumentException("Configure a URL PostgreSQL e o usuário do banco exclusivo de testes.");
    }
    Flyway.configure()
      .dataSource(url, usuario, senha)
      .locations("classpath:db/migration")
      .schemas("public")
      .cleanDisabled(true)
      .load()
      .migrate();
  }

  private static String variavel(String nome) {
    String valor = System.getenv(nome);
    if (valor == null) {
      throw new IllegalArgumentException("Defina " + nome + " para executar a integração.");
    }
    return valor;
  }
}
