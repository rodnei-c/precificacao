package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

public record AlteracaoContatos(Optional<Email> email, AlteracaoCampo<Telefone> telefone) {

    public AlteracaoContatos {
        Objects.requireNonNull(email);
        Objects.requireNonNull(telefone);
        if (email.isEmpty() && !telefone.informada()) {
            throw new IllegalArgumentException("Informe o e-mail ou o telefone a ser atualizado.");
        }
    }
}
