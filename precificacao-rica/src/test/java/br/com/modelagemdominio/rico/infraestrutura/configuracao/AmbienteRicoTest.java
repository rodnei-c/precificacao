package br.com.modelagemdominio.rico.infraestrutura.configuracao;

import br.com.modelagemdominio.rico.infraestrutura.banco.FalhaInicializacaoBanco;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmbienteRicoTest {

  @TempDir
  Path raiz;

  @Test
  void ambientePrevaleceEArquivoCompletaConfiguracaoSemAlterarAsFontes() throws Exception {
    Files.writeString(this.raiz.resolve(".env.rico"), """
      # Configuração local
      RICO_DB_USER=arquivo
      DB_PASSWORD=senha-do-arquivo
      RICO_DB_URL=jdbc:postgresql://localhost/base
      """);
    var variaveis = new HashMap<>(Map.of("RICO_DB_USER", "ambiente", "DB_PASSWORD", "senha-do-ambiente"));
    var configuracao = new AmbienteRico(this.raiz, variaveis);
    variaveis.put("DB_PASSWORD", "alterada-depois-da-composicao");

    Map<String, String> valores = configuracao.valores();

    assertEquals(Map.of("RICO_DB_USER", "ambiente", "DB_PASSWORD", "senha-do-ambiente",
      "RICO_DB_URL", "jdbc:postgresql://localhost/base"), valores);
    assertEquals("alterada-depois-da-composicao", variaveis.get("DB_PASSWORD"));
    assertThrows(UnsupportedOperationException.class, () -> valores.put("DB_PASSWORD", "outra"));
  }

  @Test
  void variavelVaziaDefinidaExplicitamenteTambemPrevalece() throws Exception {
    Files.writeString(this.raiz.resolve(".env.rico"), "DB_PASSWORD=senha-local\n");

    assertEquals("", new AmbienteRico(this.raiz, Map.of("DB_PASSWORD", "")).valores().get("DB_PASSWORD"));
  }

  @Test
  void encontraArquivoDaRaizAoExecutarModuloEPreservaUtf8() throws Exception {
    Path modulo = Files.createDirectory(this.raiz.resolve("precificacao-rica"));
    Files.writeString(this.raiz.resolve(".env.rico"), "\uFEFFDB_PASSWORD=senhá#com=igual\n");

    assertEquals(Map.of("DB_PASSWORD", "senhá#com=igual"), new AmbienteRico(modulo, Map.of()).valores());
  }

  @Test
  void arquivoNoDiretorioAtualTemPreferenciaSobreOArquivoDaRaiz() throws Exception {
    Path modulo = Files.createDirectory(this.raiz.resolve("precificacao-rica"));
    Files.writeString(this.raiz.resolve(".env.rico"), "DB_PASSWORD=raiz\nRICO_DB_USER=raiz\n");
    Files.writeString(modulo.resolve(".env.rico"), "DB_PASSWORD=modulo\n");

    assertEquals(Map.of("DB_PASSWORD", "modulo"), new AmbienteRico(modulo, Map.of()).valores());
  }

  @Test
  void arquivoAusentePreservaAmbienteSemBuscarConfiguracaoDeOutroProjeto() throws Exception {
    Path outroProjeto = Files.createDirectory(this.raiz.resolve("outro-projeto"));
    Files.writeString(this.raiz.resolve(".env.rico"), "DB_PASSWORD=outro-projeto\n");
    var ambiente = Map.of("DB_PASSWORD", "ambiente");

    assertEquals(ambiente, new AmbienteRico(outroProjeto, ambiente).valores());
  }

  @Test
  void falhaDeLeituraEInformadaAoSolicitarOsValores() throws Exception {
    Files.createDirectory(this.raiz.resolve(".env.rico"));
    var configuracao = new AmbienteRico(this.raiz, Map.of());

    var falha = assertThrows(FalhaInicializacaoBanco.class, configuracao::valores);

    assertEquals("Não foi possível ler a configuração local .env.rico."
      + " Confira o formato e as permissões do arquivo.", falha.getMessage());
    assertInstanceOf(IOException.class, falha.getCause());
  }

  @Test
  void formatoInvalidoNaoExpoeConteudoNaMensagem() throws Exception {
    Files.writeString(this.raiz.resolve(".env.rico"), "DB_PASSWORD=segredo\\uXYZW\n");

    var falha = assertThrows(FalhaInicializacaoBanco.class, () -> new AmbienteRico(this.raiz, Map.of()).valores());

    assertInstanceOf(IllegalArgumentException.class, falha.getCause());
    assertFalse(falha.getMessage().contains("segredo"));
  }
}
