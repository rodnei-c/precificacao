package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.ProdutoConstante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.LongFunction;

/** Atualiza os valores de um produto bloqueado na transação do chamador. */
final class AtualizacaoDaPrecificacao {

  private final Connection conexao;
  private final LongFunction<Produto> produtoPorCodigo;

  AtualizacaoDaPrecificacao(Connection conexao, LongFunction<Produto> produtoPorCodigo) {
    this.conexao = conexao;
    this.produtoPorCodigo = produtoPorCodigo;
  }

  ProdutoConstante atualizar(long codigo, Precificacao nova) throws SQLException {
    try (PreparedStatement atualizacao = this.conexao.prepareStatement("""
      UPDATE public.produto SET custo = ?, preco_venda = ?, percentual = ? WHERE codigo = ?
      RETURNING codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
      """)) {
      new ParametrosSqlDaPrecificacao(nova).informar(atualizacao, 1);
      atualizacao.setLong(4, codigo);
      try (ResultSet resultado = atualizacao.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("A alteração não retornou o produto bloqueado.");
        }
        return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
      }
    }
  }
}
