package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;

public record ClienteConstante(
    long codigo, IdentificacaoCliente identificacao, ContatoCliente contato, Optional<Endereco> endereco
) implements Cliente {
}
