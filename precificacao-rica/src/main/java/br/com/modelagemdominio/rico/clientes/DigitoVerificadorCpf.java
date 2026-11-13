package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;

/** Confere um dígito do CPF a partir do prefixo que participa do cálculo. */
final class DigitoVerificadorCpf {

  private final String base;
  private final char informado;

  /**
   * @param cpf CPF com onze algarismos, após a validação de formato
   * @param posicao posição do dígito a conferir: nove para o primeiro, dez para o segundo
   */
  DigitoVerificadorCpf(String cpf, int posicao) {
    this(cpf.substring(0, posicao), cpf.charAt(posicao));
  }

  /**
   * @param base algarismos anteriores ao dígito a conferir
   * @param informado algarismo recebido para comparação com o resultado
   */
  DigitoVerificadorCpf(String base, char informado) {
    this.base = Objects.requireNonNull(base);
    this.informado = informado;
  }

  /** @return se o dígito recebido corresponde ao cálculo sobre o prefixo */
  boolean valido() {
    int soma = 0;
    for (int indice = 0; indice < this.base.length(); indice++) {
      soma += (this.base.charAt(indice) - '0') * (this.base.length() + 1 - indice);
    }
    int resto = soma % 11;
    int esperado = resto < 2 ? 0 : 11 - resto;
    return esperado == this.informado - '0';
  }
}
