package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelefoneOpcionalTest {

  @Test
  void mantemAusenciaParaNuloEBrancoMesmoAcimaDoLimiteDoTelefone() {
    String ausente = null;

    assertEquals(Optional.empty(), new TelefoneOpcional(ausente).valor());
    assertEquals(Optional.empty(), new TelefoneOpcional("\u2003".repeat(40)).valor());
  }

  @Test
  void compoeTelefonePreservandoFormatacaoEApenasRemovendoEspacosDasPontas() {
    var telefone = new TelefoneOpcional(Optional.of("\u2003 +55 (48) 99999-0000 \u2003"));

    assertEquals(Optional.of(new Telefone("+55 (48) 99999-0000")), telefone.valor());
  }

  @Test
  void conteudoInvalidoContinuaSendoRejeitadoPelaRegraDoTelefone() {
    var telefone = new TelefoneOpcional("9999-0000");

    var falha = assertThrows(IllegalArgumentException.class, telefone::valor);

    assertEquals("Informe um telefone fixo ou celular no formato brasileiro, com DDD.", falha.getMessage());
  }
}
