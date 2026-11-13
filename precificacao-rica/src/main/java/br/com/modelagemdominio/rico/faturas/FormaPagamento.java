package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.taxas.SemTaxa;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaBoleto;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaCartaoCredito;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPagamento;

/** Modalidade informada na fatura, sem executar processamento de pagamento. */
public enum FormaPagamento {
  PIX,
  DINHEIRO,
  CARTAO_DEBITO,
  CARTAO_CREDITO,
  BOLETO;

  /** @return estratégia de cálculo da taxa correspondente a esta modalidade */
  public TaxaPagamento taxa() {
    return switch (this) {
      case PIX, DINHEIRO, CARTAO_DEBITO -> new SemTaxa();
      case BOLETO -> new TaxaBoleto();
      case CARTAO_CREDITO -> new TaxaCartaoCredito();
    };
  }
}
