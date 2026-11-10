package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.CadastroCategoriaRequisicao;
import br.com.precificacao.anemico.dtos.RenomeacaoCategoriaRequisicao;
import br.com.precificacao.anemico.models.Categoria;
import java.math.BigDecimal;
import java.util.List;

public interface CategoriaService {

  Categoria cadastrar(CadastroCategoriaRequisicao requisicao);

  List<Categoria> listar();

  /** @return categoria renomeada, preservando seus vínculos */
  Categoria renomear(Long codigo, RenomeacaoCategoriaRequisicao requisicao);

  long validarCategoria(BigDecimal codigo);
}
