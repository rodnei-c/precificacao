package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.dtos.CadastroCidadeRequisicao;
import br.com.precificacao.anemico.models.Cidade;
import br.com.precificacao.anemico.services.CidadeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cidades")
public class CidadeController {

  private final CidadeService cidadeService;

  public CidadeController(CidadeService cidadeService) {
    this.cidadeService = cidadeService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Cidade cadastrar(@RequestBody CadastroCidadeRequisicao requisicao) {
    return cidadeService.cadastrar(requisicao);
  }

  @GetMapping
  public List<Cidade> listar() {
    return cidadeService.listar();
  }
}
