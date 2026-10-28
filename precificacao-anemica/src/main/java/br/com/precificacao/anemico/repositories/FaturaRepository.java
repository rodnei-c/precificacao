package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.models.Fatura;

import java.util.List;
import java.util.Optional;

public interface FaturaRepository {

    Fatura salvar(Fatura fatura);

    Optional<Fatura> buscarPorId(Long id);

    List<Fatura> listar();
}
