package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

/** Produto cuja leitura consulta os valores atuais do registro correspondente. */
public final class PgProduto implements Produto {

  private final DataSource banco;
  private final long codigo;

  /**
   * @param banco fonte das conexões de leitura
   * @param codigo identificação do produto representado
   */
  public PgProduto(DataSource banco, long codigo) {
    this.banco = banco;
    this.codigo = codigo;
  }

  /** {@inheritDoc} */
  @Override
  public long codigo() {
    return this.codigo;
  }

  /** {@inheritDoc} */
  @Override
  public String nome() throws FalhaProduto {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(
        "SELECT nome FROM public.produto WHERE codigo = ?"
      )) {
      consulta.setLong(1, this.codigo);
      try (ResultSet resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return resultado.getString("nome");
      }
    } catch (SQLException falha) {
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Precificacao precificacao() throws FalhaProduto {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement("""
        SELECT custo, preco_venda, percentual, modalidade_precificacao
        FROM public.produto WHERE codigo = ?
        """)) {
      consulta.setLong(1, this.codigo);
      try (ResultSet resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return new ResultadoSqlDePrecificacao(resultado).precificacao();
      }
    } catch (SQLException falha) {
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Optional<CodigoCategoria> codigoCategoria() throws FalhaProduto {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(
        "SELECT categoria_codigo FROM public.produto WHERE codigo = ?"
      )) {
      consulta.setLong(1, this.codigo);
      try (ResultSet resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return new ResultadoSqlDeCategoria(resultado).codigo();
      }
    } catch (SQLException falha) {
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }
}
