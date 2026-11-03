package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.Dinheiro;
import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import br.com.modelagemdominio.rico.produtos.Percentual;
import br.com.modelagemdominio.rico.produtos.Precificacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrecificacoesTest {

    @ParameterizedTest
    @CsvSource({"ACRESCIMO_SOBRE_CUSTO, 120.00", "MARGEM_SOBRE_VENDA, 125.00"})
    void calculaVenda(ModalidadePrecificacao modalidade, BigDecimal venda) throws PrecificacaoInvalida {
        Precificacao resultado = calculo(modalidade)
            .comCustoEPercentual(new BigDecimal("100"), new BigDecimal("20")).precificacao();

        assertEquals(esperada(modalidade, venda), resultado);
    }

    @ParameterizedTest
    @CsvSource({"ACRESCIMO_SOBRE_CUSTO, 120.00", "MARGEM_SOBRE_VENDA, 125.00"})
    void calculaCusto(ModalidadePrecificacao modalidade, BigDecimal venda) throws PrecificacaoInvalida {
        Precificacao resultado = calculo(modalidade)
            .comVendaEPercentual(venda, new BigDecimal("20")).precificacao();

        assertEquals(esperada(modalidade, venda), resultado);
    }

    @ParameterizedTest
    @CsvSource({"ACRESCIMO_SOBRE_CUSTO, 120.00", "MARGEM_SOBRE_VENDA, 125.00"})
    void calculaPercentual(ModalidadePrecificacao modalidade, BigDecimal venda) throws PrecificacaoInvalida {
        Precificacao resultado = calculo(modalidade)
            .comCustoEVenda(new BigDecimal("100"), venda).precificacao();

        assertEquals(esperada(modalidade, venda), resultado);
    }

    @ParameterizedTest
    @CsvSource({"1.5, 1.02", "2.5, 1.02"})
    void arredondaEmpatesParaOPar(BigDecimal percentual, BigDecimal venda) throws PrecificacaoInvalida {
        Precificacao resultado = new Precificacoes(new AcrescimoSobreCusto())
            .comCustoEPercentual(BigDecimal.ONE, percentual).precificacao();

        assertEquals(new Dinheiro(venda), resultado.precoVenda());
    }

    @Test
    void arredondaPercentualEmQuatroCasas() throws PrecificacaoInvalida {
        Precificacao resultado = new Precificacoes(new AcrescimoSobreCusto())
            .comCustoEVenda(new BigDecimal("3"), new BigDecimal("4")).precificacao();

        assertEquals(new Percentual(new BigDecimal("33.3333")), resultado.percentual());
    }

    @Test
    void aceitaVendaIgualAoCustoComPercentualZero() throws PrecificacaoInvalida {
        Precificacao resultado = new Precificacoes(new MargemSobreVenda())
            .comCustoEVenda(new BigDecimal("100"), new BigDecimal("100")).precificacao();

        assertEquals(new Percentual(new BigDecimal("0.0000")), resultado.percentual());
    }

    @ParameterizedTest
    @CsvSource({
        "ACRESCIMO_SOBRE_CUSTO, 0, 20",
        "ACRESCIMO_SOBRE_CUSTO, 100, -1",
        "ACRESCIMO_SOBRE_CUSTO, 1.001, 20",
        "ACRESCIMO_SOBRE_CUSTO, 100, 20.00001",
        "ACRESCIMO_SOBRE_CUSTO, 100, 0.0040",
        "ACRESCIMO_SOBRE_CUSTO, 99999999999999999.99, 1",
        "MARGEM_SOBRE_VENDA, 100, 100",
        "MARGEM_SOBRE_VENDA, 100, 0.0040"
    })
    void rejeitaCustoPercentualOuVendaInvalidos(
        ModalidadePrecificacao modalidade, BigDecimal custo, BigDecimal percentual
    ) {
        assertThrows(PrecificacaoInvalida.class,
            () -> calculo(modalidade).comCustoEPercentual(custo, percentual));
    }

    @ParameterizedTest
    @CsvSource({"100, 100", "0.01, 99.9999", "100, 0.0040"})
    void rejeitaMargemInformadaOuCustoCalculadoInvalidos(BigDecimal venda, BigDecimal percentual) {
        Precificacoes precificacoes = new Precificacoes(new MargemSobreVenda());

        assertThrows(PrecificacaoInvalida.class,
            () -> precificacoes.comVendaEPercentual(venda, percentual));
    }

    @ParameterizedTest
    @CsvSource({"100, 99", "0.01, 100000000"})
    void rejeitaVendaInferiorAoCustoOuMargemArredondadaParaCem(BigDecimal custo, BigDecimal venda) {
        Precificacoes precificacoes = new Precificacoes(new MargemSobreVenda());

        assertThrows(PrecificacaoInvalida.class,
            () -> precificacoes.comCustoEVenda(custo, venda));
    }

    private Precificacoes calculo(ModalidadePrecificacao modalidade) {
        return new Precificacoes(switch (modalidade) {
            case ACRESCIMO_SOBRE_CUSTO -> new AcrescimoSobreCusto();
            case MARGEM_SOBRE_VENDA -> new MargemSobreVenda();
        });
    }

    private Precificacao esperada(ModalidadePrecificacao modalidade, BigDecimal venda) {
        return new Precificacao(
            new Dinheiro(new BigDecimal("100.00")),
            new Dinheiro(venda),
            new Percentual(new BigDecimal("20.0000")),
            modalidade
        );
    }
}
