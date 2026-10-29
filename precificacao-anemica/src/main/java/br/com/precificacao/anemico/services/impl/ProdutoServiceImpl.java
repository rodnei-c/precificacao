package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.CadastroProdutoRequisicao;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.PrecificacaoService;
import br.com.precificacao.anemico.services.ProdutoService;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executa as regras de cadastro e consulta dos produtos.
 */
@Service
public class ProdutoServiceImpl implements ProdutoService {

  private final ProdutoRepository produtoRepository;
  private final PrecificacaoService precificacaoService;

  /**
   * @param produtoRepository acesso aos dados de produtos
   * @param precificacaoService cálculos e validações de precificação
   */
  public ProdutoServiceImpl(
    ProdutoRepository produtoRepository,
    PrecificacaoService precificacaoService
  ) {
    this.produtoRepository = Objects.requireNonNull(produtoRepository, "produtoRepository");
    this.precificacaoService = Objects.requireNonNull(precificacaoService, "precificacaoService");
  }

  /**
   * Valida o cadastro, calcula o valor ausente e persiste o produto.
   *
   * @param requisicao nome, modalidade e dois valores de precificação
   * @return produto com código gerado pelo banco
   * @throws ValidacaoException quando os dados não atendem às regras de cadastro
   */
  @Override
  @Transactional
  public Produto cadastrar(CadastroProdutoRequisicao requisicao) {
    if (requisicao == null || requisicao.nome() == null || requisicao.nome().isBlank()) {
      throw new ValidacaoException("Informe o nome do produto.");
    }
    String nome = requisicao.nome().strip();
    if (nome.length() > 200) {
      throw new ValidacaoException("O nome do produto deve ter no máximo 200 caracteres.");
    }

    Produto produto = new Produto(
      null, nome, requisicao.custo(), requisicao.precoVenda(), requisicao.percentual(),
      requisicao.modalidadePrecificacao()
    );
    precificacaoService.precificar(produto);
    return produtoRepository.salvar(produto);
  }

  /**
   * Valida o código e recupera o produto cadastrado.
   *
   * @param codigo identificação do produto
   * @return produto encontrado
   * @throws ValidacaoException quando o código não é positivo
   * @throws RegistroNaoEncontradoException quando o código não existe
   */
  @Override
  @Transactional(readOnly = true)
  public Produto buscarPorCodigo(Long codigo) {
    if (codigo == null || codigo <= 0) {
      throw new ValidacaoException("O código do produto deve ser maior que zero.");
    }
    return produtoRepository.buscarPorCodigo(codigo)
      .orElseThrow(() -> new RegistroNaoEncontradoException("Produto não encontrado."));
  }
}
