package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Percentual;
import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

public final class MargemSobreVenda implements RegraDePrecificacao {

  private static final BigDecimal CEM = BigDecimal.valueOf(100);

  @Override
  public ModalidadePrecificacao modalidade() {
    return ModalidadePrecificacao.MARGEM_SOBRE_VENDA;
  }

  @Override
  public BigDecimal precoVenda(BigDecimal custo, BigDecimal percentual) throws PrecificacaoInvalida {
    validarMargem(percentual);
    return custo.divide(BigDecimal.ONE.subtract(new Percentual(percentual).fracao()), 2, HALF_EVEN);
  }

  @Override
  public BigDecimal custo(BigDecimal precoVenda, BigDecimal percentual) throws PrecificacaoInvalida {
    validarMargem(percentual);
    return precoVenda.multiply(BigDecimal.ONE.subtract(new Percentual(percentual).fracao())).setScale(2, HALF_EVEN);
  }

  @Override
  public BigDecimal percentual(BigDecimal custo, BigDecimal precoVenda) throws PrecificacaoInvalida {
    BigDecimal percentual = precoVenda.subtract(custo).multiply(CEM)
      .divide(precoVenda, 4, HALF_EVEN);
    validarMargem(percentual);
    return percentual;
  }

  private void validarMargem(BigDecimal percentual) throws PrecificacaoInvalida {
    if (percentual.compareTo(CEM) >= 0) {
      throw new PrecificacaoInvalida("A margem sobre a venda deve ser inferior a 100%.");
    }
  }
}
