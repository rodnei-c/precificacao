package br.com.modelagemdominio.rico.clientes;

import java.util.List;
import java.util.Optional;

public interface Clientes {

    Cliente cadastrar(IdentificacaoCliente identificacao, ContatoCliente contato, Optional<Endereco> endereco)
        throws FalhaCliente;

    default Cliente alterarEmail(long codigo, Email email) throws FalhaCliente {
        return this.alterarContatos(codigo, new AlteracaoContatos(Optional.of(email), AlteracaoCampo.preservar()));
    }

    Cliente alterarContatos(long codigo, AlteracaoContatos alteracao) throws FalhaCliente;

    Cliente cliente(long codigo) throws FalhaCliente;

    List<Cliente> listar() throws FalhaCliente;
}
