package br.com.modelagemdominio.rico.produtos;

import org.junit.jupiter.api.Test;

import static br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO;
import static br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao.MARGEM_SOBRE_VENDA;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrecificacaoTest {

  @Test
  void rejeitaVendaInferiorAoCustoNaConstrucaoDireta() {
    var falha = assertThrows(IllegalArgumentException.class,
      () -> new Precificacao(new Dinheiro(101), new Dinheiro(100), new Percentual(0), ACRESCIMO_SOBRE_CUSTO));

    assertEquals("O preço de venda não pode ser inferior ao custo.", falha.getMessage());
  }

  @Test
  void exigeVendaSuperiorAoCustoComPercentualPositivo() {
    var custo = new Dinheiro(100);
    var falha = assertThrows(IllegalArgumentException.class,
      () -> new Precificacao(custo, custo, new Percentual("0.0001"), ACRESCIMO_SOBRE_CUSTO));

    assertEquals("O percentual positivo deve produzir preço de venda maior que o custo após o arredondamento.",
      falha.getMessage());
    assertEquals(custo, new Precificacao(custo, custo, new Percentual(0), ACRESCIMO_SOBRE_CUSTO).precoVenda());
  }

  @Test
  void aplicaLimiteDeCemSomenteAMargem() {
    var custo = new Dinheiro(100);
    var venda = new Dinheiro(200);
    var cem = new Percentual(100);
    var falha = assertThrows(IllegalArgumentException.class,
      () -> new Precificacao(custo, venda, cem, MARGEM_SOBRE_VENDA));

    assertEquals("A margem sobre a venda deve ser inferior a 100%.", falha.getMessage());
    assertEquals(cem, new Precificacao(custo, venda, cem, ACRESCIMO_SOBRE_CUSTO).percentual());
  }

  @Test
  void informaAPrimeiraRegraVioladaNaComposicao() {
    var custo = new Dinheiro(100);
    var taxa = new Percentual(100);
    var abaixo = assertThrows(IllegalArgumentException.class,
      () -> new Precificacao(custo, new Dinheiro(99), taxa, MARGEM_SOBRE_VENDA));
    var igual = assertThrows(IllegalArgumentException.class,
      () -> new Precificacao(custo, custo, taxa, MARGEM_SOBRE_VENDA));

    assertEquals("O preço de venda não pode ser inferior ao custo.", abaixo.getMessage());
    assertEquals("O percentual positivo deve produzir preço de venda maior que o custo após o arredondamento.",
      igual.getMessage());
  }

  @Test
  void exigeTodosOsComponentesDoValor() {
    var dinheiro = new Dinheiro(100);
    var zero = new Percentual(0);

    assertAll(
      () -> assertThrows(NullPointerException.class,
        () -> new Precificacao(null, dinheiro, zero, ACRESCIMO_SOBRE_CUSTO)),
      () -> assertThrows(NullPointerException.class,
        () -> new Precificacao(dinheiro, null, zero, ACRESCIMO_SOBRE_CUSTO)),
      () -> assertThrows(NullPointerException.class,
        () -> new Precificacao(dinheiro, dinheiro, null, ACRESCIMO_SOBRE_CUSTO)),
      () -> assertThrows(NullPointerException.class,
        () -> new Precificacao(dinheiro, dinheiro, zero, null))
    );
  }
}
