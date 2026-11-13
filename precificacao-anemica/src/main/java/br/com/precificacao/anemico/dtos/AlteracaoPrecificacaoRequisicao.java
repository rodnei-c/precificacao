package br.com.precificacao.anemico.dtos;

import java.math.BigDecimal;

public record AlteracaoPrecificacaoRequisicao(
  BigDecimal custo,
  BigDecimal precoVenda,
  BigDecimal percentual
) {
}
