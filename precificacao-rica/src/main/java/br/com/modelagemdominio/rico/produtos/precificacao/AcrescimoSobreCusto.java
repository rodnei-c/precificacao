package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

public final class AcrescimoSobreCusto implements RegraDePrecificacao {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    @Override
    public ModalidadePrecificacao modalidade() {
        return ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO;
    }

    @Override
    public BigDecimal precoVenda(BigDecimal custo, BigDecimal percentual) {
        return custo.multiply(CEM.add(percentual)).divide(CEM, 2, HALF_EVEN);
    }

    @Override
    public BigDecimal custo(BigDecimal precoVenda, BigDecimal percentual) {
        return precoVenda.multiply(CEM).divide(CEM.add(percentual), 2, HALF_EVEN);
    }

    @Override
    public BigDecimal percentual(BigDecimal custo, BigDecimal precoVenda) {
        return precoVenda.subtract(custo).multiply(CEM).divide(custo, 4, HALF_EVEN);
    }
}
