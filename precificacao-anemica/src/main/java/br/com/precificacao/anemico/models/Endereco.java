package br.com.precificacao.anemico.models;

public record Endereco(
  String cep, String logradouro, String numero, String bairro, String complemento, Long codigoCidade
) {
}
