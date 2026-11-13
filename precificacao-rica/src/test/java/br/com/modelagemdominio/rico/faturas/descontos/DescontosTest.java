package br.com.modelagemdominio.rico.faturas.descontos;

import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @ParameterizedTest
  @MethodSource("descontosRegistrados")
  void reconstituiComportamentoSemPerderOriginalOuConfundirZeroComAusencia(
    Desconto desconto, Optional<TipoDesconto> tipo, BigDecimal original, BigDecimal subtotal, BigDecimal aplicado
  ) {
    var registro = desconto.registro();
    assertEquals(tipo, registro.map(DescontoContratado::tipo));
    assertEquals(Optional.ofNullable(original), registro.map(DescontoContratado::valor));

    Desconto reconstituido = new DescontoReconstituido(registro);

    assertEquals(aplicado, reconstituido.valorAplicado(subtotal));
    assertEquals(registro, reconstituido.registro());
  }

  @Test
  void reconstituicaoPreservaLimiteDoDescontoFixoNoSubtotalInicial() {
    var registro = new DescontoContratado(TipoDesconto.VALOR_FIXO, new BigDecimal("30.00"));
    Desconto desconto = new DescontoReconstituido(registro);

    desconto.validarSubtotalInicial(new BigDecimal("30.00"));
    var falha = assertThrows(FaturaInvalida.class,
      () -> desconto.validarSubtotalInicial(new BigDecimal("12.3456")));

    assertEquals("O desconto fixo não pode ultrapassar o subtotal inicial da fatura.", falha.getMessage());
  }

  private static Stream<Arguments> descontosRegistrados() {
    return Stream.of(
      Arguments.of(new SemDesconto(), Optional.empty(), null, BigDecimal.TEN, BigDecimal.ZERO),
      Arguments.of(new DescontoFixo(BigDecimal.ZERO), Optional.of(TipoDesconto.VALOR_FIXO),
        BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO),
      Arguments.of(new DescontoPercentual(BigDecimal.ZERO), Optional.of(TipoDesconto.PERCENTUAL),
        BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("0.00")),
      Arguments.of(new DescontoFixo(new BigDecimal("30.00")), Optional.of(TipoDesconto.VALOR_FIXO),
        new BigDecimal("30.00"), new BigDecimal("12.3456"), new BigDecimal("12.3456")),
      Arguments.of(new DescontoPercentual(new BigDecimal("12.3456")), Optional.of(TipoDesconto.PERCENTUAL),
        new BigDecimal("12.3456"), new BigDecimal("0.151782"), new BigDecimal("0.018738398592"))
    );
  }

  @Test
  void rejeitaRegistroInvalidoAntesDaReconstituicao() {
    assertThrows(FaturaInvalida.class, () -> new DescontoContratado(null, BigDecimal.ZERO));
    assertThrows(FaturaInvalida.class, () -> new DescontoContratado(TipoDesconto.VALOR_FIXO, null));
    assertThrows(FaturaInvalida.class,
      () -> new DescontoContratado(TipoDesconto.PERCENTUAL, new BigDecimal("100.0001")));
  }
}
