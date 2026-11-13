package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.Produto;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Executa o comando preparado e produz os produtos na ordem da consulta. */
final class ProdutosDaConsulta {

  private final PreparedStatement consulta;
  private final ProdutoDaLinha mapeamento;

  ProdutosDaConsulta(PreparedStatement consulta, ProdutoDaLinha mapeamento) {
    this.consulta = consulta;
    this.mapeamento = mapeamento;
  }

  List<Produto> produtos() throws SQLException {
    try (ResultSet resultado = this.consulta.executeQuery()) {
      List<Produto> produtos = new ArrayList<>();
      while (resultado.next()) {
        produtos.add(this.mapeamento.produto(resultado));
      }
      return List.copyOf(produtos);
    }
  }

  @FunctionalInterface
  interface ProdutoDaLinha {
    Produto produto(ResultSet resultado) throws SQLException;
  }
}
