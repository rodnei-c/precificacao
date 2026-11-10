package br.com.modelagemdominio.rico.produtos;

/** Violação de um valor de precificação, com descrição adaptável ao campo de entrada. */
public final class ValorInvalido extends IllegalArgumentException {

  private final String regra;

  /** @param regra descrição da regra com um marcador de nome do campo */
  public ValorInvalido(String regra) {
    this(regra, "valor");
  }

  /**
   * @param regra descrição da regra com um marcador de nome do campo
   * @param campo nome do valor que violou a regra
   */
  public ValorInvalido(String regra, String campo) {
    super(regra.formatted(campo));
    this.regra = regra;
  }

  /**
   * @param campo nome do valor na operação que recebeu a entrada
   * @return descrição da mesma violação para esse campo
   */
  public String mensagem(String campo) {
    return this.regra.formatted(campo);
  }
}
