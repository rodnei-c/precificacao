package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.clientes.AlteracaoCliente;
import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.ClienteConstante;
import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.CodigoCliente;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.Telefone;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class PgClientes implements Clientes, ClientesNaConexao {

  private static final Logger LOG = Logger.getLogger(PgClientes.class.getName());

  private static final String CONSULTA_CLIENTES = "SELECT " + ResultadoSqlDeCliente.COLUNAS + " FROM public.cliente ";

  private final DataSource banco;
  private final Cidades cidades;

  public PgClientes(DataSource banco, Cidades cidades) {
    this.banco = banco;
    this.cidades = cidades;
  }

  @Override
  public Cliente cadastrar(IdentificacaoCliente identificacao, ContatoCliente contato, Optional<Endereco> endereco)
    throws FalhaCliente {
    if (endereco.isPresent()) {
      this.validarCidade(endereco.orElseThrow().codigoCidade());
    }
    try (var conexao = this.banco.getConnection();
      var cadastro = conexao.prepareStatement("""
        INSERT INTO public.cliente
          (nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING codigo
        """)) {
      cadastro.setString(1, identificacao.nome().valor());
      cadastro.setString(2, identificacao.cpf().valor());
      cadastro.setString(3, contato.email().valor());
      cadastro.setString(4, contato.telefone().map(Telefone::valor).orElse(null));
      new ParametrosSqlDoEndereco(endereco).informar(cadastro, 5);
      try (var resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("O cadastro não retornou o código do cliente.");
        }
        return new ClienteConstante(resultado.getLong("codigo"), identificacao, contato, endereco);
      }
    } catch (SQLException falha) {
      throw new FalhaNaGravacaoCliente(falha, LOG).noCadastro();
    }
  }

  @Override
  public Cliente alterar(long codigo, AlteracaoCliente alteracao) throws FalhaCliente {
    var referencia = new CodigoCliente(codigo);
    try (var conexao = this.banco.getConnection()) {
      conexao.setAutoCommit(false);
      try {
        Cliente atualizado = new AtualizacaoDoCliente(conexao).atualizar(referencia.valor(), alteracao);
        conexao.commit();
        return atualizado;
      } catch (SQLException | FalhaCliente | RuntimeException falha) {
        new ReversaoDaAlteracaoCliente(conexao, LOG).desfazer(falha);
        throw falha;
      }
    } catch (SQLException falha) {
      throw new FalhaNaGravacaoCliente(falha, LOG).naAlteracao();
    } catch (IllegalArgumentException falha) {
      LOG.severe("Falha ao materializar cliente após alteração (" + falha.getClass().getSimpleName() + ").");
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public Cliente cliente(long codigo) throws FalhaCliente {
    var referencia = new CodigoCliente(codigo);
    try (var conexao = this.banco.getConnection();
      var consulta = conexao.prepareStatement(CONSULTA_CLIENTES + "WHERE codigo = ?")) {
      consulta.setLong(1, referencia.valor());
      try (var resultado = consulta.executeQuery()) {
        return new ResultadoSqlDeCliente(resultado).encontrado();
      }
    } catch (SQLException | IllegalArgumentException falha) {
      String estado = falha instanceof SQLException sql ? sql.getSQLState() : "não aplicável";
      LOG.severe("Falha ao consultar cliente (" + falha.getClass().getSimpleName() + ", SQLState: " + estado + ").");
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public List<Cliente> listar() throws FalhaCliente {
    try (var conexao = this.banco.getConnection();
      var consulta = conexao.prepareStatement(CONSULTA_CLIENTES + "ORDER BY codigo");
      var resultado = consulta.executeQuery()) {
      var leitura = new ResultadoSqlDeCliente(resultado);
      List<Cliente> clientes = new ArrayList<>();
      while (resultado.next()) {
        clientes.add(leitura.cliente());
      }
      return List.copyOf(clientes);
    } catch (SQLException | IllegalArgumentException falha) {
      String estado = falha instanceof SQLException sql ? sql.getSQLState() : "não aplicável";
      LOG.severe("Falha ao listar clientes (" + falha.getClass().getSimpleName() + ", SQLState: " + estado + ").");
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  /**
   * Verifica e protege a referência na transação do chamador, que mantém a conexão.
   *
   * @param conexao conexão da operação composta
   * @param codigo código positivo do cliente
   * @return se o cliente existe
   * @throws SQLException quando a consulta falhar
   */
  @Override
  public boolean existe(Connection conexao, long codigo) throws SQLException {
    return new ExistenciaDeCliente(conexao).existe(codigo, true);
  }

  /**
   * @param conexao conexão da leitura, pertencente ao chamador
   * @param codigo código positivo do cliente
   * @return se a referência existe, sem bloquear a linha
   * @throws SQLException quando a consulta falhar
   */
  @Override
  public boolean existeParaConsulta(Connection conexao, long codigo) throws SQLException {
    return new ExistenciaDeCliente(conexao).existe(codigo, false);
  }

  private void validarCidade(CodigoCidade codigo) throws FalhaCliente {
    try {
      if (!this.cidades.existe(codigo)) {
        throw new FalhaCliente(FalhaCliente.Motivo.CIDADE_AUSENTE);
      }
    } catch (FalhaCidade falha) {
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }
}
