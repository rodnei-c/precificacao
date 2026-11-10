package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;

public record LocalizacaoEndereco(String logradouro, String numero, String bairro, Optional<String> complemento) {

  public LocalizacaoEndereco {
    logradouro = new TextoObrigatorio("logradouro", logradouro, 200).valor();
    numero = new TextoObrigatorio("número", numero, 20).valor();
    bairro = new TextoObrigatorio("bairro", bairro, 100).valor();
    complemento = new TextoOpcional("complemento", complemento, 100).valor();
  }
}
