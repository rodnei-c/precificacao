package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.ProdutoConstante;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.LongFunction;

final class ResultadoSqlDeProduto {

    private final ResultSet resultado;
    private final LongFunction<Produto> produtoPorCodigo;

    ResultadoSqlDeProduto(ResultSet resultado, LongFunction<Produto> produtoPorCodigo) {
        this.resultado = resultado;
        this.produtoPorCodigo = produtoPorCodigo;
    }

    ProdutoConstante produto() throws SQLException {
        return new ProdutoConstante(
            this.produtoPorCodigo.apply(this.resultado.getLong("codigo")),
            this.resultado.getString("nome"),
            new ResultadoSqlDePrecificacao(this.resultado).precificacao(),
            codigoCategoria(this.resultado)
        );
    }

    static Optional<CodigoCategoria> codigoCategoria(ResultSet resultado) throws SQLException {
        return Optional.ofNullable(resultado.getObject("categoria_codigo", Long.class)).map(CodigoCategoria::new);
    }
}
