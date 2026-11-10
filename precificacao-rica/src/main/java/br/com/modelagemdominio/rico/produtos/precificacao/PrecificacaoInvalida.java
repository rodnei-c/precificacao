package br.com.modelagemdominio.rico.produtos.precificacao;

public final class PrecificacaoInvalida extends Exception {

  /** @param mensagem descrição da regra violada */
  public PrecificacaoInvalida(String mensagem) {
    this(mensagem, null);
  }

  /**
   * @param mensagem descrição da regra violada
   * @param causa falha original, preservada durante a adaptação do valor
   */
  public PrecificacaoInvalida(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
