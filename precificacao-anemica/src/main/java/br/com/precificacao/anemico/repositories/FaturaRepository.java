package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.dtos.FaturaResumo;
import br.com.precificacao.anemico.models.Fatura;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FaturaRepository {

  Fatura salvar(Fatura fatura);

  Optional<Fatura> buscarPorId(Long id);

  List<FaturaResumo> listar(LocalDateTime inicioInclusivo, LocalDateTime fimExclusivo, Long codigoCliente);
}
