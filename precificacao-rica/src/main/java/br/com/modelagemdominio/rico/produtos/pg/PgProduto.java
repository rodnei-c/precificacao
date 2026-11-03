package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class PgProduto implements Produto {

    private final DataSource banco;
    private final long codigo;

    public PgProduto(DataSource banco, long codigo) {
        this.banco = banco;
        this.codigo = codigo;
    }

    @Override
    public long codigo() {
        return this.codigo;
    }

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
}
