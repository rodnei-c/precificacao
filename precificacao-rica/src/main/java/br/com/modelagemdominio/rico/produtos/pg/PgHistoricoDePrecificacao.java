package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.AlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.CodigoProduto;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.HistoricoDePrecificacao;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class PgHistoricoDePrecificacao implements HistoricoDePrecificacao {

  private static final Logger LOG = Logger.getLogger(PgHistoricoDePrecificacao.class.getName());

  private final DataSource banco;

  public PgHistoricoDePrecificacao(DataSource banco) {
    this.banco = banco;
  }

  @Override
  public List<AlteracaoPrecificacao> doProduto(long codigoProduto) throws FalhaProduto {
    var referencia = new CodigoProduto(codigoProduto);
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
      consulta.setLong(1, referencia.valor());
      return new AlteracoesDaConsulta(consulta, referencia.valor()).alteracoes();
    } catch (SQLException falha) {
      LOG.severe("Falha ao consultar o histórico de precificação (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaProduto(FalhaProduto.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }
}
