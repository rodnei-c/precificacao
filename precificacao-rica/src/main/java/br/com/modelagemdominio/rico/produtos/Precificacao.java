package br.com.modelagemdominio.rico.produtos;

import java.util.Objects;

/** Valores finais de uma precificação, com a coerência entre eles protegida na construção. */
public record Precificacao(
  Dinheiro custo,
  Dinheiro precoVenda,
  Percentual percentual,
  ModalidadePrecificacao modalidade
) {

  private static final Percentual LIMITE_MARGEM = new Percentual(100);

  public Precificacao {
    Objects.requireNonNull(custo);
    Objects.requireNonNull(precoVenda);
    Objects.requireNonNull(percentual);
    Objects.requireNonNull(modalidade);

    new VendaNaoInferiorAoCusto(custo, precoVenda)
      .e(new VendaSuperiorComPercentualPositivo(custo, precoVenda, percentual))
      .exigir();
    if (modalidade == ModalidadePrecificacao.MARGEM_SOBRE_VENDA && percentual.comparar(LIMITE_MARGEM) >= 0) {
      throw new IllegalArgumentException("A margem sobre a venda deve ser inferior a 100%.");
    }
  }

  public boolean mesmosValores(Precificacao outra) {
    return this.custo.comparar(outra.custo) == 0
      && this.precoVenda.comparar(outra.precoVenda) == 0
      && this.percentual.comparar(outra.percentual) == 0;
  }
}
