package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.enums.CampoCliente;
import br.com.precificacao.anemico.models.Cliente;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ClienteRepository {

  Optional<Cliente> inserir(Cliente cliente);

  List<Cliente> listar();

  /** @return cliente encontrado pelo código ou resultado vazio */
  Optional<Cliente> buscarPorCodigo(Long codigo);

  /** @return cliente bloqueado até o fim da transação de atualização */
  Optional<Cliente> buscarPorCodigoParaAtualizacao(Long codigo);

  /**
   * @param cliente dados já validados pelo serviço
   * @param camposInformados campos que devem participar do comando de atualização
   * @return registro atualizado, preservando os campos omitidos e a identidade do cliente
   */
  Optional<Cliente> atualizar(Cliente cliente, Set<CampoCliente> camposInformados);

  boolean existePorCodigo(Long codigo);
}
