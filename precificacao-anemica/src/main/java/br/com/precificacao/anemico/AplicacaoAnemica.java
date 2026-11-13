package br.com.precificacao.anemico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inicia a API HTTP da implementação baseada em modelo anêmico.
 */
@SpringBootApplication
public class AplicacaoAnemica {

  /**
   * Inicializa o servidor e executa as migrações do banco configurado.
   *
   * @param args argumentos de inicialização
   */
  public static void main(String[] args) {
    SpringApplication.run(AplicacaoAnemica.class, args);
  }
}
