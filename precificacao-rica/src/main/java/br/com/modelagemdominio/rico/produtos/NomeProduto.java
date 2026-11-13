package br.com.modelagemdominio.rico.produtos;

public record NomeProduto(String valor) {

    public NomeProduto {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }
        valor = valor.strip();
        if (valor.length() > 200) {
            throw new IllegalArgumentException("O nome do produto deve ter no máximo 200 caracteres.");
        }
    }
}
