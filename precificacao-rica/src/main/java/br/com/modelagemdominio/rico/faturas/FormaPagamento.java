package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

/** Modalidade informada na fatura, sem executar processamento de pagamento. */
public enum FormaPagamento {
  PIX(new SemTaxaPagamento()),
  DINHEIRO(new SemTaxaPagamento()),
  CARTAO_DEBITO(new SemTaxaPagamento()),
  CARTAO_CREDITO(new TaxaCartaoCredito()),
  BOLETO(new TaxaBoleto());

  private final TaxaPagamento taxa;

  FormaPagamento(TaxaPagamento taxa) {
    this.taxa = taxa;
  }

  BigDecimal taxa(BigDecimal saldo) {
    return this.taxa.calcular(saldo);
  }
}
