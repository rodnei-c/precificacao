package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.Dinheiro;
import br.com.modelagemdominio.rico.produtos.Percentual;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

public final class Precificacoes {

    private static final BigDecimal LIMITE_MONETARIO = new BigDecimal("99999999999999999.99");
    private static final BigDecimal LIMITE_PERCENTUAL = new BigDecimal("999999999999999.9999");

    private final RegraDePrecificacao regra;

    public Precificacoes(RegraDePrecificacao regra) {
        this.regra = regra;
    }

    public PrecificacaoParaCadastro comCustoEPercentual(BigDecimal custo, BigDecimal percentual)
        throws PrecificacaoInvalida {
        validarMonetario(custo, "custo");
        validarPercentual(percentual);
        return precificacao(custo, this.regra.precoVenda(custo, percentual), percentual);
    }

    public PrecificacaoParaCadastro comVendaEPercentual(BigDecimal precoVenda, BigDecimal percentual)
        throws PrecificacaoInvalida {
        validarMonetario(precoVenda, "preço de venda");
        validarPercentual(percentual);
        return precificacao(this.regra.custo(precoVenda, percentual), precoVenda, percentual);
    }

    public PrecificacaoParaCadastro comCustoEVenda(BigDecimal custo, BigDecimal precoVenda)
        throws PrecificacaoInvalida {
        validarMonetario(custo, "custo");
        validarMonetario(precoVenda, "preço de venda");
        validarOrdem(custo, precoVenda);
        return precificacao(custo, precoVenda, this.regra.percentual(custo, precoVenda));
    }

    private PrecificacaoParaCadastro precificacao(BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual)
        throws PrecificacaoInvalida {
        BigDecimal custoFinal = custo.setScale(2, HALF_EVEN);
        BigDecimal vendaFinal = precoVenda.setScale(2, HALF_EVEN);
        BigDecimal percentualFinal = percentual.setScale(4, HALF_EVEN);

        validarMonetario(custoFinal, "custo");
        validarMonetario(vendaFinal, "preço de venda");
        validarPercentual(percentualFinal);
        validarOrdem(custoFinal, vendaFinal);
        if (percentualFinal.signum() > 0 && vendaFinal.compareTo(custoFinal) == 0) {
            throw new PrecificacaoInvalida(
                "O percentual positivo deve produzir preço de venda maior que o custo após o arredondamento."
            );
        }
        return new PrecificacaoParaCadastro(new Precificacao(
            new Dinheiro(custoFinal), new Dinheiro(vendaFinal),
            new Percentual(percentualFinal), this.regra.modalidade()
        ));
    }

    private void validarOrdem(BigDecimal custo, BigDecimal precoVenda) throws PrecificacaoInvalida {
        if (precoVenda.compareTo(custo) < 0) {
            throw new PrecificacaoInvalida("O preço de venda não pode ser inferior ao custo.");
        }
    }

    private void validarMonetario(BigDecimal valor, String campo) throws PrecificacaoInvalida {
        if (valor.signum() <= 0 || valor.compareTo(LIMITE_MONETARIO) > 0) {
            throw new PrecificacaoInvalida(
                "O " + campo + " deve ser maior que zero e caber em 17 dígitos inteiros."
            );
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new PrecificacaoInvalida("Informe o " + campo + " com até duas casas decimais.");
        }
    }

    private void validarPercentual(BigDecimal percentual) throws PrecificacaoInvalida {
        if (percentual.signum() < 0 || percentual.compareTo(LIMITE_PERCENTUAL) > 0) {
            throw new PrecificacaoInvalida("O percentual deve ser não negativo e caber em 15 dígitos inteiros.");
        }
        if (percentual.stripTrailingZeros().scale() > 4) {
            throw new PrecificacaoInvalida("Informe o percentual com até quatro casas decimais.");
        }
    }
}
