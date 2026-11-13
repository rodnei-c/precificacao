package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

public record AlteracaoCliente(
  Optional<Email> email, AlteracaoCampo<Telefone> telefone, AlteracaoCampo<Endereco> endereco
) {

  public AlteracaoCliente {
    Objects.requireNonNull(email);
    Objects.requireNonNull(telefone);
    Objects.requireNonNull(endereco);
    if (email.isEmpty() && !telefone.informada() && !endereco.informada()) {
      throw new IllegalArgumentException("Informe o e-mail, telefone ou endereço a ser atualizado.");
    }
  }
}
