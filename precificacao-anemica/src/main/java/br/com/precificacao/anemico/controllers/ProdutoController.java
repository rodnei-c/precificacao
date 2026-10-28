package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.services.ProdutoService;

import java.util.Objects;

public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = Objects.requireNonNull(produtoService, "produtoService");
    }
}
