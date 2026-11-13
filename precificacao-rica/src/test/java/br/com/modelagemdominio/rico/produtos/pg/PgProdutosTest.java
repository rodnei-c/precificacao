package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.categorias.pg.PgCategorias;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.math.BigDecimal;
import br.com.modelagemdominio.rico.produtos.Produtos;
import br.com.modelagemdominio.rico.produtos.NomeProduto;
import br.com.modelagemdominio.rico.produtos.precificacao.ValoresParaPrecificar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
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
  void mesmoObjetoObservaMudancaNoRegistroQueRepresenta() throws Exception {
    Produtos produtos = new PgProdutos(this.banco);
    Produto produto = produtos.produto(this.codigo);
    assertEquals("Produto de consulta", produto.nome());
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement atualizacao = conexao.prepareStatement(
        "UPDATE public.produto SET nome = ? WHERE codigo = ?"
      )) {
      atualizacao.setString(1, "Café d'origem");
      atualizacao.setLong(2, this.codigo);
      atualizacao.executeUpdate();
    }

    assertEquals(this.codigo, produto.codigo());
    assertEquals("Café d'origem", produto.nome());
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

  @Test
  void leituraIndividualAcompanhaVinculoSemAlterarCategoriaJaMaterializada() throws Exception {
    var individual = new PgProduto(this.banco, this.codigo);
    var produtos = new PgProdutosConstantes(this.banco);
    Produto semCategoria = produtos.produto(this.codigo);
    assertTrue(individual.codigoCategoria().isEmpty());
    assertTrue(semCategoria.codigoCategoria().isEmpty());
    var categoria = new CodigoCategoria(new PgCategorias(this.banco)
      .cadastrar(new NomeCategoria("Categoria do vínculo " + UUID.randomUUID())).codigo());
    try (var conexao = this.banco.getConnection();
      var removerVinculo = conexao.prepareStatement("UPDATE public.produto SET categoria_codigo = NULL WHERE codigo = ?");
      var excluirCategoria = conexao.prepareStatement("DELETE FROM public.categoria WHERE codigo = ?")) {
      removerVinculo.setLong(1, this.codigo);
      excluirCategoria.setLong(1, categoria.valor());
      try {
        new PgProdutos(this.banco).vincularCategoria(this.codigo, categoria);
        Produto vinculado = produtos.produto(this.codigo);

        assertEquals(Optional.of(categoria), individual.codigoCategoria());
        assertEquals(Optional.of(categoria), vinculado.codigoCategoria());
        assertTrue(semCategoria.codigoCategoria().isEmpty());

        assertEquals(1, removerVinculo.executeUpdate());

        assertTrue(individual.codigoCategoria().isEmpty());
        assertTrue(produtos.produto(this.codigo).codigoCategoria().isEmpty());
        assertEquals(Optional.of(categoria), vinculado.codigoCategoria());
      } finally {
        removerVinculo.executeUpdate();
        excluirCategoria.executeUpdate();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void categoriaRemovidaAposValidacaoNaoPermiteGravarVinculo(boolean alterar) throws Exception {
    String nome = "Produto sem categoria removida " + UUID.randomUUID();
    long categoria;
    try (var conexao = this.banco.getConnection(); var cadastro = conexao.prepareStatement(
      "INSERT INTO public.categoria (nome) VALUES (?) RETURNING codigo"
    )) {
      cadastro.setString(1, "Categoria removida " + UUID.randomUUID());
      try (var resultado = cadastro.executeQuery()) {
        assertTrue(resultado.next());
        categoria = resultado.getLong(1);
      }
    }
    try {
      Categorias removida = this.categoriaRemovidaAposConsulta();
      Produtos produtos = new PgProdutos(this.banco, codigo -> new PgProduto(this.banco, codigo), removida);
      var precificacao = new ValoresParaPrecificar(new BigDecimal("100"), null, new BigDecimal("20"))
        .precificacao(ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO);
      var referencia = new CodigoCategoria(categoria);

      FalhaProduto falha = assertThrows(FalhaProduto.class, () -> {
        if (alterar) {
          produtos.vincularCategoria(this.codigo, referencia);
        } else {
          produtos.cadastrar(new NomeProduto(nome), precificacao, Optional.of(referencia));
        }
      });
      assertEquals(FalhaProduto.Motivo.CATEGORIA_AUSENTE, falha.motivo());
      var postgres = assertInstanceOf(PSQLException.class, falha.getCause());
      assertEquals("23503", postgres.getSQLState());
      assertEquals("produto_categoria_codigo_fkey", postgres.getServerErrorMessage().getConstraint());
      assertTrue(new PgProdutosConstantes(this.banco).produto(this.codigo).codigoCategoria().isEmpty());
      try (var conexao = this.banco.getConnection(); var consulta = conexao.prepareStatement(
        "SELECT 1 FROM public.produto WHERE nome = ?"
      )) {
        consulta.setString(1, nome);
        try (var resultado = consulta.executeQuery()) {
          assertFalse(resultado.next());
        }
      }
    } finally {
      try (var conexao = this.banco.getConnection();
        var produto = conexao.prepareStatement("DELETE FROM public.produto WHERE nome = ?");
        var exclusao = conexao.prepareStatement("DELETE FROM public.categoria WHERE codigo = ?")) {
        produto.setString(1, nome);
        produto.executeUpdate();
        exclusao.setLong(1, categoria);
        exclusao.executeUpdate();
      }
    }
  }

  private Categorias categoriaRemovidaAposConsulta() {
    return new Categorias() {
      @Override
      public boolean existe(CodigoCategoria codigo) throws FalhaCategoria {
        try (var conexao = banco.getConnection(); var exclusao = conexao.prepareStatement(
          "DELETE FROM public.categoria WHERE codigo = ?"
        )) {
          exclusao.setLong(1, codigo.valor());
          return exclusao.executeUpdate() == 1;
        } catch (SQLException falha) {
          throw new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
        }
      }

      @Override
      public Categoria cadastrar(NomeCategoria nome) {
        throw new AssertionError("A validação deve consultar a categoria.");
      }

      @Override
      public Categoria renomear(CodigoCategoria codigo, NomeCategoria nome) {
        throw new AssertionError("A validação deve consultar a categoria.");
      }

      @Override
      public List<Categoria> listar() {
        throw new AssertionError("A validação deve consultar a categoria.");
      }
    };
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
