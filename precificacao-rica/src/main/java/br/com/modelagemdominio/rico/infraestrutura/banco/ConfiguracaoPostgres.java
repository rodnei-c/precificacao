package br.com.modelagemdominio.rico.infraestrutura.banco;

import java.util.Map;

/** Interpreta a configuração sem abrir conexões. */
record ConfiguracaoPostgres(ConexoesPostgres conexoes, EnderecoPostgres endereco, boolean criarSeAusente) {

  ConfiguracaoPostgres(Map<String, String> ambiente) {
    this(
      ambiente.getOrDefault("RICO_DB_URL", "jdbc:postgresql://localhost:5432/precificacao_rica"),
      ambiente.getOrDefault("RICO_DB_USER", "postgres"), ambiente.get("DB_PASSWORD"),
      new OpcaoCriacaoBanco(ambiente.getOrDefault("RICO_DB_CRIAR_SE_AUSENTE", "true")).habilitada(),
      ambiente.getOrDefault("RICO_DB_MANUTENCAO", "postgres")
    );
  }

  ConfiguracaoPostgres(String url, String usuario, String senha, boolean criarSeAusente, String manutencao) {
    this(new ConexoesPostgres(usuario, senha), new UrlPostgres(url, manutencao).endereco(), criarSeAusente);
  }
}
