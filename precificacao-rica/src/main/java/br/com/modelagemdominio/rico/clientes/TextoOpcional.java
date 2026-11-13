package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

/** Texto ausente ou em branco permanece ausente; quando presente, compõe a validação do campo. */
final class TextoOpcional implements TextoCliente<Optional<String>> {

  private final Optional<TextoCliente<String>> origem;

  /**
   * @param campo nome do campo usado na mensagem de validação
   * @param conteudo texto recebido ou ausência explícita
   * @param limite comprimento máximo do texto normalizado
   */
  TextoOpcional(String campo, Optional<String> conteudo, int limite) {
    this(conteudo.filter(texto -> !texto.isBlank())
      .<TextoCliente<String>>map(texto -> new TextoObrigatorio(campo, texto, limite)));
  }

  /** @param origem objeto que valida o texto presente, sem avaliá-lo durante a construção */
  TextoOpcional(Optional<TextoCliente<String>> origem) {
    this.origem = Objects.requireNonNull(origem);
  }

  @Override
  public Optional<String> valor() {
    return this.origem.map(TextoCliente::valor);
  }
}
