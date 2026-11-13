package br.com.modelagemdominio.rico.faturas;

import java.util.Objects;
import java.util.Optional;

/** Referências opcionais da fatura; a existência do cliente é verificada na operação de cadastro. */
public record VinculosFatura(Optional<Long> codigoCliente, Optional<FormaPagamento> formaPagamento) {

  /** Cria vínculos ausentes, sem código fictício de cliente. */
  public VinculosFatura() {
    this(Optional.empty(), Optional.empty());
  }

  /**
   * @param codigoCliente código positivo ou ausência
   * @param formaPagamento modalidade aceita ou ausência
   * @throws FaturaInvalida quando o código informado não for positivo
   */
  public VinculosFatura {
    Objects.requireNonNull(codigoCliente);
    Objects.requireNonNull(formaPagamento);
    if (codigoCliente.isPresent() && codigoCliente.orElseThrow() <= 0) {
      throw new FaturaInvalida("O código do cliente deve ser maior que zero.");
    }
  }
}
