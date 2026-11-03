package br.com.modelagemdominio.rico.faturas.descontos;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescontosTest {

  @Test
  void limitaDescontoFixoERetomaOriginalQuandoSubtotalAumenta() {
    DescontoFixo desconto = new DescontoFixo(new BigDecimal("30.00"));

    assertEquals(new BigDecimal("30.00"), desconto.valorAplicado(new BigDecimal("100.00")));
    assertEquals(new BigDecimal("12.3456"), desconto.valorAplicado(new BigDecimal("12.3456")));
    assertEquals(new BigDecimal("30.00"), desconto.valorAplicado(new BigDecimal("50.00")));
    assertEquals(new BigDecimal("30.00"), desconto.valorOriginal());
  }

  @Test
  void calculaDescontoPercentualSemArredondar() {
    DescontoPercentual desconto = new DescontoPercentual(new BigDecimal("12.3456"));

    assertEquals(new BigDecimal("0.018738398592"),
      desconto.valorAplicado(new BigDecimal("0.151782")));
  }

  @ParameterizedTest
  @CsvSource({"0, 0", "10, 10", "100, 100"})
  void aplicaPercentualInclusiveAbaixoDeDuzentosReais(BigDecimal percentual, BigDecimal esperado) {
    Desconto desconto = new DescontoPercentual(percentual);

    assertEquals(0, esperado.compareTo(desconto.valorAplicado(new BigDecimal("100"))));
  }

  @Test
  void aceitaZerosExcedentesNosValoresContratados() {
    assertEquals(new BigDecimal("10.000"), new DescontoFixo(new BigDecimal("10.000")).valorOriginal());
    assertEquals(new BigDecimal("12.345600"),
      new DescontoPercentual(new BigDecimal("12.345600")).percentual());
  }

  @Test
  void aceitaDescontoFixoZeroESubtotalZero() {
    assertEquals(BigDecimal.ZERO, new DescontoFixo(BigDecimal.ZERO).valorAplicado(BigDecimal.TEN));
    assertEquals(BigDecimal.ZERO, new DescontoFixo(BigDecimal.TEN).valorAplicado(BigDecimal.ZERO));
    assertEquals(0, new DescontoPercentual(BigDecimal.TEN).valorAplicado(BigDecimal.ZERO).signum());
  }

  @Test
  void semDescontoNaoAbateValor() {
    assertEquals(BigDecimal.ZERO, new SemDesconto().valorAplicado(new BigDecimal("123.4567")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.01", "1.001"})
  void rejeitaDescontoFixoInvalido(String valor) {
    assertThrows(FaturaInvalida.class, () -> new DescontoFixo(new BigDecimal(valor)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.0001", "100.0001", "1.00001"})
  void rejeitaDescontoPercentualInvalido(String valor) {
    assertThrows(FaturaInvalida.class, () -> new DescontoPercentual(new BigDecimal(valor)));
  }

  @Test
  void rejeitaDescontoAusente() {
    assertThrows(FaturaInvalida.class, () -> new DescontoFixo(null));
    assertThrows(FaturaInvalida.class, () -> new DescontoPercentual(null));
  }
}
