package br.com.precificacao.anemico.repositories;

import br.com.precificacao.anemico.models.AlteracaoPrecificacao;
import java.util.List;

public interface HistoricoPrecificacaoRepository {

  void registrar(Long codigoProduto, AlteracaoPrecificacao.Valores anteriores,
                 AlteracaoPrecificacao.Valores novos);

  List<AlteracaoPrecificacao> listarPorProduto(Long codigoProduto);
}
