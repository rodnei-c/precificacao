package br.com.modelagemdominio.rico.infraestrutura.configuracao;

import br.com.modelagemdominio.rico.infraestrutura.banco.FalhaInicializacaoBanco;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Compõe a configuração local com as variáveis de ambiente, que sempre têm prioridade. */
public final class AmbienteRico {

  private final Path diretorio;
  private final Map<String, String> ambiente;

  /** Compõe as fontes de configuração do processo, sem ler o arquivo na construção. */
  public AmbienteRico() {
    this(Path.of(""), System.getenv());
  }

  /**
   * @param diretorio diretório de execução, na raiz do projeto ou no módulo rico
   * @param ambiente variáveis do processo, inclusive valores vazios definidos explicitamente
   */
  public AmbienteRico(Path diretorio, Map<String, String> ambiente) {
    this.diretorio = Objects.requireNonNull(diretorio).toAbsolutePath().normalize();
    this.ambiente = Map.copyOf(ambiente);
  }

  /**
   * @return configuração imutável do arquivo opcional e das variáveis de ambiente
   * @throws FalhaInicializacaoBanco quando o arquivo existente não puder ser lido ou interpretado
   */
  public Map<String, String> valores() {
    Path arquivo = this.diretorio.resolve(".env.rico");
    if (Files.notExists(arquivo) && this.diretorio.getFileName() != null
      && "precificacao-rica".equals(this.diretorio.getFileName().toString())) {
      arquivo = this.diretorio.getParent().resolve(".env.rico");
    }
    var propriedades = new Properties();
    try {
      String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
      try (var leitor = new StringReader(conteudo.startsWith("\uFEFF") ? conteudo.substring(1) : conteudo)) {
        propriedades.load(leitor);
      }
    } catch (NoSuchFileException ausente) {
      // O arquivo é opcional; a aplicação também pode usar somente variáveis de ambiente.
    } catch (IOException | IllegalArgumentException falha) {
      throw new FalhaInicializacaoBanco("Não foi possível ler a configuração local .env.rico."
        + " Confira o formato e as permissões do arquivo.", falha);
    }
    var valores = new HashMap<String, String>();
    for (String nome : propriedades.stringPropertyNames()) {
      valores.put(nome, propriedades.getProperty(nome));
    }
    valores.putAll(this.ambiente);
    return Map.copyOf(valores);
  }
}
