package br.com.modelagemdominio.rico.clientes;

import java.util.regex.Pattern;

public record Email(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+");

    public Email {
        valor = TextoCliente.obrigatorio("email", valor, 254);
        if (!FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
    }
}
