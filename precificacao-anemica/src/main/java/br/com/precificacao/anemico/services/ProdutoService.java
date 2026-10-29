package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroProdutoRequisicao;
import br.com.precificacao.anemico.models.Produto;

public interface ProdutoService {

  Produto cadastrar(CadastroProdutoRequisicao requisicao);

  Produto buscarPorCodigo(Long codigo);
}
