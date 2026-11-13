package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;

public interface Cliente {

    long codigo();

    IdentificacaoCliente identificacao();

    ContatoCliente contato();

    Optional<Endereco> endereco();
}
