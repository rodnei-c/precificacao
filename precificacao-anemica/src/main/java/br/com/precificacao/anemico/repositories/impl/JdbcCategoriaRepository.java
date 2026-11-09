package br.com.precificacao.anemico.repositories.impl;

import br.com.precificacao.anemico.models.Categoria;
import br.com.precificacao.anemico.repositories.CategoriaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCategoriaRepository implements CategoriaRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcCategoriaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Categoria> inserir(String nome) {
    return jdbcTemplate.query("""
      INSERT INTO categoria (nome) VALUES (?)
      ON CONFLICT (LOWER(nome)) DO NOTHING
      RETURNING codigo, nome
      """, (resultado, linha) -> new Categoria(resultado.getLong("codigo"), resultado.getString("nome")),
      nome
    ).stream().findFirst();
  }

  @Override
  public List<Categoria> listar() {
    return jdbcTemplate.query(
      "SELECT codigo, nome FROM categoria ORDER BY codigo",
      (resultado, linha) -> new Categoria(resultado.getLong("codigo"), resultado.getString("nome"))
    );
  }

  @Override
  public boolean existePorCodigo(Long codigo) {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
      "SELECT EXISTS (SELECT 1 FROM categoria WHERE codigo = ?)", Boolean.class, codigo
    ));
  }
}
