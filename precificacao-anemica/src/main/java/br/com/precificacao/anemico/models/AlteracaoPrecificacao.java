package br.com.precificacao.anemico.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlteracaoPrecificacao(
  long codigo,
  long codigoProduto,
  LocalDateTime dataAlteracao,
  Valores anteriores,
  Valores novos
) {

  public record Valores(BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual) {
  }
}
