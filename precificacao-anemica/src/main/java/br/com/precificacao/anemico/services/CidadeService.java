package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroCidadeRequisicao;
import br.com.precificacao.anemico.models.Cidade;
import java.util.List;

public interface CidadeService {

  Cidade cadastrar(CadastroCidadeRequisicao requisicao);

  List<Cidade> listar();
}
