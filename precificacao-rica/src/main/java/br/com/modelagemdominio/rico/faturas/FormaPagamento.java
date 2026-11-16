package br.com.modelagemdominio.rico.faturas;

public enum FormaPagamento {

  PIX(TipoTaxaPagamento.SEM_TAXA),
  DINHEIRO(TipoTaxaPagamento.SEM_TAXA),
  CARTAO_DEBITO(TipoTaxaPagamento.SEM_TAXA),
  CARTAO_CREDITO(TipoTaxaPagamento.CARTAO_CREDITO),
  BOLETO(TipoTaxaPagamento.BOLETO);

  private final TipoTaxaPagamento tipoTaxa;

  FormaPagamento(TipoTaxaPagamento tipoTaxa) {
    this.tipoTaxa = tipoTaxa;
  }

  public TaxaPagamento taxa() {
    return this.tipoTaxa.estrategia();
  }
}
