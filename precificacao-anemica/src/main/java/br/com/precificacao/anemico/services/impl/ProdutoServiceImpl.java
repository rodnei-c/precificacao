package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.AlteracaoCategoriaRequisicao;
import br.com.precificacao.anemico.dtos.AlteracaoPrecificacaoRequisicao;
import br.com.precificacao.anemico.dtos.CadastroProdutoRequisicao;
import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.AlteracaoPrecificacao;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.HistoricoPrecificacaoRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.CategoriaService;
import br.com.precificacao.anemico.services.PrecificacaoService;
import br.com.precificacao.anemico.services.ProdutoService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordena as operações de produto.
 */
@Service
public class ProdutoServiceImpl implements ProdutoService {

  private final ProdutoRepository produtoRepository;
  private final PrecificacaoService precificacaoService;
  private final HistoricoPrecificacaoRepository historicoRepository;
  private final CategoriaService categoriaService;

  /**
   * @param produtoRepository acesso aos dados de produtos
   * @param precificacaoService cálculos e validações de precificação
   * @param historicoRepository armazenamento das alterações de precificação
   * @param categoriaService validação dos vínculos com categorias
   */
  public ProdutoServiceImpl(
    ProdutoRepository produtoRepository,
    PrecificacaoService precificacaoService,
    HistoricoPrecificacaoRepository historicoRepository,
    CategoriaService categoriaService
  ) {
    this.produtoRepository = Objects.requireNonNull(produtoRepository, "produtoRepository");
    this.precificacaoService = Objects.requireNonNull(precificacaoService, "precificacaoService");
    this.historicoRepository = Objects.requireNonNull(historicoRepository, "historicoRepository");
    this.categoriaService = Objects.requireNonNull(categoriaService, "categoriaService");
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
    if (requisicao.codigoCategoria() != null) {
      produto.setCodigoCategoria(categoriaService.validarCategoria(requisicao.codigoCategoria()));
    }
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
    validarCodigo(codigo);
    return produtoRepository.buscarPorCodigo(codigo)
      .orElseThrow(() -> new RegistroNaoEncontradoException("Produto não encontrado."));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Produto> listar(String nome, ModalidadePrecificacao modalidade, Long categoria) {
    if (categoria != null && categoria <= 0) {
      throw new ValidacaoException("O código da categoria deve ser maior que zero.");
    }
    String nomeFiltrado = nome == null || nome.isBlank() ? null : nome.strip();
    return produtoRepository.listar(nomeFiltrado, modalidade, categoria);
  }

  @Override
  @Transactional
  public Produto alterarPrecificacao(Long codigo, AlteracaoPrecificacaoRequisicao requisicao) {
    Produto atual = buscarParaAtualizacao(codigo);
    Produto atualizado = new Produto(
      atual.getCodigo(), atual.getNome(), requisicao.custo(), requisicao.precoVenda(),
      requisicao.percentual(), atual.getModalidadePrecificacao()
    );
    atualizado.setCodigoCategoria(atual.getCodigoCategoria());
    precificacaoService.precificar(atualizado);
    if (atual.getCusto().compareTo(atualizado.getCusto()) == 0
      && atual.getPrecoVenda().compareTo(atualizado.getPrecoVenda()) == 0
      && atual.getPercentual().compareTo(atualizado.getPercentual()) == 0) {
      return atual;
    }

    Produto gravado = produtoRepository.salvar(atualizado);
    historicoRepository.registrar(codigo, valores(atual), valores(gravado));
    return gravado;
  }

  @Override
  @Transactional
  public Produto alterarCategoria(Long codigo, AlteracaoCategoriaRequisicao requisicao) {
    Produto produto = buscarParaAtualizacao(codigo);
    produto.setCodigoCategoria(categoriaService.validarCategoria(requisicao.codigoCategoria()));
    return produtoRepository.salvar(produto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlteracaoPrecificacao> historicoPrecificacao(Long codigo) {
    buscarPorCodigo(codigo);
    return historicoRepository.listarPorProduto(codigo);
  }

  private Produto buscarParaAtualizacao(Long codigo) {
    validarCodigo(codigo);
    return produtoRepository.buscarPorCodigoParaAtualizacao(codigo)
      .orElseThrow(() -> new RegistroNaoEncontradoException("Produto não encontrado."));
  }

  private void validarCodigo(Long codigo) {
    if (codigo == null || codigo <= 0) {
      throw new ValidacaoException("O código do produto deve ser maior que zero.");
    }
  }

  private AlteracaoPrecificacao.Valores valores(Produto produto) {
    return new AlteracaoPrecificacao.Valores(
      produto.getCusto(), produto.getPrecoVenda(), produto.getPercentual()
    );
  }
}
