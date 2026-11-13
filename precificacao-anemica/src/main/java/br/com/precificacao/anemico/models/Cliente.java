package br.com.precificacao.anemico.models;

public record Cliente(Long codigo, String nome, String cpf, String email, String telefone, Endereco endereco) {
}
