package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.enums.CampoCliente;
import br.com.precificacao.anemico.models.Cliente;
import br.com.precificacao.anemico.models.Endereco;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClienteRepository implements ClienteRepository {

  private static final String COLUNAS =
    "codigo, nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo";
  private static final String CONSULTA_POR_CODIGO = "SELECT " + COLUNAS + " FROM cliente WHERE codigo = ?";

  private final JdbcTemplate jdbcTemplate;

  public JdbcClienteRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Cliente> inserir(Cliente cliente) {
    List<Object> parametros = new ArrayList<>(Arrays.asList(
      cliente.nome(), cliente.cpf(), cliente.email(), cliente.telefone()
    ));
    parametros.addAll(valoresDoEndereco(cliente.endereco()));
    return jdbcTemplate.query("""
      INSERT INTO cliente (nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT DO NOTHING
      RETURNING
      """ + COLUNAS, this::mapear, parametros.toArray()
    ).stream().findFirst();
  }

  @Override
  public List<Cliente> listar() {
    return jdbcTemplate.query("SELECT " + COLUNAS + " FROM cliente ORDER BY codigo", this::mapear);
  }

  /** Consulta a chave primária e reutiliza o mapeamento do cadastro e da listagem. */
  @Override
  public Optional<Cliente> buscarPorCodigo(Long codigo) {
    return jdbcTemplate.query(CONSULTA_POR_CODIGO, this::mapear, codigo).stream().findFirst();
  }

  /** Protege a leitura usada na montagem de uma alteração parcial. */
  @Override
  public Optional<Cliente> buscarPorCodigoParaAtualizacao(Long codigo) {
    return jdbcTemplate.query(CONSULTA_POR_CODIGO + " FOR UPDATE", this::mapear, codigo)
      .stream().findFirst();
  }

  /** Inclui no SQL somente os campos selecionados; todos os valores usam parâmetros. */
  @Override
  public Optional<Cliente> atualizar(Cliente cliente, Set<CampoCliente> camposInformados) {
    if (camposInformados == null || camposInformados.isEmpty()) {
      throw new IllegalArgumentException("A atualização precisa indicar ao menos um campo.");
    }
    List<String> atribuicoes = new ArrayList<>();
    List<Object> parametros = new ArrayList<>();
    if (camposInformados.contains(CampoCliente.EMAIL)) {
      atribuicoes.add("email = ?");
      parametros.add(cliente.email());
    }
    if (camposInformados.contains(CampoCliente.TELEFONE)) {
      atribuicoes.add("telefone = ?");
      parametros.add(cliente.telefone());
    }
    if (camposInformados.contains(CampoCliente.ENDERECO)) {
      atribuicoes.addAll(List.of(
        "cep = ?", "logradouro = ?", "numero = ?", "bairro = ?", "complemento = ?", "cidade_codigo = ?"
      ));
      parametros.addAll(valoresDoEndereco(cliente.endereco()));
    }
    parametros.add(cliente.codigo());
    return jdbcTemplate.query(
      "UPDATE cliente SET " + String.join(", ", atribuicoes) + " WHERE codigo = ? RETURNING " + COLUNAS,
      this::mapear, parametros.toArray()
    ).stream().findFirst();
  }

  @Override
  public boolean existePorCodigo(Long codigo) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
      "SELECT EXISTS (SELECT 1 FROM cliente WHERE codigo = ?)", Boolean.class, codigo
    ));
  }

  private List<Object> valoresDoEndereco(Endereco endereco) {
    return Arrays.asList(
      endereco == null ? null : endereco.cep(), endereco == null ? null : endereco.logradouro(),
      endereco == null ? null : endereco.numero(), endereco == null ? null : endereco.bairro(),
      endereco == null ? null : endereco.complemento(), endereco == null ? null : endereco.codigoCidade()
    );
  }

  private Cliente mapear(ResultSet resultado, int linha) throws SQLException {
    Long codigoCidade = resultado.getObject("cidade_codigo", Long.class);
    Endereco endereco = codigoCidade == null ? null : new Endereco(
      resultado.getString("cep"), resultado.getString("logradouro"), resultado.getString("numero"),
      resultado.getString("bairro"), resultado.getString("complemento"), codigoCidade
    );
    return new Cliente(
      resultado.getLong("codigo"), resultado.getString("nome"), resultado.getString("cpf"),
      resultado.getString("email"), resultado.getString("telefone"), endereco
    );
  }
}
