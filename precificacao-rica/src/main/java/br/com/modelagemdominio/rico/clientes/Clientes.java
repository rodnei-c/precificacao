package br.com.modelagemdominio.rico.clientes;

import java.util.List;
import java.util.Optional;

public interface Clientes {

  Cliente cadastrar(IdentificacaoCliente identificacao, ContatoCliente contato, Optional<Endereco> endereco)
    throws FalhaCliente;

  default Cliente alterarEmail(long codigo, Email email) throws FalhaCliente {
    return this.alterar(codigo, new AlteracaoCliente(
      Optional.of(email), new CampoPreservado<>(), new CampoPreservado<>()
    ));
  }

  Cliente alterar(long codigo, AlteracaoCliente alteracao) throws FalhaCliente;

  Cliente cliente(long codigo) throws FalhaCliente;

  List<Cliente> listar() throws FalhaCliente;
}
