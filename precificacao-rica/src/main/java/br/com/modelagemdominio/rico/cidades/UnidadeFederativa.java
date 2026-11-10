package br.com.modelagemdominio.rico.cidades;

import java.util.Locale;

public enum UnidadeFederativa {
    AC, AL, AM, AP, BA, CE, DF, ES, GO, MA, MG, MS, MT, PA,
    PB, PE, PI, PR, RJ, RN, RO, RR, RS, SC, SE, SP, TO;

    public static UnidadeFederativa daSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            throw new IllegalArgumentException("Informe a UF da cidade.");
        }
        try {
            return valueOf(sigla.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException falha) {
            throw new IllegalArgumentException("Informe uma sigla de UF brasileira válida.", falha);
        }
    }
}
