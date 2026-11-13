package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValoresParaPrecificarTest {

  @ParameterizedTest
  @MethodSource("quantidadesInvalidas")
  void rejeitaQuantidadeDeValoresForaDoHttp(BigDecimal custo, BigDecimal venda, BigDecimal percentual) {
    var valores = new ValoresParaPrecificar(custo, venda, percentual);
    var falha = assertThrows(PrecificacaoInvalida.class,
      () -> valores.precificacao(ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO));

    assertEquals("Informe exatamente dois valores entre custo, preço de venda e percentual.", falha.getMessage());
  }

  private static Stream<Arguments> quantidadesInvalidas() {
    return Stream.of(
      Arguments.of(null, null, null),
      Arguments.of(new BigDecimal("100"), null, null),
      Arguments.of(new BigDecimal("100"), new BigDecimal("120"), new BigDecimal("20"))
    );
  }
}
