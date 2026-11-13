package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

public record ValoresParaPrecificar(
    Optional<BigDecimal> custo,
    Optional<BigDecimal> precoVenda,
    Optional<BigDecimal> percentual
) {

    public ValoresParaPrecificar(BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual) {
        this(Optional.ofNullable(custo), Optional.ofNullable(precoVenda), Optional.ofNullable(percentual));
    }

    public PrecificacaoParaCadastro precificacao(ModalidadePrecificacao modalidade) throws PrecificacaoInvalida {
        if (modalidade == null) {
            throw new PrecificacaoInvalida("Informe a modalidade de precificação.");
        }
        long informados = Stream.of(this.custo, this.precoVenda, this.percentual).filter(Optional::isPresent).count();
        if (informados != 2) {
            throw new PrecificacaoInvalida(
                "Informe exatamente dois valores entre custo, preço de venda e percentual."
            );
        }
        Precificacoes calculo = new Precificacoes(switch (modalidade) {
            case ACRESCIMO_SOBRE_CUSTO -> new AcrescimoSobreCusto();
            case MARGEM_SOBRE_VENDA -> new MargemSobreVenda();
        });
        if (this.precoVenda.isEmpty()) {
            return calculo.comCustoEPercentual(this.custo.orElseThrow(), this.percentual.orElseThrow());
        }
        if (this.custo.isEmpty()) {
            return calculo.comVendaEPercentual(this.precoVenda.orElseThrow(), this.percentual.orElseThrow());
        }
        return calculo.comCustoEVenda(this.custo.orElseThrow(), this.precoVenda.orElseThrow());
    }
}
