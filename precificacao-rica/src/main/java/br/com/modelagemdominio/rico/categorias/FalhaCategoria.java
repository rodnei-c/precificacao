package br.com.modelagemdominio.rico.categorias;

public final class FalhaCategoria extends Exception {

    public enum Motivo {
        NOME_DUPLICADO,
        CATEGORIA_AUSENTE,
        PERSISTENCIA_INDISPONIVEL
    }

    private final Motivo motivo;

    public FalhaCategoria(Motivo motivo) {
        this(motivo, null);
    }

    public FalhaCategoria(Motivo motivo, Throwable causa) {
        super(motivo.name(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return this.motivo;
    }
}
