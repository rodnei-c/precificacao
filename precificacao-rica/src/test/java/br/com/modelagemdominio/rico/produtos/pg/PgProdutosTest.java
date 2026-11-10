package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.math.BigDecimal;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
class PgProdutosTest {

    private DataSource banco;
    private long codigo;

    @BeforeEach
    void cadastrarProdutoDeTeste() throws SQLException {
        PGSimpleDataSource conexoes = new PGSimpleDataSource();
        conexoes.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        conexoes.setUser(System.getenv("RICO_BANCO_TESTE_USUARIO"));
        conexoes.setPassword(System.getenv("RICO_BANCO_TESTE_SENHA"));
        this.banco = conexoes;
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement cadastro = conexao.prepareStatement(
                 "INSERT INTO public.produto"
                     + " (nome, custo, preco_venda, percentual, modalidade_precificacao)"
                     + " VALUES (?, 100, 120, 20, 'ACRESCIMO_SOBRE_CUSTO') RETURNING codigo"
             )) {
            cadastro.setString(1, "Produto de consulta");
            try (ResultSet resultado = cadastro.executeQuery()) {
                resultado.next();
                this.codigo = resultado.getLong("codigo");
            }
        }
    }

    @AfterEach
    void removerProdutoDeTeste() throws SQLException {
        if (this.codigo > 0) {
            try (Connection conexao = this.banco.getConnection();
                 PreparedStatement exclusao = conexao.prepareStatement(
                     "DELETE FROM public.produto WHERE codigo = ?"
                 )) {
                exclusao.setLong(1, this.codigo);
                exclusao.executeUpdate();
            }
        }
    }

    @Test
    void consultaProdutoPelaInterfaceDaColecao() throws Exception {
        Produtos produtos = new PgProdutos(this.banco);

        Produto produto = produtos.produto(this.codigo);

        assertEquals(this.codigo, produto.codigo());
        assertEquals("Produto de consulta", produto.nome());
    }

    @Test
    void consultaNomeComAcentosEApostrofo() throws Exception {
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement atualizacao = conexao.prepareStatement(
                 "UPDATE public.produto SET nome = ? WHERE codigo = ?"
             )) {
            atualizacao.setString(1, "Café d'origem");
            atualizacao.setLong(2, this.codigo);
            atualizacao.executeUpdate();
        }

        assertEquals("Café d'origem", new PgProdutos(this.banco).produto(this.codigo).nome());
    }

    @Test
    void mesmoObjetoObservaMudancaNoRegistroQueRepresenta() throws Exception {
        Produto produto = new PgProdutos(this.banco).produto(this.codigo);
        assertEquals("Produto de consulta", produto.nome());
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement atualizacao = conexao.prepareStatement(
                 "UPDATE public.produto SET nome = ? WHERE codigo = ?"
             )) {
            atualizacao.setString(1, "Produto atualizado");
            atualizacao.setLong(2, this.codigo);
            atualizacao.executeUpdate();
        }

        assertEquals(this.codigo, produto.codigo());
        assertEquals("Produto atualizado", produto.nome());
    }

    @Test
    void informaAusenciaSemRetornarNulo() {
        Produtos produtos = new PgProdutos(this.banco);

        FalhaProduto falha = assertThrows(FalhaProduto.class,
            () -> produtos.produto(Long.MAX_VALUE));

        assertEquals(FalhaProduto.Motivo.PRODUTO_AUSENTE, falha.motivo());
    }

    @Test
    void objetoIndividualTambemInformaRegistroAusente() {
        Produto produto = new PgProduto(this.banco, Long.MAX_VALUE);

        FalhaProduto falha = assertThrows(FalhaProduto.class, produto::nome);

        assertEquals(FalhaProduto.Motivo.PRODUTO_AUSENTE, falha.motivo());
    }

    @Test
    void permiteSubstituirAConstrucaoDoObjetoRetornado() throws Exception {
        Produtos produtos = new PgProdutos(this.banco, identificacao -> new Produto() {
            @Override
            public long codigo() {
                return identificacao;
            }

            @Override
            public String nome() {
                return "Produto alternativo";
            }

            @Override
            public Precificacao precificacao() throws FalhaProduto {
                return new PgProduto(PgProdutosTest.this.banco, identificacao).precificacao();
            }

            @Override
            public Optional<CodigoCategoria> codigoCategoria() throws FalhaProduto {
                return new PgProduto(PgProdutosTest.this.banco, identificacao).codigoCategoria();
            }
        });

        Produto produto = produtos.produto(this.codigo);

        assertEquals(this.codigo, produto.codigo());
        assertEquals("Produto alternativo", produto.nome());
    }

    @Test
    void consultaConstantePreservaOsValoresLidos() throws Exception {
        Produtos produtos = new PgProdutosConstantes(this.banco);
        Produto produto = produtos.produto(this.codigo);
        Precificacao precificacao = produto.precificacao();

        assertEquals(this.codigo, produto.codigo());
        assertEquals(new BigDecimal("100.00"), precificacao.custo().valor());
        assertEquals(new BigDecimal("120.00"), precificacao.precoVenda().valor());
        assertEquals(new BigDecimal("20.0000"), precificacao.percentual().valor());
        assertEquals(ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO, precificacao.modalidade());

        try (Connection conexao = this.banco.getConnection();
             PreparedStatement atualizacao = conexao.prepareStatement("""
                 UPDATE public.produto SET nome = 'Produto atualizado', preco_venda = 125, percentual = 25
                 WHERE codigo = ?
                 """)) {
            atualizacao.setLong(1, this.codigo);
            atualizacao.executeUpdate();
        }

        assertEquals("Produto de consulta", produto.nome());
        assertEquals(precificacao, produto.precificacao());
        Produto atualizado = produtos.produto(this.codigo);
        assertEquals("Produto atualizado", atualizado.nome());
        assertEquals(new BigDecimal("125.00"), atualizado.precificacao().precoVenda().valor());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "custo = 'NaN'::numeric, preco_venda = 'NaN'::numeric, percentual = 0",
        "preco_venda = 'NaN'::numeric",
        "percentual = 'NaN'::numeric"
    })
    void rejeitaNaNPelaRestricaoDaPrecificacao(String valores) throws SQLException {
        try (Connection conexao = this.banco.getConnection();
             PreparedStatement atualizacao = conexao.prepareStatement(
                 "UPDATE public.produto SET " + valores + " WHERE codigo = ?"
             )) {
            atualizacao.setLong(1, this.codigo);
            PSQLException falha = assertThrows(PSQLException.class, atualizacao::executeUpdate);
            assertEquals("23514", falha.getSQLState());
            assertEquals("ck_produto_precificacao_sem_nan", falha.getServerErrorMessage().getConstraint());
        }
    }
}
