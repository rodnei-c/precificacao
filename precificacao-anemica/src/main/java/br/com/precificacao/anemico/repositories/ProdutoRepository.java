package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.models.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository {

    Produto salvar(Produto produto);

    Optional<Produto> buscarPorCodigo(Long codigo);

    List<Produto> listar();
}
