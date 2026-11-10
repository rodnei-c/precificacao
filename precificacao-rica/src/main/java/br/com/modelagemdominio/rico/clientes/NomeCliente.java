package br.com.modelagemdominio.rico.clientes;

public record NomeCliente(String valor) {

  public NomeCliente {
    valor = new TextoObrigatorio("nome", valor, 200).valor();
  }
}
