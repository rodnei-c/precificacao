package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.Dinheiro;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Percentual;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import java.sql.ResultSet;
import java.sql.SQLException;

final class ResultadoSqlDePrecificacao {

    private final ResultSet resultado;

    ResultadoSqlDePrecificacao(ResultSet resultado) {
        this.resultado = resultado;
    }

    Precificacao precificacao() throws SQLException {
        try {
            return new Precificacao(
                new Dinheiro(this.resultado.getBigDecimal("custo")),
                new Dinheiro(this.resultado.getBigDecimal("preco_venda")),
                new Percentual(this.resultado.getBigDecimal("percentual")),
                ModalidadePrecificacao.valueOf(this.resultado.getString("modalidade_precificacao"))
            );
        } catch (IllegalArgumentException falha) {
            throw new SQLException("Precificação inválida no banco.", falha);
        }
    }
}
