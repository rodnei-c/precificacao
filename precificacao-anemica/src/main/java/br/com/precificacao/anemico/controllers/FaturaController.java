package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.services.FaturaService;

import java.util.Objects;

public class FaturaController {

    private final FaturaService faturaService;

    public FaturaController(FaturaService faturaService) {
        this.faturaService = Objects.requireNonNull(faturaService, "faturaService");
    }
}
