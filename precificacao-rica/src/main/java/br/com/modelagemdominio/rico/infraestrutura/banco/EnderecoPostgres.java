package br.com.modelagemdominio.rico.infraestrutura.banco;

/** Endereço explícito do banco, preservando servidores e opções na conexão de manutenção. */
record EnderecoPostgres(String url, String manutencao, NomeBancoPostgres banco) {

  String nome() {
    return this.banco.valor();
  }

  String identificadorSql() {
    return this.banco.identificadorSql();
  }
}
