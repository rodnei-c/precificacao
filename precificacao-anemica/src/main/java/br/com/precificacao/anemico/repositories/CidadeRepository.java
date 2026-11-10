package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.enums.UnidadeFederativa;
import br.com.precificacao.anemico.models.Cidade;
import java.util.List;
import java.util.Optional;

public interface CidadeRepository {

  Optional<Cidade> inserir(String nome, UnidadeFederativa uf);

  /** @return cidades que atendem a todos os filtros informados, em ordem de código */
  List<Cidade> listar(String nome, UnidadeFederativa uf);

  boolean existePorCodigo(Long codigo);
}
