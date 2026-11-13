package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

public record ContatoCliente(Email email, Optional<Telefone> telefone) {

    public ContatoCliente {
        Objects.requireNonNull(email);
        Objects.requireNonNull(telefone);
    }
}
