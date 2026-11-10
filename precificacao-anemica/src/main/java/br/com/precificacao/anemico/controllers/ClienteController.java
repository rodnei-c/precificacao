package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.dtos.AlteracaoClienteRequisicao;
import br.com.precificacao.anemico.dtos.CadastroClienteRequisicao;
import br.com.precificacao.anemico.models.Cliente;
import br.com.precificacao.anemico.services.ClienteService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

  private final ClienteService clienteService;

  public ClienteController(ClienteService clienteService) {
    this.clienteService = clienteService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Cliente cadastrar(@RequestBody CadastroClienteRequisicao requisicao) {
    return clienteService.cadastrar(requisicao);
  }

  @GetMapping
  public List<Cliente> listar() {
    return clienteService.listar();
  }

  /** @return os mesmos dados do cadastro para o código informado */
  @GetMapping("/{codigo}")
  public Cliente buscarPorCodigo(@PathVariable("codigo") Long codigo) {
    return clienteService.buscarPorCodigo(codigo);
  }

  /** @return cliente com os campos enviados atualizados */
  @PatchMapping("/{codigo}")
  public Cliente alterar(
    @PathVariable("codigo") Long codigo, @RequestBody AlteracaoClienteRequisicao requisicao
  ) {
    return clienteService.alterar(codigo, requisicao);
  }
}
