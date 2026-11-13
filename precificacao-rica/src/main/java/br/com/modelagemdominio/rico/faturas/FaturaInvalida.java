package br.com.modelagemdominio.rico.faturas;

/** Indica que um valor não atende às regras dos itens ou descontos de uma fatura. */
public final class FaturaInvalida extends IllegalArgumentException {

  /** @param mensagem descrição da regra violada */
  public FaturaInvalida(String mensagem) {
    super(mensagem);
  }
}
