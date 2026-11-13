package br.com.precificacao.anemico.services;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.services.impl.PrecificacaoServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrecificacaoServiceTest {

  private final PrecificacaoService servico = new PrecificacaoServiceImpl();

  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
    "ACRESCIMO_SOBRE_CUSTO;100;-;20;100.00;120.00;20.0000",
    "ACRESCIMO_SOBRE_CUSTO;-;120;20;100.00;120.00;20.0000",
    "ACRESCIMO_SOBRE_CUSTO;100;120;-;100.00;120.00;20.0000",
    "MARGEM_SOBRE_VENDA;100;-;20;100.00;125.00;20.0000",
    "MARGEM_SOBRE_VENDA;-;125;20;100.00;125.00;20.0000",
    "MARGEM_SOBRE_VENDA;100;125;-;100.00;125.00;20.0000",
    "MARGEM_SOBRE_VENDA;100;100;-;100.00;100.00;0.0000",
    "ACRESCIMO_SOBRE_CUSTO;100;-;0;100.00;100.00;0.0000",
    "MARGEM_SOBRE_VENDA;100;-;0;100.00;100.00;0.0000"
  })
  void calculaAsTresCombinacoesNasDuasModalidades(
    String modalidade, String custo, String venda, String percentual,
    String custoEsperado, String vendaEsperada, String percentualEsperado
  ) {
    Produto produto = produto(modalidade, custo, venda, percentual);

    servico.precificar(produto);

    assertEquals(new BigDecimal(custoEsperado), produto.getCusto());
    assertEquals(new BigDecimal(vendaEsperada), produto.getPrecoVenda());
    assertEquals(new BigDecimal(percentualEsperado), produto.getPercentual());
  }

  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
    "1;1.5;1.02",
    "1;2.5;1.02"
  })
  void arredondaSomenteResultadoPeloCriterioPar(
    String custo, String percentual, String vendaEsperada
  ) {
    Produto produto = produto("ACRESCIMO_SOBRE_CUSTO", custo, "-", percentual);

    servico.precificar(produto);

    assertEquals(new BigDecimal(vendaEsperada), produto.getPrecoVenda());
  }

  @Test
  void arredondaPercentualCalculadoParaQuatroCasas() {
    Produto produto = produto("ACRESCIMO_SOBRE_CUSTO", "3", "4", "-");

    servico.precificar(produto);

    assertEquals(new BigDecimal("33.3333"), produto.getPercentual());
  }

  @ParameterizedTest
  @CsvSource(delimiter = ';', value = {
    "ACRESCIMO_SOBRE_CUSTO;100;120;20",
    "ACRESCIMO_SOBRE_CUSTO;100;-;-",
    "ACRESCIMO_SOBRE_CUSTO;-;-;-",
    "ACRESCIMO_SOBRE_CUSTO;0;-;20",
    "ACRESCIMO_SOBRE_CUSTO;-1;-;20",
    "ACRESCIMO_SOBRE_CUSTO;-;0;20",
    "ACRESCIMO_SOBRE_CUSTO;100;99;-",
    "ACRESCIMO_SOBRE_CUSTO;100;-;-1",
    "MARGEM_SOBRE_VENDA;100;-;100",
    "MARGEM_SOBRE_VENDA;-;100;100",
    "ACRESCIMO_SOBRE_CUSTO;-;100;0.0040",
    "MARGEM_SOBRE_VENDA;100;-;0.0040",
    "MARGEM_SOBRE_VENDA;-;100;0.0040",
    "ACRESCIMO_SOBRE_CUSTO;1.001;-;20",
    "ACRESCIMO_SOBRE_CUSTO;100;-;20.00001",
    "MARGEM_SOBRE_VENDA;0.01;100000000;-",
    "MARGEM_SOBRE_VENDA;-;0.01;99.9999",
    "ACRESCIMO_SOBRE_CUSTO;99999999999999999.99;-;1"
  })
  void rejeitaEntradasOuResultadosInvalidos(
    String modalidade, String custo, String venda, String percentual
  ) {
    Produto produto = produto(modalidade, custo, venda, percentual);

    assertThrows(ValidacaoException.class, () -> servico.precificar(produto));
  }

  @Test
  void rejeitaModalidadeAusente() {
    Produto produto = new Produto(
      null, "Produto", new BigDecimal("100"), null, new BigDecimal("20"), null
    );

    assertThrows(ValidacaoException.class, () -> servico.precificar(produto));
  }

  @Test
  void preservaOsDadosOriginaisQuandoCalculoEhRejeitado() {
    Produto produto = produto("ACRESCIMO_SOBRE_CUSTO", "100", "-", "0.0040");

    assertThrows(ValidacaoException.class, () -> servico.precificar(produto));

    assertEquals(new BigDecimal("100"), produto.getCusto());
    assertEquals(new BigDecimal("0.0040"), produto.getPercentual());
    assertNull(produto.getPrecoVenda());
  }

  private Produto produto(String modalidade, String custo, String venda, String percentual) {
    return new Produto(
      null, "Produto", numero(custo), numero(venda), numero(percentual),
      ModalidadePrecificacao.valueOf(modalidade)
    );
  }

  private BigDecimal numero(String valor) {
    return "-".equals(valor) ? null : new BigDecimal(valor);
  }
}
