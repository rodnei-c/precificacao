package br.com.modelagemdominio.rico.categorias.pg;

import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.CategoriaConstante;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Compõe uma categoria com os valores materializados da linha, sem conservar o cursor SQL. */
final class CategoriaDaLinha {

  private final long codigo;
  private final NomeCategoria nome;

  /**
   * @param resultado cursor posicionado na linha da categoria
   * @throws SQLException quando não for possível ler as colunas
   */
  CategoriaDaLinha(ResultSet resultado) throws SQLException {
    this(resultado.getLong("codigo"), new NomeCategoria(resultado.getString("nome")));
  }

  /**
   * @param codigo identificação lida no registro
   * @param nome nome validado da categoria
   */
  CategoriaDaLinha(long codigo, NomeCategoria nome) {
    this.codigo = codigo;
    this.nome = Objects.requireNonNull(nome);
  }

  /** @return categoria composta com os valores da leitura */
  Categoria categoria() throws SQLException {
    try {
      return new CategoriaConstante(this.codigo, this.nome);
    } catch (IllegalArgumentException falha) {
      throw new SQLException("Categoria com identidade inválida no banco.", falha);
    }
  }
}
