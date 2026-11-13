package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.clientes.Cep;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.LocalizacaoEndereco;
import java.math.BigDecimal;
import java.util.Optional;

record EnderecoJson(
  String cep, String logradouro, String numero, String bairro, String complemento, BigDecimal codigoCidade
) {

  Endereco endereco() {
    return new Endereco(
      new Cep(this.cep),
      new LocalizacaoEndereco(this.logradouro, this.numero, this.bairro, Optional.ofNullable(this.complemento)),
      new CodigoCidade(this.codigoCidade)
    );
  }
}
