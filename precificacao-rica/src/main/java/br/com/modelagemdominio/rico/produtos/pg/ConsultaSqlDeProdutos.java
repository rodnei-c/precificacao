package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

final class ConsultaSqlDeProdutos {

  private static final Logger LOG = Logger.getLogger(ConsultaSqlDeProdutos.class.getName());

  private final DataSource banco;

  ConsultaSqlDeProdutos(DataSource banco) {
    this.banco = banco;
  }

  List<Produto> listar(String colunas, FiltrosProdutos filtros, ProdutoDaLinha mapeamento) throws FalhaProduto {
    StringBuilder sql = new StringBuilder("SELECT ").append(colunas).append(" FROM public.produto WHERE TRUE");
    List<Object> parametros = new ArrayList<>();
    filtros.nome().ifPresent(nome -> {
      sql.append(" AND STRPOS(LOWER(nome), LOWER(?)) > 0");
      parametros.add(nome);
    });
    filtros.modalidade().ifPresent(modalidade -> {
      sql.append(" AND modalidade_precificacao = ?");
      parametros.add(modalidade.name());
    });
    filtros.categoria().ifPresent(categoria -> {
      sql.append(" AND categoria_codigo = ?");
      parametros.add(categoria.valor());
    });
    sql.append(" ORDER BY codigo");
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(sql.toString())) {
      for (int indice = 0; indice < parametros.size(); indice++) {
        consulta.setObject(indice + 1, parametros.get(indice));
      }
      return produtos(consulta, mapeamento);
    } catch (SQLException falha) {
      LOG.severe("Falha ao listar produtos (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  private static List<Produto> produtos(PreparedStatement consulta, ProdutoDaLinha mapeamento) throws SQLException {
    try (ResultSet resultado = consulta.executeQuery()) {
      List<Produto> produtos = new ArrayList<>();
      while (resultado.next()) {
        produtos.add(mapeamento.produto(resultado));
      }
      return List.copyOf(produtos);
    }
  }

  @FunctionalInterface
  interface ProdutoDaLinha {
    Produto produto(ResultSet resultado) throws SQLException;
  }
}
