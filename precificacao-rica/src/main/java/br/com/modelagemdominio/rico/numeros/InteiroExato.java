package br.com.modelagemdominio.rico.numeros;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Número recebido que exige presença e conversão para long sem perda de precisão. */
public final class InteiroExato {

  private final Optional<BigDecimal> conteudo;
  private final String mensagem;

  /**
   * @param conteudo número recebido, inclusive nulo para ausência
   * @param mensagem descrição da violação no campo de origem
   */
  public InteiroExato(BigDecimal conteudo, String mensagem) {
    this(Optional.ofNullable(conteudo), mensagem);
  }

  /**
   * @param conteudo número recebido ou ausência explícita
   * @param mensagem descrição da violação no campo de origem
   */
  public InteiroExato(Optional<BigDecimal> conteudo, String mensagem) {
    this.conteudo = Objects.requireNonNull(conteudo);
    this.mensagem = Objects.requireNonNull(mensagem);
  }

  /**
   * @return inteiro exato, sem truncamento ou arredondamento
   * @throws IllegalArgumentException quando faltar número, houver fração ou o limite de long for excedido
   */
  public long valor() {
    BigDecimal numero = this.conteudo.orElseThrow(() -> new IllegalArgumentException(this.mensagem));
    try {
      return numero.longValueExact();
    } catch (ArithmeticException falha) {
      throw new IllegalArgumentException(this.mensagem, falha);
    }
  }
}
