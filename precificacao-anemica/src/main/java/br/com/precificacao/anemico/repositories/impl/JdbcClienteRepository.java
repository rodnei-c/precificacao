package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.models.Cliente;
import br.com.precificacao.anemico.models.Endereco;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClienteRepository implements ClienteRepository {

  private static final String COLUNAS =
    "codigo, nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo";

  private final JdbcTemplate jdbcTemplate;

  public JdbcClienteRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Cliente> inserir(Cliente cliente) {
    Endereco endereco = cliente.endereco();
    return jdbcTemplate.query("""
      INSERT INTO cliente (nome, cpf, email, telefone, cep, logradouro, numero, bairro, complemento, cidade_codigo)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT DO NOTHING
      RETURNING
      """ + COLUNAS, this::mapear, cliente.nome(), cliente.cpf(), cliente.email(), cliente.telefone(),
      endereco == null ? null : endereco.cep(), endereco == null ? null : endereco.logradouro(),
      endereco == null ? null : endereco.numero(), endereco == null ? null : endereco.bairro(),
      endereco == null ? null : endereco.complemento(), endereco == null ? null : endereco.codigoCidade()
    ).stream().findFirst();
  }

  @Override
  public List<Cliente> listar() {
    return jdbcTemplate.query("SELECT " + COLUNAS + " FROM cliente ORDER BY codigo", this::mapear);
  }

  @Override
  public boolean existePorCodigo(Long codigo) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
      "SELECT EXISTS (SELECT 1 FROM cliente WHERE codigo = ?)", Boolean.class, codigo
    ));
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
