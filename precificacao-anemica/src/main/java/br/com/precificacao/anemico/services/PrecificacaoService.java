package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Produto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Concentra as fórmulas e as validações de precificação do modelo anêmico.
 */
@Service
public class PrecificacaoService {

  private static final BigDecimal CEM = new BigDecimal("100");
  private static final BigDecimal LIMITE_MONETARIO = new BigDecimal("99999999999999999.99");
  private static final BigDecimal LIMITE_PERCENTUAL = new BigDecimal("999999999999999.9999");
  private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

  /**
   * Calcula o valor ausente e somente altera o produto após validar o resultado.
   *
   * @param produto dados com modalidade e exatamente dois valores informados
   * @throws ValidacaoException quando a entrada ou o resultado são inválidos
   */
  public void precificar(Produto produto) {
    if (produto == null || produto.getModalidadePrecificacao() == null) {
      throw new ValidacaoException("Informe a modalidade de precificação.");
    }

    BigDecimal custo = produto.getCusto();
    BigDecimal venda = produto.getPrecoVenda();
    BigDecimal percentual = produto.getPercentual();
    int informados = (custo == null ? 0 : 1)
      + (venda == null ? 0 : 1) + (percentual == null ? 0 : 1);
    if (informados != 2) {
      throw new ValidacaoException("Informe exatamente dois valores entre custo, preço de venda e percentual.");
    }

    validarMonetarioInformado(custo, "custo");
    validarMonetarioInformado(venda, "preço de venda");
    validarPercentual(percentual, produto.getModalidadePrecificacao());

    boolean acrescimo = produto.getModalidadePrecificacao()
      == ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO;
    if (venda == null) {
      venda = acrescimo
        ? custo.multiply(CEM.add(percentual)).divide(CEM, 2, ARREDONDAMENTO)
        : custo.multiply(CEM).divide(CEM.subtract(percentual), 2, ARREDONDAMENTO);
    } else if (custo == null) {
      custo = acrescimo
        ? venda.multiply(CEM).divide(CEM.add(percentual), 2, ARREDONDAMENTO)
        : venda.multiply(CEM.subtract(percentual)).divide(CEM, 2, ARREDONDAMENTO);
    } else {
      if (venda.compareTo(custo) < 0) {
        throw new ValidacaoException("O preço de venda não pode ser inferior ao custo.");
      }
      percentual = venda.subtract(custo).multiply(CEM)
        .divide(acrescimo ? custo : venda, 4, ARREDONDAMENTO);
    }

    custo = custo.setScale(2, ARREDONDAMENTO);
    venda = venda.setScale(2, ARREDONDAMENTO);
    percentual = percentual.setScale(4, ARREDONDAMENTO);
    validarMonetarioInformado(custo, "custo");
    validarMonetarioInformado(venda, "preço de venda");
    validarPercentual(percentual, produto.getModalidadePrecificacao());
    if (venda.compareTo(custo) < 0) {
      throw new ValidacaoException("O preço de venda não pode ser inferior ao custo.");
    }
    if (percentual.signum() > 0 && venda.compareTo(custo) == 0) {
      throw new ValidacaoException("O percentual positivo deve produzir preço de venda maior que o custo após o arredondamento.");
    }

    produto.setCusto(custo);
    produto.setPrecoVenda(venda);
    produto.setPercentual(percentual);
  }

  private void validarMonetarioInformado(BigDecimal valor, String campo) {
    if (valor == null) {
      return;
    }
    if (valor.signum() <= 0 || valor.compareTo(LIMITE_MONETARIO) > 0) {
      throw new ValidacaoException("O " + campo + " deve ser maior que zero e caber em 17 dígitos inteiros.");
    }
    if (valor.stripTrailingZeros().scale() > 2) {
      throw new ValidacaoException("Informe o " + campo + " com até duas casas decimais.");
    }
  }

  private void validarPercentual(BigDecimal percentual, ModalidadePrecificacao modalidade) {
    if (percentual == null) {
      return;
    }
    if (percentual.signum() < 0 || percentual.compareTo(LIMITE_PERCENTUAL) > 0) {
      throw new ValidacaoException("O percentual deve ser não negativo e caber em 15 dígitos inteiros.");
    }
    if (percentual.stripTrailingZeros().scale() > 4) {
      throw new ValidacaoException("Informe o percentual com até quatro casas decimais.");
    }
    if (modalidade == ModalidadePrecificacao.MARGEM_SOBRE_VENDA
      && percentual.compareTo(CEM) >= 0) {
      throw new ValidacaoException("A margem sobre a venda deve ser inferior a 100%.");
    }
  }
}
