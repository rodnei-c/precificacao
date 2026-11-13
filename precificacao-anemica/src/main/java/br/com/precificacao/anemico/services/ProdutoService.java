package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.dtos.AlteracaoCategoriaRequisicao;
import br.com.precificacao.anemico.dtos.AlteracaoPrecificacaoRequisicao;
import br.com.precificacao.anemico.dtos.CadastroProdutoRequisicao;
import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.models.AlteracaoPrecificacao;
import br.com.precificacao.anemico.models.Produto;
import java.util.List;

public interface ProdutoService {

  Produto cadastrar(CadastroProdutoRequisicao requisicao);

  Produto buscarPorCodigo(Long codigo);

  List<Produto> listar(String nome, ModalidadePrecificacao modalidade, Long categoria);

  Produto alterarPrecificacao(Long codigo, AlteracaoPrecificacaoRequisicao requisicao);

  Produto alterarCategoria(Long codigo, AlteracaoCategoriaRequisicao requisicao);

  List<AlteracaoPrecificacao> historicoPrecificacao(Long codigo);
}
