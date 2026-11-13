package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;

/** Compõe a conferência dos dois dígitos de um CPF já normalizado. */
final class DigitosVerificadoresCpf {

  private final DigitoVerificadorCpf primeiro;
  private final DigitoVerificadorCpf segundo;

  /** @param cpf CPF com onze algarismos, após a validação de formato */
  DigitosVerificadoresCpf(String cpf) {
    this(new DigitoVerificadorCpf(cpf, 9), new DigitoVerificadorCpf(cpf, 10));
  }

  /**
   * @param primeiro conferência do primeiro dígito
   * @param segundo conferência do segundo dígito
   */
  DigitosVerificadoresCpf(DigitoVerificadorCpf primeiro, DigitoVerificadorCpf segundo) {
    this.primeiro = Objects.requireNonNull(primeiro);
    this.segundo = Objects.requireNonNull(segundo);
  }

  /** @return se ambos os dígitos estão corretos, sem aceitar falha em qualquer um deles */
  boolean validos() {
    return this.primeiro.valido() && this.segundo.valido();
  }
}
