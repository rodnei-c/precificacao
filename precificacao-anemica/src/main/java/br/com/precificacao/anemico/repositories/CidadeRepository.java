package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.enums.UnidadeFederativa;
import br.com.precificacao.anemico.models.Cidade;
import java.util.List;
import java.util.Optional;

public interface CidadeRepository {

  Optional<Cidade> inserir(String nome, UnidadeFederativa uf);

  List<Cidade> listar();

  boolean existePorCodigo(Long codigo);
}
