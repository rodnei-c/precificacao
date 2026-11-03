package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.precificacao.PrecificacaoParaCadastro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.LongFunction;
import javax.sql.DataSource;

public final class PgProdutosConstantes implements Produtos {

    private final DataSource banco;
    private final LongFunction<Produto> produtoPorCodigo;
    private final Produtos origem;

    public PgProdutosConstantes(DataSource banco) {
        this(banco, codigo -> new PgProduto(banco, codigo));
    }

    public PgProdutosConstantes(DataSource banco, LongFunction<Produto> produtoPorCodigo) {
        this(banco, produtoPorCodigo, new PgProdutos(banco, produtoPorCodigo));
    }

    public PgProdutosConstantes(DataSource banco, LongFunction<Produto> produtoPorCodigo, Produtos origem) {
        this.banco = banco;
        this.origem = origem;
        this.produtoPorCodigo = produtoPorCodigo;
    }

    @Override
    public Produto produto(long codigo) throws FalhaProduto {
        if (codigo <= 0) {
            throw new FalhaProduto(FalhaProduto.Motivo.CODIGO_INVALIDO);
        }
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement consulta = conexao.prepareStatement("""
                 SELECT codigo, nome, custo, preco_venda, percentual, modalidade_precificacao
                 FROM public.produto WHERE codigo = ?
                 """)) {
            consulta.setLong(1, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
                }
                return new ResultadoSqlDeProduto(resultado, this.produtoPorCodigo).produto();
            }
        } catch (SQLException falha) {
            throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
        }
    }

    @Override
    public Produto cadastrar(NomeProduto nome, PrecificacaoParaCadastro precificacao) throws FalhaProduto {
        return this.origem.cadastrar(nome, precificacao);
    }
}
