package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.repositories.FaturaRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;

import java.util.Objects;

public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final ProdutoRepository produtoRepository;

    public FaturaService(FaturaRepository faturaRepository, ProdutoRepository produtoRepository) {
        this.faturaRepository = Objects.requireNonNull(faturaRepository, "faturaRepository");
        this.produtoRepository = Objects.requireNonNull(produtoRepository, "produtoRepository");
    }
}
