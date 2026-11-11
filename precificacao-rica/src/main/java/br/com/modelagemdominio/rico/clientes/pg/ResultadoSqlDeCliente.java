package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.ClienteConstante;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Cpf;
import br.com.modelagemdominio.rico.clientes.Email;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.NomeCliente;
import br.com.modelagemdominio.rico.clientes.TelefoneOpcional;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Compõe um cliente com os valores do cursor pertencente ao chamador. */
final class ResultadoSqlDeCliente {

  static final String COLUNAS =
    "codigo, nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo";

  private final ResultSet resultado;

  ResultadoSqlDeCliente(ResultSet resultado) {
    this.resultado = resultado;
  }

  Cliente encontrado() throws SQLException, FalhaCliente {
    if (!this.resultado.next()) {
      throw new FalhaCliente(FalhaCliente.Motivo.CLIENTE_AUSENTE);
    }
    return this.cliente();
  }

  Cliente cliente() throws SQLException {
    var identificacao = new IdentificacaoCliente(
      new NomeCliente(this.resultado.getString("nome")), new Cpf(this.resultado.getString("cpf"))
    );
    var contato = new ContatoCliente(
      new Email(this.resultado.getString("email")),
      new TelefoneOpcional(this.resultado.getString("telefone")).valor()
    );
    return new ClienteConstante(
      this.resultado.getLong("codigo"), identificacao, contato, new ResultadoSqlDeEndereco(this.resultado).endereco()
    );
  }
}
