package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;

public record IdentificacaoCliente(NomeCliente nome, Cpf cpf) {

    public IdentificacaoCliente {
        Objects.requireNonNull(nome);
        Objects.requireNonNull(cpf);
    }
}
