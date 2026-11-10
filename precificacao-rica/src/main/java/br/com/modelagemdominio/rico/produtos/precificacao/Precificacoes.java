package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.Dinheiro;
import br.com.modelagemdominio.rico.produtos.Percentual;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import br.com.modelagemdominio.rico.produtos.ValorInvalido;
import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

public final class Precificacoes {

  private final RegraDePrecificacao regra;

  public Precificacoes(RegraDePrecificacao regra) {
    this.regra = regra;
  }

  public PrecificacaoParaCadastro comCustoEPercentual(BigDecimal custo, BigDecimal percentual)
    throws PrecificacaoInvalida {
    Dinheiro quantia = this.dinheiro(custo, "custo");
    Percentual taxa = this.percentual(percentual);
    return this.precificacao(quantia.valor(), this.regra.precoVenda(quantia.valor(), taxa.valor()), taxa.valor());
  }

  public PrecificacaoParaCadastro comVendaEPercentual(BigDecimal precoVenda, BigDecimal percentual)
    throws PrecificacaoInvalida {
    Dinheiro quantia = this.dinheiro(precoVenda, "preço de venda");
    Percentual taxa = this.percentual(percentual);
    return this.precificacao(this.regra.custo(quantia.valor(), taxa.valor()), quantia.valor(), taxa.valor());
  }

  public PrecificacaoParaCadastro comCustoEVenda(BigDecimal custo, BigDecimal precoVenda)
    throws PrecificacaoInvalida {
    Dinheiro custoValidado = this.dinheiro(custo, "custo");
    Dinheiro vendaValidada = this.dinheiro(precoVenda, "preço de venda");
    this.validarOrdem(custoValidado, vendaValidada);
    return this.precificacao(custoValidado.valor(), vendaValidada.valor(),
      this.regra.percentual(custoValidado.valor(), vendaValidada.valor()));
  }

  private PrecificacaoParaCadastro precificacao(BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual)
    throws PrecificacaoInvalida {
    Dinheiro custoFinal = this.dinheiro(custo.setScale(2, HALF_EVEN), "custo");
    Dinheiro vendaFinal = this.dinheiro(precoVenda.setScale(2, HALF_EVEN), "preço de venda");
    Percentual percentualFinal = this.percentual(percentual.setScale(4, HALF_EVEN));

    this.validarOrdem(custoFinal, vendaFinal);
    if (percentualFinal.valor().signum() > 0 && vendaFinal.comparar(custoFinal) == 0) {
      throw new PrecificacaoInvalida(
        "O percentual positivo deve produzir preço de venda maior que o custo após o arredondamento."
      );
    }
    return new PrecificacaoParaCadastro(new Precificacao(
      custoFinal, vendaFinal, percentualFinal, this.regra.modalidade()
    ));
  }

  private void validarOrdem(Dinheiro custo, Dinheiro precoVenda) throws PrecificacaoInvalida {
    if (precoVenda.comparar(custo) < 0) {
      throw new PrecificacaoInvalida("O preço de venda não pode ser inferior ao custo.");
    }
  }

  private Dinheiro dinheiro(BigDecimal valor, String campo) throws PrecificacaoInvalida {
    try {
      return new Dinheiro(valor);
    } catch (ValorInvalido falha) {
      throw new PrecificacaoInvalida(falha.mensagem(campo), falha);
    }
  }

  private Percentual percentual(BigDecimal valor) throws PrecificacaoInvalida {
    try {
      return new Percentual(valor);
    } catch (ValorInvalido falha) {
      throw new PrecificacaoInvalida(falha.getMessage(), falha);
    }
  }
}
