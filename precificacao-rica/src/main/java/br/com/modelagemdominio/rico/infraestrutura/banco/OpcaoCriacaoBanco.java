package br.com.modelagemdominio.rico.infraestrutura.banco;

final class OpcaoCriacaoBanco {

  private final String valor;

  OpcaoCriacaoBanco(String valor) {
    this.valor = valor;
  }

  boolean habilitada() {
    String opcao = this.valor == null ? "" : this.valor.strip();
    if ("true".equalsIgnoreCase(opcao)) {
      return true;
    }
    if ("false".equalsIgnoreCase(opcao)) {
      return false;
    }
    throw new FalhaInicializacaoBanco("Defina RICO_DB_CRIAR_SE_AUSENTE como true ou false.");
  }
}
