package br.com.precificacao.anemico.dtos;

import br.com.precificacao.anemico.enums.FormaPagamento;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FaturaResposta(
  Long codigo,
  LocalDateTime dataCriacao,
  List<Item> itens,
  BigDecimal subtotal,
  BigDecimal descontoAplicado,
  BigDecimal total,
  @JsonInclude(JsonInclude.Include.NON_NULL) Long codigoCliente,
  @JsonInclude(JsonInclude.Include.NON_NULL) FormaPagamento formaPagamento
) {

  public record Item(Long codigoProduto, BigDecimal quantidade, BigDecimal precoVenda, BigDecimal subtotal) {
  }
}
