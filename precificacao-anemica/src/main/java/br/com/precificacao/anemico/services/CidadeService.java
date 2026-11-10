package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroCidadeRequisicao;
import br.com.precificacao.anemico.models.Cidade;
import java.util.List;

public interface CidadeService {

  Cidade cadastrar(CadastroCidadeRequisicao requisicao);

  /** @return cidades em ordem de código, combinando os filtros opcionais por nome e UF */
  List<Cidade> listar(String nome, String uf);
}
