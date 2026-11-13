package br.com.modelagemdominio.rico.clientes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlteracoesCampoTest {

  @ParameterizedTest
  @MethodSource("operacoes")
  void compoeEscritaSomenteParaCamposInformados(AlteracaoCampo<String> alteracao, boolean informada,
    Map<String, String> esperado) {
    var colunas = new LinkedHashMap<String, String>();
    var escritas = new AtomicInteger();

    var resultado = alteracao.aplicar(colunas, valor -> {
      escritas.incrementAndGet();
      colunas.put("telefone", valor.orElse(null));
      return colunas;
    });

    assertSame(colunas, resultado);
    assertEquals(esperado, colunas);
    assertEquals(informada ? 1 : 0, escritas.get());
    assertEquals(informada, alteracao.informada());
    assertEquals(Optional.ofNullable(esperado.get("telefone")), alteracao.valor());
  }

  private static Stream<Arguments> operacoes() {
    return Stream.of(
      Arguments.of(new CampoPreservado<String>(), false, Map.of()),
      Arguments.of(new CampoRemovido<String>(), true, Collections.singletonMap("telefone", null)),
      Arguments.of(new CampoSubstituido<>("novo"), true, Map.of("telefone", "novo"))
    );
  }

  @Test
  void rejeitaSubstituicaoNulaParaNaoConfundirComRemocao() {
    assertThrows(NullPointerException.class, () -> new CampoSubstituido<String>(null));
  }
}
