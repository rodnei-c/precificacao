package br.com.modelagemdominio.rico.clientes;

public final class FalhaCliente extends Exception {

    public enum Motivo {
        CPF_OU_EMAIL_DUPLICADO,
        EMAIL_DUPLICADO,
        CIDADE_AUSENTE,
        CODIGO_INVALIDO,
        CLIENTE_AUSENTE,
        PERSISTENCIA_INDISPONIVEL
    }

    private final Motivo motivo;

    public FalhaCliente(Motivo motivo) {
        this(motivo, null);
    }

    public FalhaCliente(Motivo motivo, Throwable causa) {
        super(motivo.name(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return this.motivo;
    }
}
