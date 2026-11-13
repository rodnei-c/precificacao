package br.com.modelagemdominio.rico.produtos;

import java.util.Optional;

/** Ordem dos preços exigida tanto antes do cálculo da taxa quanto no resultado. */
public record VendaNaoInferiorAoCusto(Dinheiro custo, Dinheiro venda) implements EspecificacaoDePrecificacao {

  @Override
  public Optional<String> violacao() {
    if (this.venda.comparar(this.custo) < 0) {
      return Optional.of("O preço de venda não pode ser inferior ao custo.");
    }
    return Optional.empty();
  }
}
