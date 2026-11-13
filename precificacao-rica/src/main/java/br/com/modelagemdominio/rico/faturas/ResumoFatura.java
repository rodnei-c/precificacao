package br.com.modelagemdominio.rico.faturas;

import java.time.LocalDateTime;
import java.util.Objects;

/** Valores da listagem, sem carregar itens individuais ou preços dos produtos. */
public record ResumoFatura(long codigo, LocalDateTime dataCriacao, long quantidadeItens, VinculosFatura vinculos) {

  /**
   * @param codigo identidade persistida da fatura
   * @param dataCriacao data gerada no cadastro
   * @param quantidadeItens número de produtos distintos, sem somar suas quantidades
   * @param vinculos referências opcionais da fatura
   */
  public ResumoFatura {
    if (codigo <= 0 || quantidadeItens < 0) {
      throw new FaturaInvalida("O resumo da fatura deve ter código positivo e contagem não negativa.");
    }
    Objects.requireNonNull(dataCriacao);
    Objects.requireNonNull(vinculos);
  }
}
