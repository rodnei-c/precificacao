package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

public interface TaxaPagamento {

    BigDecimal valorSobre(BigDecimal saldo);

}
