package br.com.precificacao.anemico.exceptions;

/** Falha na preparação da conexão ou na criação inicial do banco. */
public class InicializacaoBancoException extends RuntimeException {

  /** @param mensagem orientação para corrigir a configuração */
  public InicializacaoBancoException(String mensagem) {
    super(mensagem);
  }

  /**
   * @param mensagem orientação para corrigir a configuração
   * @param causa falha de conexão ou de criação
   */
  public InicializacaoBancoException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
