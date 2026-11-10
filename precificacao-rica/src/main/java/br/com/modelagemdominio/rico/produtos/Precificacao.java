package br.com.modelagemdominio.rico.produtos;

public record Precificacao(
  Dinheiro custo,
  Dinheiro precoVenda,
  Percentual percentual,
  ModalidadePrecificacao modalidade
) {

  public boolean mesmosValores(Precificacao outra) {
    return this.custo.comparar(outra.custo) == 0
      && this.precoVenda.comparar(outra.precoVenda) == 0
      && this.percentual.comparar(outra.percentual) == 0;
  }
}
