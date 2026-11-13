package br.com.precificacao.anemico.config;

import br.com.precificacao.anemico.repositories.impl.CriadorBancoPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Prepara o banco configurado antes de executar as migrações. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "precificacao.banco", name = "criar-automaticamente",
  havingValue = "true", matchIfMissing = true)
public class ConfiguracaoBanco {

  /**
   * @param url conexão usada pelo Flyway
   * @param usuario usuário da conexão
   * @param senha senha da conexão
   * @param manutencao banco existente usado para executar CREATE DATABASE
   * @return estratégia que garante a existência do banco e aplica as migrações
   */
  @Bean
  public FlywayMigrationStrategy migracaoComCriacaoBanco(
    @Value("${spring.flyway.url:${spring.datasource.url}}") String url,
    @Value("${spring.flyway.user:${spring.datasource.username}}") String usuario,
    @Value("${spring.flyway.password:${spring.datasource.password}}") String senha,
    @Value("${precificacao.banco.manutencao:postgres}") String manutencao
  ) {
    CriadorBancoPostgres criador = new CriadorBancoPostgres(url, usuario, senha, manutencao);
    return flyway -> {
      criador.garantirExistencia();
      flyway.migrate();
    };
  }
}
