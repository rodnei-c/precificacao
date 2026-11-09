package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroFaturaRequisicao;
import br.com.precificacao.anemico.dtos.FaturaResposta;
import br.com.precificacao.anemico.dtos.FaturaResumo;
import br.com.precificacao.anemico.dtos.ValoresFaturaResposta;
import java.time.LocalDate;
import java.util.List;

public interface FaturaService {

  FaturaResposta cadastrar(CadastroFaturaRequisicao requisicao);

  ValoresFaturaResposta simular(CadastroFaturaRequisicao requisicao);

  FaturaResposta buscarPorCodigo(Long codigo);

  List<FaturaResumo> listar(LocalDate inicio, LocalDate fim, Long codigoCliente);
}
