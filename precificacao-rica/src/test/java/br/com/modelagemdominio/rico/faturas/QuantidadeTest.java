package br.com.modelagemdominio.rico.faturas;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantidadeTest {

  @ParameterizedTest
  @ValueSource(strings = {"1", "0.0001", "2.5", "1.2345000", "999999999999999.999900", "1E+14"})
  void aceitaInteirosFracoesEZerosExcedentes(String entrada) {
    BigDecimal valor = new BigDecimal(entrada);

    assertEquals(valor, new Quantidade(valor).valor());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "0.00001", "1.23451", "1000000000000000", "1E+15"})
  void rejeitaQuantidadeNaoPositivaOuComPrecisaoExcedente(String entrada) {
    assertThrows(FaturaInvalida.class, () -> new Quantidade(new BigDecimal(entrada)));
  }

  @Test
  void rejeitaQuantidadeAusente() {
    assertThrows(FaturaInvalida.class, () -> new Quantidade(null));
  }
}
