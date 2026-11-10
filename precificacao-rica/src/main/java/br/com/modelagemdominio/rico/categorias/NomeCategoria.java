package br.com.modelagemdominio.rico.categorias;

public record NomeCategoria(String valor) {

    public NomeCategoria {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Informe o nome da categoria.");
        }
        valor = valor.strip();
        if (valor.length() > 200) {
            throw new IllegalArgumentException("O nome da categoria deve ter no máximo 200 caracteres.");
        }
    }
}
