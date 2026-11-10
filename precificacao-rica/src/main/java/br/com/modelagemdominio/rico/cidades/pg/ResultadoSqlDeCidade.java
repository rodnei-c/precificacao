package br.com.modelagemdominio.rico.cidades.pg;

import br.com.modelagemdominio.rico.cidades.Cidade;
import br.com.modelagemdominio.rico.cidades.CidadeConstante;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Compõe uma cidade com os valores materializados da linha, sem conservar o cursor SQL. */
final class ResultadoSqlDeCidade {

  private final long codigo;
  private final NomeCidade nome;
  private final UnidadeFederativa uf;

  /**
   * @param resultado cursor posicionado na linha da cidade
   * @throws SQLException quando não for possível ler as colunas
   */
  ResultadoSqlDeCidade(ResultSet resultado) throws SQLException {
    this(resultado.getLong("codigo"), new NomeCidade(resultado.getString("nome")),
      UnidadeFederativa.valueOf(resultado.getString("uf")));
  }

  /**
   * @param codigo identificação lida no registro
   * @param nome nome validado da cidade
   * @param uf unidade federativa armazenada
   */
  ResultadoSqlDeCidade(long codigo, NomeCidade nome, UnidadeFederativa uf) {
    this.codigo = codigo;
    this.nome = Objects.requireNonNull(nome);
    this.uf = Objects.requireNonNull(uf);
  }

  /** @return cidade composta com os valores da leitura */
  Cidade cidade() {
    return new CidadeConstante(this.codigo, this.nome, this.uf);
  }
}
