package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

public enum TipoTaxaPagamento {

    SEM_TAXA,
    BOLETO,
    CARTAO_CREDITO;

    public TaxaPagamento estrategia() {
        return switch (this) {
            case SEM_TAXA -> new SemTaxaPagamento();
            case BOLETO -> new TaxaFixaPagamento(new BigDecimal("5.00"));
            case CARTAO_CREDITO -> new TaxaPercentualPagamento(new BigDecimal("2.00"));
        };
    }
}
