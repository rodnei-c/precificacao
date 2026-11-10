package br.com.modelagemdominio.rico.cidades;

public record NomeCidade(String valor) {

    public NomeCidade {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Informe o nome da cidade.");
        }
        valor = valor.strip();
        if (valor.length() > 200) {
            throw new IllegalArgumentException("O nome da cidade deve ter no máximo 200 caracteres.");
        }
    }
}
