package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

public final class TaxaPercentualPagamento implements TaxaPagamento {

    private final BigDecimal percentual;

    public TaxaPercentualPagamento(BigDecimal percentual) {
        if (percentual == null
                || percentual.signum() < 0
                || percentual.compareTo(new BigDecimal("100")) > 0
                || percentual.scale() > 4) {
            throw new FaturaInvalida("Informe um percentual de taxa válido.");
        }
        this.percentual = percentual;
    }

    @Override
    public BigDecimal valorSobre(BigDecimal saldo) {
        return saldo.multiply(this.percentual).movePointLeft(2);
    }
}
