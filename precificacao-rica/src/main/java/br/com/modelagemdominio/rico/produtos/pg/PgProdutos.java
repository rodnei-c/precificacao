package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.LongFunction;
import javax.sql.DataSource;

public final class PgProdutos implements Produtos {

    private final DataSource banco;
    private final LongFunction<Produto> produtoPorCodigo;

    public PgProdutos(DataSource banco) {
        this(banco, codigo -> new PgProduto(banco, codigo));
    }

    public PgProdutos(DataSource banco, LongFunction<Produto> produtoPorCodigo) {
        this.banco = banco;
        this.produtoPorCodigo = produtoPorCodigo;
    }

    @Override
    public Produto produto(long codigo) throws FalhaProduto {
        if (codigo <= 0) {
            throw new FalhaProduto(FalhaProduto.Motivo.CODIGO_INVALIDO);
        }
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement consulta = conexao.prepareStatement(
                 "SELECT codigo FROM public.produto WHERE codigo = ?"
             )) {
            consulta.setLong(1, codigo);
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
    public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro cadastroValidado) throws FalhaProduto {
        Precificacao precificacao = cadastroValidado.precificacao();
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement cadastro = conexao.prepareStatement("""
                 INSERT INTO public.produto (nome, custo, preco_venda, percentual, modalidade_precificacao)
                 VALUES (?, ?, ?, ?, ?)
                 RETURNING codigo, nome, custo, preco_venda, percentual, modalidade_precificacao
                 """)) {
            cadastro.setString(1, nome.valor());
            cadastro.setBigDecimal(2, precificacao.custo().valor());
            cadastro.setBigDecimal(3, precificacao.precoVenda().valor());
            cadastro.setBigDecimal(4, precificacao.percentual().valor());
            cadastro.setString(5, precificacao.modalidade().name());
            try (ResultSet resultado = cadastro.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("O cadastro não retornou o produto.");
                }
                return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
            }
        } catch (SQLException falha) {
            throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
        }
    }
}
