package br.com.modelagemdominio.rico.cidades;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Interpreta a sigla recebida e produz uma unidade federativa validada e normalizada. */
public final class SiglaUf {

  private final Optional<String> conteudo;

  /** @param conteudo sigla recebida, inclusive nula para ausência */
  public SiglaUf(String conteudo) {
    this(Optional.ofNullable(conteudo));
  }

  /** @param conteudo sigla recebida ou ausência explícita */
  public SiglaUf(Optional<String> conteudo) {
    this.conteudo = Objects.requireNonNull(conteudo);
  }

  /**
   * @return unidade federativa correspondente à sigla normalizada
   * @throws IllegalArgumentException quando faltar sigla ou ela não corresponder a uma UF brasileira
   */
  public UnidadeFederativa unidade() {
    String sigla = this.conteudo.filter(texto -> !texto.isBlank())
      .orElseThrow(() -> new IllegalArgumentException("Informe a UF da cidade."));
    try {
      return UnidadeFederativa.valueOf(sigla.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException falha) {
      throw new IllegalArgumentException("Informe uma sigla de UF brasileira válida.", falha);
    }
  }
}
