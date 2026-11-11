package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.pg.ClientesNaConexao;
import br.com.modelagemdominio.rico.clientes.pg.PgClientes;
import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.Faturas;
import br.com.modelagemdominio.rico.faturas.FiltrosFaturas;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.ResumoFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.ValoresFatura;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.produtos.pg.ProdutosNaConexao;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;

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
        long codigo = new CadastroDoCabecalhoDaFatura(conexao).cadastrar(preparado, vinculos);
        new CadastroDosItensDaFatura(conexao).cadastrar(codigo, preparado.itens());
        Fatura fatura;
        try {
          fatura = new ConsultaSqlDeFaturas(this.banco).fatura(conexao, codigo);
          fatura.valores();
        } catch (FalhaFatura | IllegalArgumentException falha) {
          throw new FalhaNaLeituraDeFatura(falha).traduzida();
        }
        conexao.commit();
        return fatura;
      } catch (SQLException | FalhaFatura | RuntimeException falha) {
        new ReversaoDaOperacaoDeFatura(conexao, LOG).desfazer(falha);
        throw falha;
      }
    } catch (SQLException falha) {
      throw new FalhaNaGravacaoDeFatura(falha, LOG).traduzida();
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
        new ReversaoDaOperacaoDeFatura(conexao, LOG).desfazer(falha);
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
      throw new FalhaNaLeituraDeFatura(falha).traduzida();
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
      throw new FalhaNaLeituraDeFatura(falha).traduzida();
    }
  }
}
