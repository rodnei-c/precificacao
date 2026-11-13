package br.com.modelagemdominio.rico.cidades;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SiglaUfTest {

  @Test
  @ResourceLock(Resources.LOCALE)
  void normalizaEspacosUnicodeEMinusculasIndependenteDoIdiomaDoSistema() {
    Locale original = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));

      assertEquals(UnidadeFederativa.PI, new SiglaUf("\u2003 pi \u2003").unidade());
    } finally {
      Locale.setDefault(original);
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"\u2003 \t"})
  void exigeSiglaPresenteSemApresentarCausaDeConversao(String entrada) {
    var falha = assertThrows(IllegalArgumentException.class, () -> new SiglaUf(entrada).unidade());

    assertEquals("Informe a UF da cidade.", falha.getMessage());
    assertNull(falha.getCause());
  }

  @ParameterizedTest
  @ValueSource(strings = {"XX", "P R"})
  void rejeitaSiglaInexistenteOuEspacoInternoPreservandoACausa(String entrada) {
    var falha = assertThrows(IllegalArgumentException.class, () -> new SiglaUf(entrada).unidade());

    assertEquals("Informe uma sigla de UF brasileira válida.", falha.getMessage());
    assertInstanceOf(IllegalArgumentException.class, falha.getCause());
  }
}
