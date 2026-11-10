package br.com.modelagemdominio.rico.clientes;

import java.util.regex.Pattern;

public record Cep(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[0-9]{5}-?[0-9]{3}");

    public Cep {
        valor = TextoCliente.obrigatorio("CEP", valor, 9);
        if (!FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("Informe um CEP com oito dígitos.");
        }
        valor = valor.replace("-", "");
    }
}
