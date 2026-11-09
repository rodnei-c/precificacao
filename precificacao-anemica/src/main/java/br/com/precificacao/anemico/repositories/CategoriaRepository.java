package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.models.Categoria;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository {

  Optional<Categoria> inserir(String nome);

  List<Categoria> listar();

  boolean existePorCodigo(Long codigo);
}
