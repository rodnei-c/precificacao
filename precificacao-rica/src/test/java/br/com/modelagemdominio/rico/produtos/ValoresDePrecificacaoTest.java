package br.com.modelagemdominio.rico.produtos;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValoresDePrecificacaoTest {

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "-0.01", "1.001", "100000000000000000.00"})
  void rejeitaQuantiaInvalidaSemDependerDoCalculo(String entrada) {
    BigDecimal valor = entrada == null ? null : new BigDecimal(entrada);

    assertThrows(ValorInvalido.class, () -> new Dinheiro(valor));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"-0.0001", "0.00001", "1000000000000000", "999999999999999.99991"})
  void rejeitaPercentualInvalidoSemDependerDoCalculo(String entrada) {
    BigDecimal valor = entrada == null ? null : new BigDecimal(entrada);

    assertThrows(ValorInvalido.class, () -> new Percentual(valor));
  }

  @Test
  void aceitaLimitesValidosSemRestringirAcrescimoACemPorCento() {
    assertTrue(new Dinheiro("0.01").comparar(new Dinheiro("99999999999999999.99")) < 0);
    assertTrue(new Percentual(0).comparar(new Percentual("999999999999999.9999")) < 0);
    assertEquals(new BigDecimal("9999999999999.999999"), new Percentual("999999999999999.9999").fracao());
  }

  @Test
  void representaOMesmoValorApesarDaEscalaOuDaOrigemDaEntrada() {
    var quantias = new HashSet<>(List.of(new Dinheiro(1), new Dinheiro("1.0000"), new Dinheiro(BigDecimal.ONE)));
    var percentuais = new HashSet<>(List.of(new Percentual(20), new Percentual("20.000000"),
      new Percentual(new BigDecimal("2E+1"))));

    assertEquals(1, quantias.size());
    assertEquals(1, percentuais.size());
  }

  @Test
  void transformaPontosPercentuaisEmFracaoExata() {
    assertEquals(new BigDecimal("0.123456"), new Percentual("12.3456").fracao());
  }
}
