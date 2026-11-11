package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import br.com.modelagemdominio.rico.categorias.pg.PgCategorias;
import br.com.modelagemdominio.rico.produtos.CodigoProduto;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.FiltrosProdutos;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoInvalida;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class PgProdutos implements Produtos {

  private static final Logger LOG = Logger.getLogger(PgProdutos.class.getName());

  private final DataSource banco;
  private final LongFunction<Produto> produtoPorCodigo;
  private final Categorias categorias;

  public PgProdutos(DataSource banco) {
    this(banco, codigo -> new PgProduto(banco, codigo));
  }

  public PgProdutos(DataSource banco, LongFunction<Produto> produtoPorCodigo) {
    this(banco, produtoPorCodigo, new PgCategorias(banco));
  }

  public PgProdutos(DataSource banco, LongFunction<Produto> produtoPorCodigo, Categorias categorias) {
    this.banco = banco;
    this.produtoPorCodigo = produtoPorCodigo;
    this.categorias = categorias;
  }

  @Override
  public Produto produto(long codigo) throws FalhaProduto {
    var referencia = new CodigoProduto(codigo);
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(
        "SELECT codigo FROM public.produto WHERE codigo = ?"
      )) {
      consulta.setLong(1, referencia.valor());
      try (ResultSet resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return this.produtoPorCodigo.apply(resultado.getLong("codigo"));
      }
    } catch (SQLException falha) {
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public List<Produto> listar(FiltrosProdutos filtros) throws FalhaProduto {
    return new ConsultaSqlDeProdutos(this.banco).listar("codigo", filtros,
      resultado -> this.produtoPorCodigo.apply(resultado.getLong("codigo")));
  }

  @Override
  public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro cadastroValidado,
              Optional<CodigoCategoria> codigoCategoria) throws FalhaProduto {
    if (codigoCategoria.isPresent()) {
      this.validarCategoria(codigoCategoria.orElseThrow());
    }
    Precificacao precificacao = cadastroValidado.precificacao();
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement cadastro = conexao.prepareStatement("""
        INSERT INTO public.produto
          (nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo)
        VALUES (?, ?, ?, ?, ?, ?)
        RETURNING codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
        """)) {
      cadastro.setString(1, nome.valor());
      cadastro.setBigDecimal(2, precificacao.custo().valor());
      cadastro.setBigDecimal(3, precificacao.precoVenda().valor());
      cadastro.setBigDecimal(4, precificacao.percentual().valor());
      cadastro.setString(5, precificacao.modalidade().name());
      cadastro.setObject(6, codigoCategoria.map(CodigoCategoria::valor).orElse(null), Types.BIGINT);
      try (ResultSet resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("O cadastro não retornou o produto.");
        }
        return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
      }
    } catch (SQLException falha) {
      throw new FalhaNaGravacaoProduto(falha, codigoCategoria.isPresent(), LOG).traduzida();
    }
  }

  @Override
  public Produto vincularCategoria(long codigo, CodigoCategoria codigoCategoria) throws FalhaProduto {
    var referencia = new CodigoProduto(codigo);
    this.validarCategoria(codigoCategoria);
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement atualizacao = conexao.prepareStatement("""
        UPDATE public.produto SET categoria_codigo = ? WHERE codigo = ?
        RETURNING codigo, nome, custo, preco_venda, percentual, modalidade_precificacao, categoria_codigo
        """)) {
      atualizacao.setLong(1, codigoCategoria.valor());
      atualizacao.setLong(2, referencia.valor());
      try (ResultSet resultado = atualizacao.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
      }
    } catch (SQLException falha) {
      throw new FalhaNaGravacaoProduto(falha, true, LOG).traduzida();
    }
  }

  @Override
  public Produto alterarPrecificacao(long codigo, ValoresParaPrecificar valores)
    throws FalhaProduto, PrecificacaoInvalida {
    var referencia = new CodigoProduto(codigo);
    return new AlteracaoSqlDePrecificacao(this.banco, this.produtoPorCodigo).alterar(referencia.valor(), valores);
  }

  private void validarCategoria(CodigoCategoria codigoCategoria) throws FalhaProduto {
    try {
      if (!this.categorias.existe(codigoCategoria)) {
        throw new FalhaProduto(FalhaProduto.Motivo.CATEGORIA_AUSENTE);
      }
    } catch (FalhaCategoria falha) {
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }
}
