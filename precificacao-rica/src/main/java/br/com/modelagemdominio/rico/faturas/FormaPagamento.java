package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.taxas.SemTaxaPagamento;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaFixa;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPagamento;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPercentual;
import java.math.BigDecimal;

/** Modalidade informada na fatura, sem executar processamento de pagamento. */
public enum FormaPagamento implements TaxaPagamento {
  PIX(new SemTaxaPagamento()),
  DINHEIRO(new SemTaxaPagamento()),
  CARTAO_DEBITO(new SemTaxaPagamento()),
  CARTAO_CREDITO(new TaxaPercentual(new BigDecimal("2"))),
  BOLETO(new TaxaFixa(new BigDecimal("5.00")));

  private final TaxaPagamento taxa;

  FormaPagamento(TaxaPagamento taxa) {
    this.taxa = taxa;
  }

  @Override
  public BigDecimal valorSobre(BigDecimal saldo) {
    return this.taxa.valorSobre(saldo);
  }
}
