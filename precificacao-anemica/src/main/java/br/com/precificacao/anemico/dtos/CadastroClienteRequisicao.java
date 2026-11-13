package br.com.precificacao.anemico.dtos;

import java.math.BigDecimal;

public record CadastroClienteRequisicao(
  String nome, String cpf, String email, String telefone, EnderecoRequisicao endereco
) {

  public record EnderecoRequisicao(
    String cep, String logradouro, String numero, String bairro, String complemento, BigDecimal codigoCidade
  ) {
  }
}
