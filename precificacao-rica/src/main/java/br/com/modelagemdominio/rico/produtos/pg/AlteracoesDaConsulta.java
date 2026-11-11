package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.AlteracaoPrecificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Executa a consulta preparada e materializa o histórico do produto. */
final class AlteracoesDaConsulta {

  private final PreparedStatement consulta;
  private final long codigoProduto;

  AlteracoesDaConsulta(PreparedStatement consulta, long codigoProduto) {
    this.consulta = consulta;
    this.codigoProduto = codigoProduto;
  }

  List<AlteracaoPrecificacao> alteracoes() throws SQLException, FalhaProduto {
    try (ResultSet resultado = this.consulta.executeQuery()) {
      if (!resultado.next()) {
        throw new FalhaProduto(FalhaProduto.Motivo.PRODUTO_AUSENTE);
      }
      if (resultado.getObject("codigo", Long.class) == null) {
        return List.of();
      }
      var alteracoes = new ArrayList<AlteracaoPrecificacao>();
      do {
        alteracoes.add(this.alteracao(resultado));
      } while (resultado.next());
      return List.copyOf(alteracoes);
    }
  }

  private AlteracaoPrecificacao alteracao(ResultSet resultado) throws SQLException {
    return new AlteracaoPrecificacao(
      new AlteracaoPrecificacao.Identidade(resultado.getLong("codigo"), this.codigoProduto),
      resultado.getObject("data_alteracao", LocalDateTime.class),
      new AlteracaoPrecificacao.Valores(
        resultado.getBigDecimal("custo_anterior"), resultado.getBigDecimal("preco_venda_anterior"),
        resultado.getBigDecimal("percentual_anterior")
      ),
      new AlteracaoPrecificacao.Valores(
        resultado.getBigDecimal("custo_novo"), resultado.getBigDecimal("preco_venda_novo"),
        resultado.getBigDecimal("percentual_novo")
      )
    );
  }
}
