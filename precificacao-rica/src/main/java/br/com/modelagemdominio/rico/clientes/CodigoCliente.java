package br.com.modelagemdominio.rico.clientes;

/** Referência positiva a um cliente. */
public final class CodigoCliente {

  private final long valor;

  public CodigoCliente(long valor) throws FalhaCliente {
    if (valor <= 0) {
      throw new FalhaCliente(FalhaCliente.Motivo.CODIGO_INVALIDO);
    }
    this.valor = valor;
  }

  public long valor() {
    return this.valor;
  }
}
