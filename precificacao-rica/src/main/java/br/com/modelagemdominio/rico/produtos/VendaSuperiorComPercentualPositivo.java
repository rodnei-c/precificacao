package br.com.modelagemdominio.rico.produtos;

import java.util.Optional;

/** Um percentual positivo deve resultar em uma venda acima do custo já arredondado. */
record VendaSuperiorComPercentualPositivo(Dinheiro custo, Dinheiro venda, Percentual percentual)
  implements EspecificacaoDePrecificacao {

  @Override
  public Optional<String> violacao() {
    if (this.percentual.valor().signum() > 0 && this.venda.comparar(this.custo) <= 0) {
      return Optional.of(
        "O percentual positivo deve produzir preço de venda maior que o custo após o arredondamento."
      );
    }
    return Optional.empty();
  }
}
