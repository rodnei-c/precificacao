package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.clientes.pg.ClientesNaConexao;
import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.Quantidade;
import br.com.modelagemdominio.rico.faturas.ValoresFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.descontos.SemDesconto;
import br.com.modelagemdominio.rico.faturas.pg.PgFaturas;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import br.com.modelagemdominio.rico.produtos.pg.ProdutosNaConexao;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integracao")
class ComposicaoFaturasPostgresTest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void usaColaboradoresRecebidosNaMesmaConexaoDaOperacao(boolean simular) throws Exception {
    try (var cenario = new CenarioFatura()) {
      var produtos = new Observacao(simular);
      var clientes = new Observacao(simular);
      var faturas = new PgFaturas(cenario.sql.banco(),
        new ProdutosObservados(new PgProdutosConstantes(new DataSourceSemConexao()), produtos),
        new ClienteDaOperacao(cenario.cliente, true, clientes));
      var calculo = calculo(cenario);
      var vinculos = new VinculosFatura(Optional.of(cenario.cliente), Optional.of(FormaPagamento.PIX));
      cenario.sql.zerar();

      ValoresFatura valores = simular ? faturas.simular(calculo, vinculos)
        : faturas.cadastrar(calculo, vinculos).valores();

      assertEquals(new BigDecimal("0.50"), valores.total());
      assertEquals(cenario.produtos.get(0).longValue(), valores.itens().get(0).codigoProduto());
      assertEquals(1, produtos.chamadas().get());
      assertEquals(1, clientes.chamadas().get());
      assertSame(produtos.conexao().get(), clientes.conexao().get());
      assertTrue(produtos.conexao().get().isClosed(), "A operação deve encerrar sua própria conexão.");
      assertEquals(1, cenario.sql.conexoes());
      assertEquals(simular ? 1 : 4, cenario.sql.comandos(),
        "O fake de cliente substitui a consulta real; não deve haver outro colaborador criado internamente.");
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void respeitaRecusaDoColaboradorSemConsultarProdutosOuGravar(boolean simular) throws Exception {
    try (var cenario = new CenarioFatura()) {
      var produtos = new Observacao(simular);
      var clientes = new Observacao(simular);
      var faturas = new PgFaturas(cenario.sql.banco(),
        new ProdutosObservados(new PgProdutosConstantes(new DataSourceSemConexao()), produtos),
        new ClienteDaOperacao(cenario.cliente, false, clientes));
      var calculo = calculo(cenario);
      var vinculos = new VinculosFatura(Optional.of(cenario.cliente), Optional.empty());
      cenario.sql.zerar();

      FalhaFatura falha = assertThrows(FalhaFatura.class, () -> {
        if (simular) {
          faturas.simular(calculo, vinculos);
        } else {
          faturas.cadastrar(calculo, vinculos);
        }
      });

      assertEquals(FalhaFatura.Motivo.CLIENTE_AUSENTE, falha.motivo());
      assertEquals(1, clientes.chamadas().get());
      assertEquals(0, produtos.chamadas().get());
      assertTrue(clientes.conexao().get().isClosed());
      assertEquals(1, cenario.sql.conexoes());
      assertEquals(0, cenario.sql.comandos());
      cenario.conferirAusenciaDeGravacao();
    }
  }

  private static CalculoFatura calculo(CenarioFatura cenario) {
    // Consultar esta coleção durante o cálculo falha: a preparação precisa usar o colaborador recebido.
    var produtos = new PgProdutosConstantes(new DataSourceSemConexao());
    return new CalculoFatura(List.of(new ItemFatura(produtos, cenario.produtos.get(0),
      new Quantidade(new BigDecimal("0.5")))), new SemDesconto());
  }

  private record Observacao(boolean consulta, AtomicReference<Connection> conexao, AtomicInteger chamadas) {

    Observacao(boolean consulta) {
      this(consulta, new AtomicReference<>(), new AtomicInteger());
    }

    Connection conferir(Connection atual, boolean consulta) throws SQLException {
      assertEquals(this.consulta, consulta, "Cadastro e simulação devem escolher o modo correto da consulta.");
      assertFalse(atual.isClosed());
      assertFalse(atual.getAutoCommit());
      assertEquals(this.consulta, atual.isReadOnly());
      if (this.consulta) {
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, atual.getTransactionIsolation());
      }
      this.conexao.set(atual);
      this.chamadas.incrementAndGet();
      return atual;
    }
  }

  private record ProdutosObservados(ProdutosNaConexao origem, Observacao observacao) implements ProdutosNaConexao {

    @Override
    public Map<Long, Produto> produtos(Connection conexao, List<Long> codigos) throws SQLException, FalhaProduto {
      return this.origem.produtos(this.observacao.conferir(conexao, false), codigos);
    }

    @Override
    public Map<Long, Produto> produtosParaConsulta(Connection conexao, List<Long> codigos)
      throws SQLException, FalhaProduto {
      return this.origem.produtosParaConsulta(this.observacao.conferir(conexao, true), codigos);
    }
  }

  private record ClienteDaOperacao(long codigo, boolean encontrado, Observacao observacao) implements ClientesNaConexao {

    @Override
    public boolean existe(Connection conexao, long codigo) throws SQLException {
      assertEquals(this.codigo, codigo);
      this.observacao.conferir(conexao, false);
      return this.encontrado;
    }

    @Override
    public boolean existeParaConsulta(Connection conexao, long codigo) throws SQLException {
      assertEquals(this.codigo, codigo);
      this.observacao.conferir(conexao, true);
      return this.encontrado;
    }
  }
}
