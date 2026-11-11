package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import br.com.modelagemdominio.rico.http.ParametrosDaConsulta;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

final class FiltrosDeFaturasNaUrl {

  private final ParametrosDaConsulta parametros;

  FiltrosDeFaturasNaUrl(ParametrosDaConsulta parametros) {
    this.parametros = parametros;
  }

  FiltrosFaturas interpretados() {
    return new FiltrosFaturas(this.parametro("inicio").map(this::data),
      this.parametro("fim").map(this::data),
      this.parametro("codigoCliente").map(this::codigo));
  }

  private Optional<String> parametro(String nome) {
    return this.parametros.primeiro(nome).map(String::strip).filter(texto -> !texto.isEmpty());
  }

  private LocalDate data(String valor) {
    try {
      return LocalDate.parse(valor);
    } catch (DateTimeParseException falha) {
      throw new FaturaInvalida("Informe as datas no formato AAAA-MM-DD.");
    }
  }

  private long codigo(String valor) {
    try {
      return Long.parseLong(valor);
    } catch (NumberFormatException falha) {
      throw new FaturaInvalida("Informe um código de cliente inteiro positivo válido.");
    }
  }
}
