package br.com.modelagemdominio.rico.faturas.taxas;

import java.math.BigDecimal;

public interface TaxaPagamento {
  BigDecimal valorSobre(BigDecimal saldo);
}
