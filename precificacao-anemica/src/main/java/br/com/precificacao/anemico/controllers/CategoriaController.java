package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.dtos.CadastroCategoriaRequisicao;
import br.com.precificacao.anemico.models.Categoria;
import br.com.precificacao.anemico.services.CategoriaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

  private final CategoriaService categoriaService;

  public CategoriaController(CategoriaService categoriaService) {
    this.categoriaService = categoriaService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Categoria cadastrar(@RequestBody CadastroCategoriaRequisicao requisicao) {
    return categoriaService.cadastrar(requisicao);
  }

  @GetMapping
  public List<Categoria> listar() {
    return categoriaService.listar();
  }
}
