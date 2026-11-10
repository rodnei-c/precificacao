package br.com.modelagemdominio.rico.categorias;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodigoCategoriaTest {

  @ParameterizedTest
  @CsvSource({
    "1.0000,1",
    "9007199254740993,9007199254740993",
    "9223372036854775807,9223372036854775807"
  })
  void converteInteirosExatosSemPerderPrecisao(String entrada, long esperado) {
    assertEquals(new CodigoCategoria(esperado), new CodigoCategoria(new BigDecimal(entrada)));
  }

  @Test
  void ausenciaERejeitadaComAMensagemDoDominio() {
    var falha = assertThrows(IllegalArgumentException.class, () -> new CodigoCategoria((BigDecimal) null));

    assertEquals("Informe um código de categoria inteiro positivo válido.", falha.getMessage());
    assertNull(falha.getCause());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.0001", "9223372036854775808", "-9223372036854775809"})
  void rejeitaFracaoEOsDoisLimitesExcedidosPreservandoACausa(String entrada) {
    var falha = assertThrows(IllegalArgumentException.class,
      () -> new CodigoCategoria(new BigDecimal(entrada)));

    assertEquals("Informe um código de categoria inteiro positivo válido.", falha.getMessage());
    assertInstanceOf(ArithmeticException.class, falha.getCause());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, Long.MIN_VALUE})
  void exigeCodigoPositivoNosDoisConstrutores(long entrada) {
    var falhaInteiro = assertThrows(IllegalArgumentException.class, () -> new CodigoCategoria(entrada));
    var falhaDecimal = assertThrows(IllegalArgumentException.class,
      () -> new CodigoCategoria(BigDecimal.valueOf(entrada)));

    assertEquals("Informe um código de categoria inteiro positivo válido.", falhaInteiro.getMessage());
    assertEquals(falhaInteiro.getMessage(), falhaDecimal.getMessage());
    assertNull(falhaInteiro.getCause());
    assertNull(falhaDecimal.getCause());
  }
}
