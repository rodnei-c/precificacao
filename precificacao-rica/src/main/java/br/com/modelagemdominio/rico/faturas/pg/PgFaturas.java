package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.pg.ClientesNaConexao;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.ResumoFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.ValoresFatura;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoContratado;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.produtos.pg.ProdutosNaConexao;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.postgresql.util.PSQLException;

/** Coleção de faturas com gravação de cabeçalho e itens em uma única transação. */
public final class PgFaturas implements Faturas {

  private static final Logger LOG = Logger.getLogger(PgFaturas.class.getName());

  private final DataSource banco;
  private final ProdutosNaConexao produtos;
  private final ClientesNaConexao clientes;

  /** @param banco fonte de conexões; a construção não acessa o banco */
  public PgFaturas(DataSource banco) {
    this(banco, new PgProdutosConstantes(banco), new PgClientes(banco, new PgCidades(banco)));
  }

  /**
   * Concentra a composição sem executar consultas ou criar colaboradores.
   *
   * @param banco fonte da conexão que pertence à operação da fatura
   * @param produtos consulta de produtos na conexão fornecida pela fatura
   * @param clientes consulta de referências na conexão fornecida pela fatura
   */
  public PgFaturas(DataSource banco, ProdutosNaConexao produtos, ClientesNaConexao clientes) {
    this.banco = Objects.requireNonNull(banco);
    this.produtos = Objects.requireNonNull(produtos);
    this.clientes = Objects.requireNonNull(clientes);
  }

  @Override
  public Fatura cadastrar(CalculoFatura calculo, VinculosFatura vinculos) throws FalhaFatura {
    Objects.requireNonNull(calculo);
    Objects.requireNonNull(vinculos);
    try (var conexao = this.banco.getConnection()) {
      conexao.setAutoCommit(false);
      try {
        CalculoFatura preparado = this.preparar(conexao, calculo, vinculos, true);
        preparado.preparar();
        long codigo = cadastrarCabecalho(conexao, preparado, vinculos);
        cadastrarItens(conexao, codigo, preparado.itens());
        Fatura fatura;
        try {
          fatura = new ConsultaSqlDeFaturas(this.banco).fatura(conexao, codigo);
          fatura.valores();
        } catch (FalhaFatura | IllegalArgumentException falha) {
          throw falhaLeitura(falha);
        }
        conexao.commit();
        return fatura;
      } catch (SQLException | FalhaFatura | RuntimeException falha) {
        desfazer(conexao, falha);
        throw falha;
      }
    } catch (SQLException falha) {
      throw falhaSql(falha);
    }
  }

