package br.com.modelagemdominio.rico.cidades;

public final class FalhaCidade extends Exception {

    public enum Motivo {
        NOME_DUPLICADO_NA_UF,
        PERSISTENCIA_INDISPONIVEL
    }

    private final Motivo motivo;

    public FalhaCidade(Motivo motivo) {
        this(motivo, null);
    }

    public FalhaCidade(Motivo motivo, Throwable causa) {
        super(motivo.name(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return this.motivo;
    }
}
