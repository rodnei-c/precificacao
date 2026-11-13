package br.com.precificacao.anemico.dtos;

import java.math.BigDecimal;
import java.util.List;

public record ValoresFaturaResposta(
  List<FaturaResposta.Item> itens,
  BigDecimal subtotal,
  BigDecimal descontoAplicado,
  BigDecimal total
) {
}
