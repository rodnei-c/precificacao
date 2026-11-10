package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.AlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.HistoricoDePrecificacao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public final class PgHistoricoDePrecificacao implements HistoricoDePrecificacao {

    private final DataSource banco;

    public PgHistoricoDePrecificacao(DataSource banco) {
        this.banco = banco;
    }

    @Override
    public List<AlteracaoPrecificacao> doProduto(long codigoProduto) throws FalhaProduto {
        if (codigoProduto <= 0) {
            throw new FalhaProduto(FalhaProduto.Motivo.CODIGO_INVALIDO);
        }
        try (var conexao = this.banco.getConnection();
             var consulta = conexao.prepareStatement("""
                 SELECT h.codigo, h.data_alteracao,
                        h.custo_anterior, h.preco_venda_anterior, h.percentual_anterior,
                        h.custo_novo, h.preco_venda_novo, h.percentual_novo
                 FROM public.produto p
                 LEFT JOIN public.historico_precificacao h ON h.produto_codigo = p.codigo
                 WHERE p.codigo = ?
                 ORDER BY h.codigo DESC
                 """)) {
            consulta.setLong(1, codigoProduto);
            try (var resultado = consulta.executeQuery()) {
                return alteracoes(resultado, codigoProduto);
            }
        } catch (SQLException falha) {
            throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
        }
    }

    private static List<AlteracaoPrecificacao> alteracoes(ResultSet resultado, long codigoProduto)
        throws SQLException, FalhaProduto {
        if (!resultado.next()) {
            throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
        }
        if (resultado.getObject("codigo", Long.class) == null) {
            return List.of();
        }
        var alteracoes = new ArrayList<AlteracaoPrecificacao>();
        do {
            alteracoes.add(new AlteracaoPrecificacao(
                resultado.getLong("codigo"), codigoProduto,
                resultado.getObject("data_alteracao", LocalDateTime.class),
                new AlteracaoPrecificacao.Valores(
                    resultado.getBigDecimal("custo_anterior"),
                    resultado.getBigDecimal("preco_venda_anterior"),
                    resultado.getBigDecimal("percentual_anterior")
                ),
                new AlteracaoPrecificacao.Valores(
                    resultado.getBigDecimal("custo_novo"),
                    resultado.getBigDecimal("preco_venda_novo"),
                    resultado.getBigDecimal("percentual_novo")
                )
            ));
        } while (resultado.next());
        return List.copyOf(alteracoes);
    }
}
