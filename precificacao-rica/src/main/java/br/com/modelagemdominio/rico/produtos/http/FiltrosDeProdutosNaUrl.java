package br.com.modelagemdominio.rico.produtos.http;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.http.ParametrosDaConsulta;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;

final class FiltrosDeProdutosNaUrl {

  private final ParametrosDaConsulta parametros;

  FiltrosDeProdutosNaUrl(ParametrosDaConsulta parametros) {
    this.parametros = parametros;
  }

  FiltrosProdutos interpretados() {
    return new FiltrosProdutos(
      this.parametros.primeiro("nome"),
      this.parametros.primeiro("modalidadePrecificacao").map(String::trim).map(ModalidadePrecificacao::valueOf),
      this.parametros.primeiro("categoria").map(String::trim).map(Long::parseLong).map(CodigoCategoria::new)
    );
  }
}
