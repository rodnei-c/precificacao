package br.com.modelagemdominio.rico.cidades;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodigoCidadeTest {

  @Test
  void aceitaMaiorCodigoComZerosDecimaisSemPerderPrecisao() {
    assertEquals(new CodigoCidade(Long.MAX_VALUE),
      new CodigoCidade(new BigDecimal("9223372036854775807.0000")));
  }

  @Test
  void ausenciaMantemAMensagemDeCidadeSemCausaNumerica() {
    var falha = assertThrows(IllegalArgumentException.class, () -> new CodigoCidade((BigDecimal) null));

    assertEquals("Informe um código de cidade inteiro positivo válido.", falha.getMessage());
    assertNull(falha.getCause());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0001", "9223372036854775808"})
  void conversaoInvalidaMantemAMensagemDeCidadeEACausa(String entrada) {
    var falha = assertThrows(IllegalArgumentException.class, () -> new CodigoCidade(new BigDecimal(entrada)));

    assertEquals("Informe um código de cidade inteiro positivo válido.", falha.getMessage());
    assertInstanceOf(ArithmeticException.class, falha.getCause());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void exigeCodigoPositivoNosDoisConstrutores(long entrada) {
    var falhaInteiro = assertThrows(IllegalArgumentException.class, () -> new CodigoCidade(entrada));
    var falhaDecimal = assertThrows(IllegalArgumentException.class,
      () -> new CodigoCidade(BigDecimal.valueOf(entrada)));

    assertEquals("Informe um código de cidade inteiro positivo válido.", falhaInteiro.getMessage());
    assertEquals(falhaInteiro.getMessage(), falhaDecimal.getMessage());
    assertNull(falhaInteiro.getCause());
    assertNull(falhaDecimal.getCause());
  }
}
