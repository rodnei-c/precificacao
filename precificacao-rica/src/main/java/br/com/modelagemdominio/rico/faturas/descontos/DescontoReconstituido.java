package br.com.modelagemdominio.rico.faturas.descontos;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Desconto composto a partir do registro original, com comportamento delegado à estratégia. */
public final class DescontoReconstituido implements Desconto {

  private final Desconto origem;

  /** Compõe ausência de desconto, sem criar um registro de valor zero. */
  public DescontoReconstituido() {
    this(Optional.empty());
  }

  /** @param registro tipo e valor original presentes, inclusive quando o valor for zero */
  public DescontoReconstituido(DescontoContratado registro) {
    this(Optional.of(registro));
  }

  /** @param registro desconto original armazenado, ou ausência explícita */
  public DescontoReconstituido(Optional<DescontoContratado> registro) {
    this(Objects.requireNonNull(registro).map(DescontoContratado::desconto).orElseGet(SemDesconto::new));
  }

  /** @param origem estratégia cujo cálculo, validação e registro serão utilizados */
  public DescontoReconstituido(Desconto origem) {
    this.origem = Objects.requireNonNull(origem);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimal valorAplicado(BigDecimal subtotal) {
    return this.origem.valorAplicado(subtotal);
  }

  /** {@inheritDoc} */
  @Override
  public void validarSubtotalInicial(BigDecimal subtotal) {
    this.origem.validarSubtotalInicial(subtotal);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<DescontoContratado> registro() {
    return this.origem.registro();
  }
}
