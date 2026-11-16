package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

public final class TaxaFixaPagamento implements TaxaPagamento {

    private final BigDecimal valor;

    public TaxaFixaPagamento(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new FaturaInvalida("O valor da taxa deve ser positivo ou zero.");
        }
        this.valor = valor;
    }

    @Override
    public BigDecimal valorSobre(BigDecimal saldo) {
        return saldo.signum() == 0 ? BigDecimal.ZERO : this.valor;
    }
}
