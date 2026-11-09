package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroClienteRequisicao;
import br.com.precificacao.anemico.models.Cliente;
import java.util.List;

public interface ClienteService {

  Cliente cadastrar(CadastroClienteRequisicao requisicao);

  List<Cliente> listar();
}
