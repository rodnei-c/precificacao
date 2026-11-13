package br.com.precificacao.anemico.dtos;

import br.com.precificacao.anemico.dtos.CadastroClienteRequisicao.EnderecoRequisicao;
import com.fasterxml.jackson.annotation.JsonSetter;

/** Guarda os valores enviados e distingue um campo omitido de um campo nulo. */
public class AlteracaoClienteRequisicao {

  private String email;
  private boolean emailInformado;
  private String telefone;
  private boolean telefoneInformado;
  private EnderecoRequisicao endereco;
  private boolean enderecoInformado;

  /** @param email novo e-mail, inclusive nulo para permitir a validação pelo serviço */
  @JsonSetter("email")
  public void setEmail(String email) {
    this.email = email;
    this.emailInformado = true;
  }

  /** @return valor recebido para o e-mail */
  public String email() {
    return email;
  }

  /** @return se o campo estava presente no JSON */
  public boolean emailInformado() {
    return emailInformado;
  }

  /** @param telefone novo telefone; nulo solicita a remoção */
  @JsonSetter("telefone")
  public void setTelefone(String telefone) {
    this.telefone = telefone;
    this.telefoneInformado = true;
  }

  /** @return telefone recebido, inclusive nulo */
  public String telefone() {
    return telefone;
  }

  /** @return se o telefone estava presente no JSON */
  public boolean telefoneInformado() {
    return telefoneInformado;
  }

  /** @param endereco substituição completa do endereço; nulo solicita a remoção */
  @JsonSetter("endereco")
  public void setEndereco(EnderecoRequisicao endereco) {
    this.endereco = endereco;
    this.enderecoInformado = true;
  }

  /** @return endereço recebido, inclusive nulo */
  public EnderecoRequisicao endereco() {
    return endereco;
  }

  /** @return se o endereço estava presente no JSON */
  public boolean enderecoInformado() {
    return enderecoInformado;
  }
}
