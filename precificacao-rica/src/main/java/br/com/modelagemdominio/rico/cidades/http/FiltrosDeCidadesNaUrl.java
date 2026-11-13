package br.com.modelagemdominio.rico.cidades.http;

import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.SiglaUf;
import br.com.modelagemdominio.rico.http.ParametrosDaConsulta;

final class FiltrosDeCidadesNaUrl {

  private final ParametrosDaConsulta parametros;

  FiltrosDeCidadesNaUrl(ParametrosDaConsulta parametros) {
    this.parametros = parametros;
  }

  FiltrosCidades interpretados() {
    return new FiltrosCidades(
      this.parametros.primeiro("nome").filter(texto -> !texto.isBlank()).map(NomeCidade::new),
      this.parametros.primeiro("uf").filter(texto -> !texto.isBlank()).map(SiglaUf::new).map(SiglaUf::unidade)
    );
  }
}
