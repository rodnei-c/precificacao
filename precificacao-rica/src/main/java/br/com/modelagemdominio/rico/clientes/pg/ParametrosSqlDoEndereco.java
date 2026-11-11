package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.clientes.Cep;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.LocalizacaoEndereco;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

/** Preenche os parâmetros consecutivos do endereço ou da sua ausência. */
record ParametrosSqlDoEndereco(Optional<Endereco> endereco) {

  void informar(PreparedStatement comando, int inicio) throws SQLException {
    var localizacao = this.endereco.map(Endereco::localizacao);
    comando.setString(inicio, this.endereco.map(Endereco::cep).map(Cep::valor).orElse(null));
    comando.setString(inicio + 1, localizacao.map(LocalizacaoEndereco::logradouro).orElse(null));
    comando.setString(inicio + 2, localizacao.map(LocalizacaoEndereco::numero).orElse(null));
    comando.setString(inicio + 3, localizacao.map(LocalizacaoEndereco::bairro).orElse(null));
    comando.setString(inicio + 4, localizacao.flatMap(LocalizacaoEndereco::complemento).orElse(null));
    comando.setObject(inicio + 5, this.endereco.map(Endereco::codigoCidade).map(CodigoCidade::valor).orElse(null), Types.BIGINT);
  }
}
