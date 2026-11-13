package br.com.precificacao.anemico.models;

import br.com.precificacao.anemico.enums.UnidadeFederativa;

public record Cidade(Long codigo, String nome, UnidadeFederativa uf) {
}
