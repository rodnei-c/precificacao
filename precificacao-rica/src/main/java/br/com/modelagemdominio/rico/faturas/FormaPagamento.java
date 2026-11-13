package br.com.modelagemdominio.rico.faturas;

/** Modalidade informada na fatura, sem executar processamento de pagamento. */
public enum FormaPagamento {
  PIX,
  DINHEIRO,
  CARTAO_DEBITO,
  CARTAO_CREDITO,
  BOLETO
}
