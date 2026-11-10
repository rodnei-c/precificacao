package br.com.modelagemdominio.rico.clientes;

public record NomeCliente(String valor) {

    public NomeCliente {
        valor = TextoCliente.obrigatorio("nome", valor, 200);
    }
}
