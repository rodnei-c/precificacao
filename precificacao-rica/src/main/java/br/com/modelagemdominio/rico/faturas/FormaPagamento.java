package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.taxas.*;

import java.math.BigDecimal;

/** Modalidade informada na fatura, sem executar processamento de pagamento. */
public enum FormaPagamento {
  PIX,
  DINHEIRO,
  CARTAO_DEBITO,
  CARTAO_CREDITO,
  BOLETO;

  /** @return regra de taxa desta forma de pagamento */
  public TaxaPagamento taxa() {
    return switch (this) {
      case BOLETO -> new TaxaSomenteComSaldo(new TaxaFixa(new BigDecimal("5.00")));
      case CARTAO_CREDITO -> new TaxaPercentual(new BigDecimal("2"));
      case PIX, DINHEIRO, CARTAO_DEBITO -> new SemTaxa();
    };
  }
}
