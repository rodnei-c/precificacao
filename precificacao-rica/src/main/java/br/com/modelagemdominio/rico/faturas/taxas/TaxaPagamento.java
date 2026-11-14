package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

public interface TaxaPagamento {

    /**
     * @param saldo subtotal menos desconto aplicado, sem arredondamento
     * @return taxa sem arredondamento
     */
    BigDecimal sobre(BigDecimal saldo);
}
