package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.clientes.Cep;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.LocalizacaoEndereco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Lê o endereço opcional na linha já consultada do cliente. */
final class ResultadoSqlDeEndereco {

  private final ResultSet resultado;

  ResultadoSqlDeEndereco(ResultSet resultado) {
    this.resultado = resultado;
  }

  Optional<Endereco> endereco() throws SQLException {
    Long codigoCidade = this.resultado.getObject("cidade_codigo", Long.class);
    if (codigoCidade == null) {
      return Optional.empty();
    }
    var localizacao = new LocalizacaoEndereco(
      this.resultado.getString("logradouro"), this.resultado.getString("numero"),
      this.resultado.getString("bairro"), Optional.ofNullable(this.resultado.getString("complemento"))
    );
    return Optional.of(new Endereco(
      new Cep(this.resultado.getString("cep")), localizacao, new CodigoCidade(codigoCidade)
    ));
  }
}