  @Override
  public ValoresFatura simular(CalculoFatura calculo, VinculosFatura vinculos) throws FalhaFatura {
    Objects.requireNonNull(calculo);
    Objects.requireNonNull(vinculos);
    try (var conexao = this.banco.getConnection()) {
      conexao.setReadOnly(true);
      conexao.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      conexao.setAutoCommit(false);
      try {
        ValoresFatura valores = this.preparar(conexao, calculo, vinculos, false).preparar();
        conexao.commit();
        return valores;
      } catch (SQLException | FalhaFatura | RuntimeException falha) {
        desfazer(conexao, falha);
        throw falha;
      }
    } catch (SQLException falha) {
      LOG.severe("Falha ao simular fatura (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaFatura(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  private CalculoFatura preparar(Connection conexao, CalculoFatura calculo, VinculosFatura vinculos, boolean proteger)
    throws SQLException, FalhaFatura {
    if (vinculos.codigoCliente().isPresent()
      && !(proteger ? this.clientes.existe(conexao, vinculos.codigoCliente().orElseThrow())
        : this.clientes.existeParaConsulta(conexao, vinculos.codigoCliente().orElseThrow()))) {
      throw new FalhaFatura(FalhaFatura.Motivo.CLIENTE_AUSENTE);
    }
    try {
      var codigos = calculo.itens().stream().map(ItemFatura::codigoProduto).toList();
      var produtos = proteger ? this.produtos.produtos(conexao, codigos)
        : this.produtos.produtosParaConsulta(conexao, codigos);
      var itens = calculo.itens().stream()
        .map(item -> new ItemFatura(produtos.get(item.codigoProduto()), item.quantidade())).toList();
      return new CalculoFatura(itens, calculo.desconto());
    } catch (FalhaProduto falha) {
      throw new FalhaFatura(falha.motivo() == FalhaProduto.Motivo.PRODUTO_AUSENTE
        ? FalhaFatura.Motivo.PRODUTO_AUSENTE : FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    } catch (IllegalArgumentException falha) {
      throw falhaLeitura(falha);
    }
  }

  private static long cadastrarCabecalho(Connection conexao, CalculoFatura calculo, VinculosFatura vinculos)
    throws SQLException {
    try (var cadastro = conexao.prepareStatement("""
      INSERT INTO public.fatura (tipo_desconto, valor_desconto, cliente_codigo, forma_pagamento)
      VALUES (?, ?, ?, ?) RETURNING id
      """)) {
      var desconto = calculo.desconto().registro();
      cadastro.setString(1, desconto.map(DescontoContratado::tipo).map(Enum::name).orElse(null));
      cadastro.setBigDecimal(2, desconto.map(DescontoContratado::valor).orElse(null));
      cadastro.setObject(3, vinculos.codigoCliente().orElse(null), Types.BIGINT);
      cadastro.setString(4, vinculos.formaPagamento().map(FormaPagamento::name).orElse(null));
      try (var resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("O cadastro não retornou o código da fatura.");
        }
        return resultado.getLong("id");
      }
    }
  }

  private static void cadastrarItens(Connection conexao, long codigo, List<ItemFatura> itens) throws SQLException {
    try (var cadastro = conexao.prepareStatement("""
      INSERT INTO public.item_fatura (fatura_id, produto_codigo, quantidade, posicao) VALUES (?, ?, ?, ?)
      """)) {
      for (int posicao = 0; posicao < itens.size(); posicao++) {
        ItemFatura item = itens.get(posicao);
        cadastro.setLong(1, codigo);
        cadastro.setLong(2, item.codigoProduto());
        cadastro.setBigDecimal(3, item.quantidade().valor());
        cadastro.setInt(4, posicao);
        cadastro.addBatch();
      }
      int[] gravados = cadastro.executeBatch();
      if (gravados.length != itens.size()) {
        throw new SQLException("A gravação não retornou todos os itens da fatura.");
      }
      for (int gravado : gravados) {
        if (gravado != 1 && gravado != Statement.SUCCESS_NO_INFO) {
          throw new SQLException("Um item da fatura não foi gravado.");
        }
      }
    }
  }

  @Override
  public Fatura fatura(long codigo) throws FalhaFatura {
    return new PgFatura(this.banco, codigo).materializada();
  }

  @Override
  public List<ResumoFatura> listar(FiltrosFaturas filtros) throws FalhaFatura {
    Objects.requireNonNull(filtros);
    try {
      return new ConsultaSqlDeResumosFaturas(this.banco).listar(filtros);
    } catch (SQLException | IllegalArgumentException falha) {
      throw falhaLeitura(falha);
    }
  }

  static FalhaFatura falhaLeitura(Exception falha) {
    String estado = falha instanceof SQLException sql ? sql.getSQLState() : "não aplicável";
    LOG.severe("Falha ao ler fatura (" + falha.getClass().getSimpleName() + ", SQLState: " + estado + ").");
    return new FalhaFatura(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
  }

  private static FalhaFatura falhaSql(SQLException falha) {
    for (SQLException causa = falha; causa != null; causa = causa.getNextException()) {
      if (causa instanceof PSQLException postgres && "23503".equals(postgres.getSQLState())
        && postgres.getServerErrorMessage() != null) {
        String restricao = postgres.getServerErrorMessage().getConstraint();
        if ("fatura_cliente_codigo_fkey".equals(restricao)) {
          return new FalhaFatura(FalhaFatura.Motivo.CLIENTE_AUSENTE, falha);
        }
        if ("item_fatura_produto_codigo_fkey".equals(restricao)) {
          return new FalhaFatura(FalhaFatura.Motivo.PRODUTO_AUSENTE, falha);
        }
      }
    }
    LOG.severe("Falha ao cadastrar fatura (SQLState: " + falha.getSQLState() + ").");
    return new FalhaFatura(FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
  }

  private static void desfazer(Connection conexao, Throwable falha) {
    try {
      conexao.rollback();
    } catch (SQLException falhaAoDesfazer) {
      LOG.severe("Falha ao desfazer operação de fatura (SQLState: " + falhaAoDesfazer.getSQLState() + ").");
      falha.addSuppressed(falhaAoDesfazer);
    }
  }
}
