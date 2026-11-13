package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

/** Traduz a falha SQL conforme as restrições conhecidas de cada gravação. */
final class FalhaNaGravacaoCliente {

  private final SQLException causa;
  private final Logger registro;

  FalhaNaGravacaoCliente(SQLException causa, Logger registro) {
    this.causa = causa;
    this.registro = registro;
  }

  FalhaCliente noCadastro() {
    if (this.violaRestricao(PSQLState.UNIQUE_VIOLATION, "cliente_cpf_key") || this.emailDuplicado()) {
      return new FalhaCliente(FalhaCliente.Motivo.CPF_OU_EMAIL_DUPLICADO, this.causa);
    }
    return this.cidadeOuIndisponibilidade("cadastrar");
  }

  FalhaCliente naAlteracao() {
    if (this.emailDuplicado()) {
      return new FalhaCliente(FalhaCliente.Motivo.EMAIL_DUPLICADO, this.causa);
    }
    return this.cidadeOuIndisponibilidade("alterar");
  }

  private boolean emailDuplicado() {
    return this.violaRestricao(PSQLState.UNIQUE_VIOLATION, "ux_cliente_email");
  }

  private FalhaCliente cidadeOuIndisponibilidade(String operacao) {
    if (this.violaRestricao(PSQLState.FOREIGN_KEY_VIOLATION, "cliente_cidade_codigo_fkey")) {
      return new FalhaCliente(FalhaCliente.Motivo.CIDADE_AUSENTE, this.causa);
    }
    this.registro.severe("Falha ao " + operacao + " cliente (SQLState: " + this.causa.getSQLState() + ").");
    return new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, this.causa);
  }

  private boolean violaRestricao(PSQLState estado, String restricao) {
    return this.causa instanceof PSQLException postgres
      && estado.getState().equals(postgres.getSQLState())
      && postgres.getServerErrorMessage() != null
      && restricao.equals(postgres.getServerErrorMessage().getConstraint());
  }
}
