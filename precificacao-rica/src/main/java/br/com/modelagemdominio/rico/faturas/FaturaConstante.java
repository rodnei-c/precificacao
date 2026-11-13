package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Fatura e preços materializados em uma operação, sem consultas por atributo. */
public record FaturaConstante(long codigo, LocalDateTime dataCriacao, CalculoFatura calculo, VinculosFatura vinculos)
  implements Fatura {

  /**
   * @param codigo código persistido
   * @param dataCriacao data gerada no banco
   * @param calculo itens e desconto da leitura
   * @param vinculos referências opcionais da leitura
   */
  public FaturaConstante {
    if (codigo <= 0) {
      throw new FaturaInvalida("O código da fatura deve ser maior que zero.");
    }
    Objects.requireNonNull(dataCriacao);
    Objects.requireNonNull(calculo);
    Objects.requireNonNull(vinculos);
  }

  /** @return itens da leitura, em lista imutável */
  @Override
  public List<ItemFatura> itens() {
    return this.calculo.itens();
  }

  /** @return desconto original da leitura */
  @Override
  public Desconto desconto() {
    return this.calculo.desconto();
  }

  /** @return valores calculados sem I/O adicional */
  @Override
  public ValoresFatura valores() throws FalhaFatura {
    return this.calculo.calcular();
  }
}
