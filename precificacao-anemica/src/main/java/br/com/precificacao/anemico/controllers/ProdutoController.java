package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.dtos.CadastroProdutoRequisicao;
import br.com.precificacao.anemico.dtos.ProdutoResposta;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.services.ProdutoService;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe requisições HTTP de produtos e apresenta os resultados do serviço.
 */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

  private final ProdutoService produtoService;

  /**
   * @param produtoService serviço responsável pelo cadastro e pelas consultas
   */
  public ProdutoController(ProdutoService produtoService) {
    this.produtoService = Objects.requireNonNull(produtoService, "produtoService");
  }

  /**
   * Cadastra um produto e informa o endereço para sua consulta.
   *
   * @param requisicao dados recebidos no corpo JSON
   * @return produto cadastrado, com status 201 e cabeçalho Location
   */
  @PostMapping
  public ResponseEntity<ProdutoResposta> cadastrar(
    @RequestBody CadastroProdutoRequisicao requisicao
  ) {
    Produto produto = produtoService.cadastrar(requisicao);
    URI localizacao = URI.create("/api/produtos/" + produto.getCodigo());
    return ResponseEntity.created(localizacao).body(resposta(produto));
  }

  /**
   * Consulta um produto pelo código.
   *
   * @param codigo identificação recebida na URL
   * @return dados do produto encontrado
   */
  @GetMapping("/{codigo}")
  public ProdutoResposta buscarPorCodigo(@PathVariable("codigo") Long codigo) {
    return resposta(produtoService.buscarPorCodigo(codigo));
  }

  private ProdutoResposta resposta(Produto produto) {
    return new ProdutoResposta(
      produto.getCodigo(), produto.getNome(), produto.getCusto(),
      produto.getPrecoVenda(), produto.getPercentual(),
      produto.getModalidadePrecificacao()
    );
  }
}
