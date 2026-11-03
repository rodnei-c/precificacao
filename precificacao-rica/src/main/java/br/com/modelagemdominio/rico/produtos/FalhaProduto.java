package br.com.modelagemdominio.rico.produtos;

public final class FalhaProduto extends Exception {

    public enum Motivo {
        CODIGO_INVALIDO,
        PRODUTO_AUSENTE,
        PERSISTENCIA_INDISPONIVEL
    }

    private final Motivo motivo;

    public FalhaProduto(Motivo motivo) {
        this(motivo, null);
    }

    public FalhaProduto(Motivo motivo, Throwable causa) {
        super(motivo.name(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return this.motivo;
    }
}
