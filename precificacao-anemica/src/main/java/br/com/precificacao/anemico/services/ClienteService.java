package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.AlteracaoClienteRequisicao;
import br.com.precificacao.anemico.dtos.CadastroClienteRequisicao;
import br.com.precificacao.anemico.models.Cliente;
import java.util.List;

public interface ClienteService {

  Cliente cadastrar(CadastroClienteRequisicao requisicao);

  List<Cliente> listar();

  /** @return cliente cadastrado; rejeita código inválido ou ausente */
  Cliente buscarPorCodigo(Long codigo);

  /** @return cliente após a atualização dos campos presentes na requisição */
  Cliente alterar(Long codigo, AlteracaoClienteRequisicao requisicao);
}
