package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PgConstrucaoTest {

    @Test
    void constroiProdutoSemAcessarOBanco() {
        Produto produto = assertDoesNotThrow(() -> new PgProduto(new DataSourceSemConexao(), 42));

        assertEquals(42, produto.codigo());
    }

    @Test
    void constroiColecaoSemAcessarOBanco() {
        assertDoesNotThrow(() -> new PgProdutos(new DataSourceSemConexao()));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejeitaCodigoInvalidoAntesDeAcessarOBanco(long codigo) {
        Produtos produtos = new PgProdutos(new DataSourceSemConexao());

        FalhaProduto falha = assertThrows(FalhaProduto.class, () -> produtos.produto(codigo));

        assertEquals(FalhaProduto.Motivo.CODIGO_INVALIDO, falha.motivo());
    }
}
