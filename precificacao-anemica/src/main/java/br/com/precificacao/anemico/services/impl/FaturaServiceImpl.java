package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.repositories.FaturaRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.FaturaService;

import java.util.Objects;

public class FaturaServiceImpl implements FaturaService {

    private final FaturaRepository faturaRepository;
    private final ProdutoRepository produtoRepository;

    public FaturaServiceImpl(FaturaRepository faturaRepository, ProdutoRepository produtoRepository) {
        this.faturaRepository = Objects.requireNonNull(faturaRepository, "faturaRepository");
        this.produtoRepository = Objects.requireNonNull(produtoRepository, "produtoRepository");
    }
}
