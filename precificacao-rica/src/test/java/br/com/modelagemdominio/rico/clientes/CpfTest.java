package br.com.modelagemdominio.rico.clientes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpfTest {

  @Test
  void normalizaMascaraEEspacosUnicodeParaAMesmaIdentidade() {
    assertEquals(new Cpf("52998224725"), new Cpf("\u2003 529.982.247-25 \u2003"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "10000000604", "12345678909", "10000000280", "20000000370", "00000000191"
  })
  void aceitaDigitoZeroNosDoisRestosPermitidosEPreservaZerosIniciais(String conteudo) {
    assertEquals(conteudo, new Cpf(conteudo).valor());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "52998224733", // Primeiro incorreto, segundo confere com o prefixo recebido.
    "52998224726" // Primeiro correto, segundo incorreto.
  })
  void rejeitaFalhaEmQualquerUmDosDigitosVerificadores(String conteudo) {
    var falha = assertThrows(IllegalArgumentException.class, () -> new Cpf(conteudo));

    assertEquals("Informe um CPF válido.", falha.getMessage());
  }

  @Test
  void rejeitaSequenciaRepetidaMesmoQuandoADivisaoAceitariaOsDigitos() {
    var falha = assertThrows(IllegalArgumentException.class, () -> new Cpf("00000000000"));

    assertEquals("Informe um CPF válido.", falha.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"5299822472", "529-982-247.25", "５２９９８２２４７２５"})
  void rejeitaFormatoInvalidoAntesDeConferirOsDigitos(String conteudo) {
    var falha = assertThrows(IllegalArgumentException.class, () -> new Cpf(conteudo));

    assertEquals("Informe um CPF válido.", falha.getMessage());
  }
}
