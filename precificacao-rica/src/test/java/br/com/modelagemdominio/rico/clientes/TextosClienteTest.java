package br.com.modelagemdominio.rico.clientes;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextosClienteTest {

  @Test
  void removeEspacosUnicodeDasPontasEPreservaTextoInterno() {
    var nome = new NomeCliente("\u2003 Ana  d'Ávila \u2003");
    var localizacao = new LocalizacaoEndereco("\u2003 Rua  d'Ávila \u2003", " 12A ", " Centro ",
      Optional.of("\u2003 Sala  2 \u2003"));

    assertEquals("Ana  d'Ávila", nome.valor());
    assertEquals("Rua  d'Ávila", localizacao.logradouro());
    assertEquals("12A", localizacao.numero());
    assertEquals(Optional.of("Sala  2"), localizacao.complemento());
  }

  @Test
  void distingueObrigatorioEOpcionalMesmoComTextoEmBrancoAcimaDoLimite() {
    String vazio = "\u2003".repeat(300);

    var falha = assertThrows(IllegalArgumentException.class, () -> new NomeCliente(vazio));
    var localizacao = new LocalizacaoEndereco("Rua A", "12", "Centro", Optional.of(vazio));

    assertEquals("O campo nome é obrigatório.", falha.getMessage());
    assertEquals(Optional.empty(), localizacao.complemento());
  }

  @Test
  void rejeitaCaractereNuloNoOpcionalComDescricaoDoCampo() {
    var falha = assertThrows(IllegalArgumentException.class, () ->
      new LocalizacaoEndereco("Rua A", "12", "Centro", Optional.of("Sala\0inválida")));

    assertEquals("O campo complemento deve conter texto válido de até 100 caracteres.", falha.getMessage());
  }
}
