package br.com.modelagemdominio.rico.faturas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Filtros combináveis da listagem, com as duas datas inclusivas para o usuário. */
public record FiltrosFaturas(Optional<LocalDate> inicio, Optional<LocalDate> fim, Optional<Long> codigoCliente) {

  /** Cria uma consulta sem filtros. */
  public FiltrosFaturas() {
    this(Optional.empty(), Optional.empty(), Optional.empty());
  }

  /**
   * @param inicio primeiro dia do período, quando informado
   * @param fim último dia do período, quando informado
   * @param codigoCliente código positivo, sem exigir que o cliente tenha faturas
   * @throws FaturaInvalida quando o intervalo, as datas ou o código forem inválidos
   */
  public FiltrosFaturas {
    Objects.requireNonNull(inicio);
    Objects.requireNonNull(fim);
    Objects.requireNonNull(codigoCliente);
    inicio = inicio.map(data -> new DataDaConsultaDeFaturas(data).validada());
    fim = fim.map(data -> new DataDaConsultaDeFaturas(data).validada());
    if (inicio.isPresent() && fim.isPresent() && inicio.orElseThrow().isAfter(fim.orElseThrow())) {
      throw new FaturaInvalida("A data inicial deve ser anterior ou igual à data final.");
    }
    if (codigoCliente.isPresent() && codigoCliente.orElseThrow() <= 0) {
      throw new FaturaInvalida("O código do cliente deve ser maior que zero.");
    }
  }

  /** @return início inclusivo do período, sem horário informado pelo usuário */
  public Optional<LocalDateTime> inicioInclusivo() {
    return this.inicio.map(LocalDate::atStartOfDay);
  }

  /** @return começo do dia seguinte, para incluir todo o último dia na consulta */
  public Optional<LocalDateTime> fimExclusivo() {
    return this.fim.map(data -> data.plusDays(1).atStartOfDay());
  }
}
