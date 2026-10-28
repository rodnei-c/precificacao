package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.repositories.ProdutoRepository;

import java.util.Objects;

public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final PrecificacaoService precificacaoService;

    public ProdutoService(ProdutoRepository produtoRepository, PrecificacaoService precificacaoService) {
        this.produtoRepository = Objects.requireNonNull(produtoRepository, "produtoRepository");
        this.precificacaoService = Objects.requireNonNull(precificacaoService, "precificacaoService");
    }
}
