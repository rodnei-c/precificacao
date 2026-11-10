package br.com.modelagemdominio.rico.categorias;

import java.math.BigDecimal;

public record CodigoCategoria(long valor) {

    private static final String INVALIDO = "Informe um código de categoria inteiro positivo válido.";

    public CodigoCategoria(BigDecimal valor) {
        this(inteiroExato(valor));
    }

    public CodigoCategoria {
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
