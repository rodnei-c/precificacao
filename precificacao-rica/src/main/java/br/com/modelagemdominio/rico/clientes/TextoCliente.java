package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;

final class TextoCliente {

    private TextoCliente() {
    }

    static String obrigatorio(String campo, String valor, int limite) {
        return opcional(campo, Optional.ofNullable(valor), limite).orElseThrow(
            () -> new IllegalArgumentException("O campo " + campo + " é obrigatório.")
        );
    }

    static Optional<String> opcional(String campo, Optional<String> valor, int limite) {
        return valor.filter(texto -> !texto.isBlank()).map(texto -> validar(campo, texto.strip(), limite));
    }

    private static String validar(String campo, String texto, int limite) {
        if (texto.indexOf('\0') >= 0 || texto.length() > limite) {
            throw new IllegalArgumentException(
                "O campo " + campo + " deve conter texto válido de até " + limite + " caracteres."
            );
        }
        return texto;
    }
}
