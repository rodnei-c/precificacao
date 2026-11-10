package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.models.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository {

  Optional<Categoria> inserir(String nome);

  List<Categoria> listar();

  /** @return categoria após atualizar somente o nome, ou vazio quando ausente */
  Optional<Categoria> renomear(Long codigo, String nome);

  boolean existePorCodigo(Long codigo);
}
