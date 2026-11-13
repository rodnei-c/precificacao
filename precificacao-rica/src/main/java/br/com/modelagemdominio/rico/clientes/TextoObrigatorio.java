package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

/** Texto com presença obrigatória e limite próprio do campo do cliente. */
final class TextoObrigatorio implements TextoCliente<String> {

  private final String campo;
  private final Optional<String> conteudo;
  private final int limite;

  /**
   * @param campo nome do campo usado na mensagem de validação
   * @param conteudo texto recebido, inclusive nulo para ausência
   * @param limite comprimento máximo depois de remover espaços das extremidades
   */
  TextoObrigatorio(String campo, String conteudo, int limite) {
    this(campo, Optional.ofNullable(conteudo), limite);
  }

  /**
   * @param campo nome do campo usado na mensagem de validação
   * @param conteudo texto recebido ou ausência explícita
   * @param limite comprimento máximo do texto normalizado
   */
  TextoObrigatorio(String campo, Optional<String> conteudo, int limite) {
    this.campo = Objects.requireNonNull(campo);
    this.conteudo = Objects.requireNonNull(conteudo);
    this.limite = limite;
  }

  @Override
  public String valor() {
    String texto = this.conteudo.filter(valor -> !valor.isBlank()).map(String::strip).orElseThrow(
      () -> new IllegalArgumentException("O campo " + this.campo + " é obrigatório.")
    );
    if (texto.indexOf('\0') >= 0 || texto.length() > this.limite) {
      throw new IllegalArgumentException(
        "O campo " + this.campo + " deve conter texto válido de até " + this.limite + " caracteres."
      );
    }
    return texto;
  }
}
