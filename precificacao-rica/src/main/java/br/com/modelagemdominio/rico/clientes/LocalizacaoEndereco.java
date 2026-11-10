package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;

public record LocalizacaoEndereco(String logradouro, String numero, String bairro, Optional<String> complemento) {

    public LocalizacaoEndereco {
        logradouro = TextoCliente.obrigatorio("logradouro", logradouro, 200);
        numero = TextoCliente.obrigatorio("número", numero, 20);
        bairro = TextoCliente.obrigatorio("bairro", bairro, 100);
        complemento = TextoCliente.opcional("complemento", complemento, 100);
    }
}
