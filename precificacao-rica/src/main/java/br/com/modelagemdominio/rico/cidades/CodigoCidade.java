package br.com.modelagemdominio.rico.cidades;

import java.math.BigDecimal;

public record CodigoCidade(long valor) {

    private static final String INVALIDO = "Informe um código de cidade inteiro positivo válido.";

    public CodigoCidade(BigDecimal valor) {
        this(inteiroExato(valor));
    }

    public CodigoCidade {
        if (valor <= 0) {
            throw new IllegalArgumentException(INVALIDO);
        }
    }

    private static long inteiroExato(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException(INVALIDO);
        }
        try {
            return valor.longValueExact();
        } catch (ArithmeticException falha) {
            throw new IllegalArgumentException(INVALIDO, falha);
        }
    }
}
