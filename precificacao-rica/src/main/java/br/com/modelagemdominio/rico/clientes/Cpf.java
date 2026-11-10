package br.com.modelagemdominio.rico.clientes;

import java.util.regex.Pattern;

public record Cpf(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}");
    private static final Pattern REPETIDO = Pattern.compile("([0-9])\\1{10}");
    private static final String INVALIDO = "Informe um CPF válido.";

    public Cpf {
        valor = TextoCliente.obrigatorio("CPF", valor, 14);
        if (!FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException(INVALIDO);
        }
        valor = valor.replace(".", "").replace("-", "");
        if (REPETIDO.matcher(valor).matches() || !digitosVerificadoresValidos(valor)) {
            throw new IllegalArgumentException(INVALIDO);
        }
    }

    private static boolean digitosVerificadoresValidos(String cpf) {
        return digitoVerificador(cpf, 9) == cpf.charAt(9) - '0'
            && digitoVerificador(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digitoVerificador(String cpf, int quantidade) {
        int soma = 0;
        for (int indice = 0; indice < quantidade; indice++) {
            soma += (cpf.charAt(indice) - '0') * (quantidade + 1 - indice);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
