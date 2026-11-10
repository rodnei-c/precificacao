package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.enums.UnidadeFederativa;
import br.com.precificacao.anemico.models.Cidade;
import br.com.precificacao.anemico.repositories.CidadeRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCidadeRepository implements CidadeRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcCidadeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Cidade> inserir(String nome, UnidadeFederativa uf) {
    return jdbcTemplate.query("""
      INSERT INTO cidade (nome, uf) VALUES (?, ?)
      ON CONFLICT (LOWER(nome), uf) DO NOTHING
      RETURNING codigo, nome, uf
      """, this::mapear, nome, uf.name()).stream().findFirst();
  }

  @Override
  public List<Cidade> listar(String nome, UnidadeFederativa uf) {
    StringBuilder sql = new StringBuilder("SELECT codigo, nome, uf FROM cidade WHERE TRUE");
    List<Object> parametros = new ArrayList<>();
    if (nome != null) {
      sql.append(" AND STRPOS(LOWER(nome), LOWER(?)) > 0");
      parametros.add(nome);
    }
    if (uf != null) {
      sql.append(" AND uf = ?");
      parametros.add(uf.name());
    }
    sql.append(" ORDER BY codigo");
    return jdbcTemplate.query(sql.toString(), this::mapear, parametros.toArray());
  }

  @Override
  public boolean existePorCodigo(Long codigo) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
      "SELECT EXISTS (SELECT 1 FROM cidade WHERE codigo = ?)", Boolean.class, codigo
    ));
  }

  private Cidade mapear(ResultSet resultado, int linha) throws SQLException {
    return new Cidade(
      resultado.getLong("codigo"), resultado.getString("nome"),
      UnidadeFederativa.valueOf(resultado.getString("uf"))
    );
  }
}
