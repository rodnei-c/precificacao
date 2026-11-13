package br.com.modelagemdominio.rico.infraestrutura.banco;

/** Impede a publicação da API quando o banco não puder ser preparado. */
public final class FalhaInicializacaoBanco extends RuntimeException {

  /** @param mensagem orientação de configuração, sem dados de conexão sensíveis */
  public FalhaInicializacaoBanco(String mensagem) {
    super(mensagem);
  }

  /**
   * @param mensagem orientação de configuração, sem dados de conexão sensíveis
   * @param causa falha original, preservada para diagnóstico
   */
  public FalhaInicializacaoBanco(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
