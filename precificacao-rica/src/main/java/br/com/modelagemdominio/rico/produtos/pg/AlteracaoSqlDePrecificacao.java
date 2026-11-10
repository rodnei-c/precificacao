package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.ProdutoConstante;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.LongFunction;
import java.util.logging.Logger;
import javax.sql.DataSource;

final class AlteracaoSqlDePrecificacao {

  private static final Logger LOG = Logger.getLogger(AlteracaoSqlDePrecificacao.class.getName());

  private final DataSource banco;
  private final LongFunction<Produto> produtoPorCodigo;

  AlteracaoSqlDePrecificacao(DataSource banco, LongFunction<Produto> produtoPorCodigo) {
    this.banco = banco;
    this.produtoPorCodigo = produtoPorCodigo;
  }

  Produto alterar(long codigo, ValoresParaPrecificar valores) throws FalhaProduto, PrecificacaoInvalida {
    try (Connection conexao = this.banco.getConnection()) {
      conexao.setAutoCommit(false);
      try {
        Produto produto = this.alterar(conexao, codigo, valores);
        conexao.commit();
        return produto;
      } catch (SQLException | FalhaProduto | PrecificacaoInvalida | RuntimeException falha) {
        try {
          conexao.rollback();
        } catch (SQLException falhaAoDesfazer) {
          falha.addSuppressed(falhaAoDesfazer);
          LOG.severe("Falha ao desfazer a alteração de precificação (SQLState: "
            + falhaAoDesfazer.getSQLState() + ").");
        }
        throw falha;
      }
    } catch (SQLException falha) {
      LOG.severe("Falha ao alterar a precificação (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  private Produto alterar(Connection conexao, long codigo, ValoresParaPrecificar valores)
    throws SQLException, FalhaProduto, PrecificacaoInvalida {
    ProdutoConstante produto = this.produtoParaAtualizacao(conexao, codigo);
    Precificacao anterior = produto.precificacao();
    Precificacao nova = valores.precificacao(anterior.modalidade()).precificacao();
    if (anterior.mesmosValores(nova)) {
      return produto;
    }

    ProdutoConstante atualizado = this.atualizar(conexao, codigo, nova);
    registrar(conexao, codigo, anterior, atualizado.precificacao());
    return atualizado;
  }

  private ProdutoConstante produtoParaAtualizacao(Connection conexao, long codigo)
    throws SQLException, FalhaProduto {
    try (PreparedStatement consulta = conexao.prepareStatement("""
      SELECT codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
      FROM public.produto WHERE codigo = ? FOR UPDATE
      """)) {
      consulta.setLong(1, codigo);
      try (ResultSet resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
      }
    }
  }

  private ProdutoConstante atualizar(Connection conexao, long codigo, Precificacao nova) throws SQLException {
    try (PreparedStatement atualizacao = conexao.prepareStatement("""
      UPDATE public.produto SET custo = ?, preco_venda = ?, percentual = ? WHERE codigo = ?
      RETURNING codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
      """)) {
      informarValores(atualizacao, 1, nova);
      atualizacao.setLong(4, codigo);
      try (ResultSet resultado = atualizacao.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("A alteração não retornou o produto bloqueado.");
        }
        return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
      }
    }
  }

  private static void registrar(Connection conexao, long codigo, Precificacao anterior, Precificacao nova)
    throws SQLException {
    try (PreparedStatement insercao = conexao.prepareStatement("""
      INSERT INTO public.historico_precificacao
        (produto_codigo, custo_anterior, preco_venda_anterior, percentual_anterior,
        custo_novo, preco_venda_novo, percentual_novo)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """)) {
      insercao.setLong(1, codigo);
      informarValores(insercao, 2, anterior);
      informarValores(insercao, 5, nova);
      if (insercao.executeUpdate() != 1) {
        throw new SQLException("A alteração não gerou o registro de histórico.");
      }
    }
  }

  private static void informarValores(PreparedStatement comando, int inicio, Precificacao precificacao)
    throws SQLException {
    comando.setBigDecimal(inicio, precificacao.custo().valor());
    comando.setBigDecimal(inicio + 1, precificacao.precoVenda().valor());
    comando.setBigDecimal(inicio + 2, precificacao.percentual().valor());
  }
}
