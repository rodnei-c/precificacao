package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.CadastroFaturaRequisicao;
import br.com.precificacao.anemico.dtos.FaturaResposta;
import br.com.precificacao.anemico.dtos.FaturaResumo;
import br.com.precificacao.anemico.dtos.ValoresFaturaResposta;
import br.com.precificacao.anemico.enums.TipoDesconto;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Fatura;
import br.com.precificacao.anemico.models.ItemFatura;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import br.com.precificacao.anemico.repositories.FaturaRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.FaturaService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaturaServiceImpl implements FaturaService {

  private static final BigDecimal LIMITE_QUANTIDADE = new BigDecimal("999999999999999.9999");
  private static final BigDecimal CEM = BigDecimal.valueOf(100);

  private final FaturaRepository faturaRepository;
  private final ProdutoRepository produtoRepository;
  private final ClienteRepository clienteRepository;

  public FaturaServiceImpl(
    FaturaRepository faturaRepository, ProdutoRepository produtoRepository, ClienteRepository clienteRepository
  ) {
    this.faturaRepository = Objects.requireNonNull(faturaRepository, "faturaRepository");
    this.produtoRepository = Objects.requireNonNull(produtoRepository, "produtoRepository");
    this.clienteRepository = Objects.requireNonNull(clienteRepository, "clienteRepository");
  }

  @Override
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public FaturaResposta cadastrar(CadastroFaturaRequisicao requisicao) {
    FaturaPreparada preparada = prepararFatura(requisicao);
    Fatura fatura = faturaRepository.salvar(preparada.fatura());
    return resposta(fatura, preparada.valores());
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ValoresFaturaResposta simular(CadastroFaturaRequisicao requisicao) {
    return prepararFatura(requisicao).valores();
  }

  @Override
  @Transactional(readOnly = true)
  public FaturaResposta buscarPorCodigo(Long codigo) {
    if (codigo == null || codigo <= 0) {
      throw new ValidacaoException("O código da fatura deve ser maior que zero.");
    }
    Fatura fatura = faturaRepository.buscarPorId(codigo)
      .orElseThrow(() -> new RegistroNaoEncontradoException("Fatura não encontrada."));
    return resposta(fatura, calcularValores(fatura, resumirItens(fatura.getItens())));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FaturaResumo> listar(LocalDate inicio, LocalDate fim, Long codigoCliente) {
    validarData(inicio);
    validarData(fim);
    if (inicio != null && fim != null && inicio.isAfter(fim)) {
      throw new ValidacaoException("A data inicial deve ser anterior ou igual à data final.");
    }

    if (codigoCliente != null && codigoCliente <= 0) {
      throw new ValidacaoException("O código do cliente deve ser maior que zero.");
    }

    LocalDateTime inicioInclusivo = inicio == null ? null : inicio.atStartOfDay();
    LocalDateTime fimExclusivo = fim == null ? null : fim.plusDays(1).atStartOfDay();
    return faturaRepository.listar(inicioInclusivo, fimExclusivo, codigoCliente);
  }

  private FaturaPreparada prepararFatura(CadastroFaturaRequisicao requisicao) {
    Map<Long, BigDecimal> quantidades = quantidadesPorProduto(requisicao.itens());
    Long codigoCliente = validarCliente(requisicao.codigoCliente());
    List<ItemFatura> itens = new ArrayList<>();
    for (var entrada : quantidades.entrySet()) {
      Long codigo = entrada.getKey();
      Produto produto = produtoRepository.buscarPorCodigo(codigo)
        .orElseThrow(() -> new RegistroNaoEncontradoException("Produto não encontrado: " + codigo + "."));
      itens.add(new ItemFatura(produto, entrada.getValue()));
    }

    ResumoItens resumo = resumirItens(itens);
    CadastroFaturaRequisicao.Desconto desconto = requisicao.desconto();
    validarDesconto(desconto, resumo.subtotal());
    Fatura fatura = new Fatura(
      null, null, itens, desconto == null ? null : desconto.tipo(),
      desconto == null ? null : desconto.valor(), codigoCliente, requisicao.formaPagamento()
    );
    return new FaturaPreparada(fatura, calcularValores(fatura, resumo));
  }

  private Long validarCliente(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    long codigo = codigoInteiroPositivo(valor, "cliente");
    if (!clienteRepository.existePorCodigo(codigo)) {
      throw new RegistroNaoEncontradoException("Cliente não encontrado.");
    }
    return codigo;
  }

  private void validarData(LocalDate data) {
    if (data != null && (data.getYear() < 1 || data.getYear() > 9999)) {
      throw new ValidacaoException("Informe datas entre 0001-01-01 e 9999-12-31.");
    }
  }

  private Map<Long, BigDecimal> quantidadesPorProduto(List<CadastroFaturaRequisicao.Item> itens) {
    if (itens == null || itens.isEmpty()) {
      throw new ValidacaoException("Informe pelo menos um item na fatura.");
    }
    Map<Long, BigDecimal> quantidades = new LinkedHashMap<>();
    for (CadastroFaturaRequisicao.Item item : itens) {
      if (item == null) {
        throw new ValidacaoException("Informe o código e a quantidade de cada item.");
      }
      long codigo = codigoInteiroPositivo(item.codigoProduto(), "produto");
      BigDecimal quantidade = item.quantidade();
      if (quantidade == null || quantidade.signum() <= 0
        || quantidade.compareTo(LIMITE_QUANTIDADE) > 0) {
        throw new ValidacaoException("A quantidade deve ser maior que zero e caber em 15 dígitos inteiros.");
      }
      if (quantidade.stripTrailingZeros().scale() > 4) {
        throw new ValidacaoException("Informe a quantidade com até quatro casas decimais.");
      }
      if (quantidades.putIfAbsent(codigo, quantidade.setScale(4)) != null) {
        throw new ValidacaoException("Informe cada produto uma única vez na fatura.");
      }
    }
    return quantidades;
  }

  private long codigoInteiroPositivo(BigDecimal valor, String entidade) {
    if (valor != null && valor.signum() > 0) {
      try {
        return valor.longValueExact();
      } catch (ArithmeticException excecao) {
        throw new ValidacaoException("Informe um código de " + entidade + " inteiro positivo válido.");
      }
    }
    throw new ValidacaoException("Informe um código de " + entidade + " inteiro positivo válido.");
  }

  private void validarDesconto(CadastroFaturaRequisicao.Desconto desconto, BigDecimal subtotal) {
    if (desconto == null) {
      return;
    }
    BigDecimal valor = desconto.valor();
    if (desconto.tipo() == null || valor == null || valor.signum() < 0) {
      throw new ValidacaoException("Informe o tipo do desconto e um valor não negativo.");
    }

    if (desconto.tipo() == TipoDesconto.PERCENTUAL) {
      if (valor.compareTo(CEM) > 0 || valor.stripTrailingZeros().scale() > 4) {
        throw new ValidacaoException("Informe um desconto de 0 a 100% com até quatro casas decimais.");
      }
    } else if (valor.compareTo(subtotal) > 0 || valor.stripTrailingZeros().scale() > 2) {
      throw new ValidacaoException("Informe um desconto fixo de até duas casas decimais que não ultrapasse o subtotal.");
    }
  }

  private ResumoItens resumirItens(List<ItemFatura> itens) {
    List<FaturaResposta.Item> itensResposta = new ArrayList<>();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (ItemFatura item : itens) {
      Produto produto = item.getProduto();
      BigDecimal precoVenda = produto.getPrecoVenda();
      BigDecimal quantidade = item.getQuantidade();
      BigDecimal subtotalItem = precoVenda.multiply(quantidade);
      itensResposta.add(new FaturaResposta.Item(produto.getCodigo(), quantidade, precoVenda, subtotalItem));
      subtotal = subtotal.add(subtotalItem);
    }
    return new ResumoItens(itensResposta, subtotal);
  }

  private ValoresFaturaResposta calcularValores(Fatura fatura, ResumoItens resumo) {
    BigDecimal subtotal = resumo.subtotal();
    BigDecimal descontoAplicado = calcularDesconto(fatura, subtotal);
    BigDecimal total = subtotal.subtract(descontoAplicado).setScale(2, RoundingMode.HALF_EVEN);
    return new ValoresFaturaResposta(resumo.itens(), subtotal, descontoAplicado, total);
  }

  private FaturaResposta resposta(Fatura fatura, ValoresFaturaResposta valores) {
    return new FaturaResposta(
      fatura.getId(), fatura.getDataCriacao(), valores.itens(), valores.subtotal(),
      valores.descontoAplicado(), valores.total(), fatura.getCodigoCliente(), fatura.getFormaPagamento()
    );
  }

  private BigDecimal calcularDesconto(Fatura fatura, BigDecimal subtotal) {
    if (fatura.getTipoDesconto() == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal valor = fatura.getValorDesconto();
    return switch (fatura.getTipoDesconto()) {
      case PERCENTUAL -> subtotal.multiply(valor.setScale(4)).movePointLeft(2);
      case VALOR_FIXO -> valor.setScale(2).min(subtotal);
    };
  }

  private record FaturaPreparada(Fatura fatura, ValoresFaturaResposta valores) {
  }

  private record ResumoItens(List<FaturaResposta.Item> itens, BigDecimal subtotal) {
  }
}
