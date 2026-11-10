package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.pg.PgCidades;
import br.com.modelagemdominio.rico.clientes.AlteracaoCliente;
import br.com.modelagemdominio.rico.clientes.Cep;
import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.ClienteConstante;
import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Cpf;
import br.com.modelagemdominio.rico.clientes.Email;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.LocalizacaoEndereco;
import br.com.modelagemdominio.rico.clientes.NomeCliente;
import br.com.modelagemdominio.rico.clientes.Telefone;
import br.com.modelagemdominio.rico.clientes.TelefoneOpcional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

public final class PgClientes implements Clientes, ClientesNaConexao {

  private static final Logger LOG = Logger.getLogger(PgClientes.class.getName());

  private static final String COLUNAS_CLIENTE =
    "codigo, nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo";

  private static final String CONSULTA_CLIENTES = "SELECT " + COLUNAS_CLIENTE + " FROM public.cliente ";

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
      preencherEndereco(cadastro, endereco, 5);
      try (var resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new SQLException("O cadastro não retornou o código do cliente.");
        }
        return new ClienteConstante(resultado.getLong("codigo"), identificacao, contato, endereco);
      }
    } catch (SQLException falha) {
      throw falhaAoCadastrar(falha);
    }
  }

  @Override
  public Cliente alterar(long codigo, AlteracaoCliente alteracao) throws FalhaCliente {
    validarCodigo(codigo);
    try (var conexao = this.banco.getConnection()) {
      conexao.setAutoCommit(false);
      try {
        if (alteracao.endereco().valor().isPresent()
          && !new PgCidades(this.banco).existe(conexao, alteracao.endereco().valor().orElseThrow().codigoCidade())) {
          throw new FalhaCliente(FalhaCliente.Motivo.CIDADE_AUSENTE);
        }
        Cliente atualizado = atualizar(conexao, codigo, alteracao);
        conexao.commit();
        return atualizado;
      } catch (SQLException | FalhaCliente | RuntimeException falha) {
        desfazer(conexao, falha);
        throw falha;
      }
    } catch (SQLException falha) {
      if (violaRestricao(falha, PSQLState.UNIQUE_VIOLATION, "ux_cliente_email")) {
        throw new FalhaCliente(FalhaCliente.Motivo.EMAIL_DUPLICADO, falha);
      }
      if (violaRestricao(falha, PSQLState.FOREIGN_KEY_VIOLATION, "cliente_cidade_codigo_fkey")) {
        throw new FalhaCliente(FalhaCliente.Motivo.CIDADE_AUSENTE, falha);
      }
      LOG.severe("Falha ao alterar cliente (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    } catch (IllegalArgumentException falha) {
      LOG.severe("Falha ao materializar cliente após alteração (" + falha.getClass().getSimpleName() + ").");
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  private static Cliente atualizar(Connection conexao, long codigo, AlteracaoCliente alteracao)
    throws SQLException, FalhaCliente {
    var campos = new LinkedHashMap<String, String>();
    alteracao.email().ifPresent(email -> campos.put("email", email.valor()));
    alteracao.telefone().aplicar(campos, telefone -> {
      campos.put("telefone", telefone.map(Telefone::valor).orElse(null));
      return campos;
    });
    String atribuicoes = campos.keySet().stream()
      .map(campo -> campo + " = ?")
      .collect(Collectors.joining(", "));
    if (alteracao.endereco().informada()) {
      String endereco = "cep = ?, logradouro = ?, numero = ?, bairro = ?, complemento = ?, cidade_codigo = ?";
      atribuicoes = atribuicoes.isEmpty() ? endereco : atribuicoes + ", " + endereco;
    }
    try (var atualizacao = conexao.prepareStatement(
      "UPDATE public.cliente SET " + atribuicoes + " WHERE codigo = ? RETURNING " + COLUNAS_CLIENTE
    )) {
      int indice = 1;
      for (String valor : campos.values()) {
        atualizacao.setString(indice++, valor);
      }
      if (alteracao.endereco().informada()) {
        preencherEndereco(atualizacao, alteracao.endereco().valor(), indice);
        indice += 6;
      }
      atualizacao.setLong(indice, codigo);
      try (var resultado = atualizacao.executeQuery()) {
        return clienteEncontrado(resultado);
      }
    }
  }

  private static void desfazer(Connection conexao, Throwable falha) {
    try {
      conexao.rollback();
    } catch (SQLException falhaAoDesfazer) {
      LOG.severe("Falha ao desfazer alteração do cliente (SQLState: " + falhaAoDesfazer.getSQLState() + ").");
      falha.addSuppressed(falhaAoDesfazer);
    }
  }

  @Override
  public Cliente cliente(long codigo) throws FalhaCliente {
    validarCodigo(codigo);
    try (var conexao = this.banco.getConnection();
      var consulta = conexao.prepareStatement(CONSULTA_CLIENTES + "WHERE codigo = ?")) {
      consulta.setLong(1, codigo);
      try (var resultado = consulta.executeQuery()) {
        return clienteEncontrado(resultado);
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
      List<Cliente> clientes = new ArrayList<>();
      while (resultado.next()) {
        clientes.add(clienteDaLinha(resultado));
      }
      return List.copyOf(clientes);
    } catch (SQLException | IllegalArgumentException falha) {
      String estado = falha instanceof SQLException sql ? sql.getSQLState() : "não aplicável";
      LOG.severe("Falha ao listar clientes (" + falha.getClass().getSimpleName() + ", SQLState: " + estado + ").");
      throw new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  private static void validarCodigo(long codigo) throws FalhaCliente {
    if (codigo <= 0) {
      throw new FalhaCliente(FalhaCliente.Motivo.CODIGO_INVALIDO);
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
    return existe(conexao, codigo, true);
  }

  /**
   * @param conexao conexão da leitura, pertencente ao chamador
   * @param codigo código positivo do cliente
   * @return se a referência existe, sem bloquear a linha
   * @throws SQLException quando a consulta falhar
   */
  @Override
  public boolean existeParaConsulta(Connection conexao, long codigo) throws SQLException {
    return existe(conexao, codigo, false);
  }

  private static boolean existe(Connection conexao, long codigo, boolean proteger) throws SQLException {
    String bloqueio = proteger ? " FOR KEY SHARE" : "";
    try (var consulta = conexao.prepareStatement("SELECT 1 FROM public.cliente WHERE codigo = ?" + bloqueio)) {
      consulta.setLong(1, codigo);
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }

  private static Cliente clienteEncontrado(ResultSet resultado) throws SQLException, FalhaCliente {
    if (!resultado.next()) {
      throw new FalhaCliente(FalhaCliente.Motivo.CLIENTE_AUSENTE);
    }
    return clienteDaLinha(resultado);
  }

  private static Cliente clienteDaLinha(ResultSet resultado) throws SQLException {
    var identificacao = new IdentificacaoCliente(
      new NomeCliente(resultado.getString("nome")), new Cpf(resultado.getString("cpf"))
    );
    var contato = new ContatoCliente(
      new Email(resultado.getString("email")), new TelefoneOpcional(resultado.getString("telefone")).valor()
    );
    return new ClienteConstante(resultado.getLong("codigo"), identificacao, contato, enderecoDaLinha(resultado));
  }

  private static Optional<Endereco> enderecoDaLinha(ResultSet resultado) throws SQLException {
    Long codigoCidade = resultado.getObject("cidade_codigo", Long.class);
    if (codigoCidade == null) {
      return Optional.empty();
    }
    var localizacao = new LocalizacaoEndereco(
      resultado.getString("logradouro"), resultado.getString("numero"), resultado.getString("bairro"),
      Optional.ofNullable(resultado.getString("complemento"))
    );
    return Optional.of(new Endereco(new Cep(resultado.getString("cep")), localizacao, new CodigoCidade(codigoCidade)));
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

  private static void preencherEndereco(PreparedStatement cadastro, Optional<Endereco> endereco, int inicio)
    throws SQLException {
    var localizacao = endereco.map(Endereco::localizacao);
    cadastro.setString(inicio, endereco.map(Endereco::cep).map(Cep::valor).orElse(null));
    cadastro.setString(inicio + 1, localizacao.map(LocalizacaoEndereco::logradouro).orElse(null));
    cadastro.setString(inicio + 2, localizacao.map(LocalizacaoEndereco::numero).orElse(null));
    cadastro.setString(inicio + 3, localizacao.map(LocalizacaoEndereco::bairro).orElse(null));
    cadastro.setString(inicio + 4, localizacao.flatMap(LocalizacaoEndereco::complemento).orElse(null));
    cadastro.setObject(inicio + 5, endereco.map(Endereco::codigoCidade).map(CodigoCidade::valor).orElse(null), Types.BIGINT);
  }

  private static FalhaCliente falhaAoCadastrar(SQLException falha) {
    if (violaRestricao(falha, PSQLState.UNIQUE_VIOLATION, "cliente_cpf_key")
      || violaRestricao(falha, PSQLState.UNIQUE_VIOLATION, "ux_cliente_email")) {
      return new FalhaCliente(FalhaCliente.Motivo.CPF_OU_EMAIL_DUPLICADO, falha);
    }
    if (violaRestricao(falha, PSQLState.FOREIGN_KEY_VIOLATION, "cliente_cidade_codigo_fkey")) {
      return new FalhaCliente(FalhaCliente.Motivo.CIDADE_AUSENTE, falha);
    }
    LOG.severe("Falha ao cadastrar cliente (SQLState: " + falha.getSQLState() + ").");
    return new FalhaCliente(FalhaCliente.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
  }

  private static boolean violaRestricao(SQLException falha, PSQLState estado, String restricao) {
    return falha instanceof PSQLException postgres
      && estado.getState().equals(postgres.getSQLState())
      && postgres.getServerErrorMessage() != null
      && restricao.equals(postgres.getServerErrorMessage().getConstraint());
  }
}
