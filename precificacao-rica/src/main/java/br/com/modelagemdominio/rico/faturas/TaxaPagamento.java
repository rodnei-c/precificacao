package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;

interface TaxaPagamento {

  BigDecimal calcular(BigDecimal saldo);
}
