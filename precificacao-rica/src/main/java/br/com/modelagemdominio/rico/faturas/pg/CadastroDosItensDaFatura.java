package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.ItemFatura;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Grava os itens em lote na conexão da transação, com sua posição original. */
final class CadastroDosItensDaFatura {

  private final Connection conexao;

  CadastroDosItensDaFatura(Connection conexao) {
    this.conexao = conexao;
  }

  void cadastrar(long codigo, List<ItemFatura> itens) throws SQLException {
    try (var cadastro = this.conexao.prepareStatement("""
      INSERT INTO public.item_fatura (fatura_id, produto_codigo, quantidade, posicao) VALUES (?, ?, ?, ?)
      """)) {
      for (int posicao = 0; posicao < itens.size(); posicao++) {
        ItemFatura item = itens.get(posicao);
        cadastro.setLong(1, codigo);
        cadastro.setLong(2, item.codigoProduto());
        cadastro.setBigDecimal(3, item.quantidade().valor());
        cadastro.setInt(4, posicao);
        cadastro.addBatch();
      }
      int[] gravados = cadastro.executeBatch();
      if (gravados.length != itens.size()) {
        throw new SQLException("A gravação não retornou todos os itens da fatura.");
      }
      for (int gravado : gravados) {
        if (gravado != 1 && gravado != Statement.SUCCESS_NO_INFO) {
          throw new SQLException("Um item da fatura não foi gravado.");
        }
      }
    }
  }
}
