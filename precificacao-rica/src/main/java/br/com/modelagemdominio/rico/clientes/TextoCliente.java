package br.com.modelagemdominio.rico.clientes;

/**
 * Valor do cadastro produzido a partir de texto, validado quando seu resultado é solicitado.
 *
 * @param <T> resultado obrigatório ou opcional da leitura do texto
 */
interface TextoCliente<T> {

  /**
   * @return resultado normalizado segundo a obrigatoriedade do campo
   * @throws IllegalArgumentException quando o texto não atender às regras do campo
   */
  T valor();
}
