package br.com.modelagemdominio.rico.produtos;

/** Referência positiva a um produto. */
public final class CodigoProduto {

  private final long valor;

  public CodigoProduto(long valor) throws FalhaProduto {
    if (valor <= 0) {
      throw new FalhaProduto(FalhaProduto.Motivo.CODIGO_INVALIDO);
    }
    this.valor = valor;
  }

  public long valor() {
    return this.valor;
  }
}
