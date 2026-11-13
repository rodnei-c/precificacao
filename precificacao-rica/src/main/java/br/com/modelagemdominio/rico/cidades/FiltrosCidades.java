package br.com.modelagemdominio.rico.cidades;

import java.util.Optional;

public record FiltrosCidades(Optional<NomeCidade> nome, Optional<UnidadeFederativa> uf) {
}
