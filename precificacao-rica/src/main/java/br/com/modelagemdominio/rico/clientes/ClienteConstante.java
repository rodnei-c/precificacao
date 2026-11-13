package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

public record ClienteConstante(
    long codigo, IdentificacaoCliente identificacao, ContatoCliente contato, Optional<Endereco> endereco
) implements Cliente {

    public ClienteConstante {
        if (codigo <= 0) {
            throw new IllegalArgumentException("Informe um código de cliente inteiro positivo válido.");
        }
        Objects.requireNonNull(identificacao);
        Objects.requireNonNull(contato);
        Objects.requireNonNull(endereco);
    }
}
