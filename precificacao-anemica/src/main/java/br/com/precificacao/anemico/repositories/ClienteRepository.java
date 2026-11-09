package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.models.Cliente;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository {

  Optional<Cliente> inserir(Cliente cliente);

  List<Cliente> listar();

  boolean existePorCodigo(Long codigo);
}
