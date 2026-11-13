package br.com.modelagemdominio.rico.faturas;

import java.time.LocalDate;

/** Aplica o intervalo de datas aceito na listagem de faturas. */
final class DataDaConsultaDeFaturas {

  private final LocalDate data;

  DataDaConsultaDeFaturas(LocalDate data) {
    this.data = data;
  }

  LocalDate validada() {
    if (this.data.getYear() < 1 || this.data.getYear() > 9999) {
      throw new FaturaInvalida("Informe datas entre 0001-01-01 e 9999-12-31.");
    }
    return this.data;
  }
}
