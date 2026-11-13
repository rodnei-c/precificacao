package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

/** Telefone ausente ou em branco permanece ausente; o conteúdo presente compõe a validação de Telefone. */
public final class TelefoneOpcional implements TextoCliente<Optional<Telefone>> {

  private final Optional<String> conteudo;

  /** @param conteudo texto recebido, inclusive nulo para ausência */
  public TelefoneOpcional(String conteudo) {
    this(Optional.ofNullable(conteudo));
  }

  /** @param conteudo texto recebido ou ausência explícita, sem validação antecipada */
  public TelefoneOpcional(Optional<String> conteudo) {
    this.conteudo = Objects.requireNonNull(conteudo);
  }

  /**
   * @return ausência ou telefone validado, com sua formatação preservada
   * @throws IllegalArgumentException quando o conteúdo presente não for um telefone válido
   */
  @Override
  public Optional<Telefone> valor() {
    return this.conteudo.filter(texto -> !texto.isBlank()).map(Telefone::new);
  }
}
