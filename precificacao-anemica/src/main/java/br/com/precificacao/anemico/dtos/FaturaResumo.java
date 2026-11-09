package br.com.precificacao.anemico.dtos;

import br.com.precificacao.anemico.enums.FormaPagamento;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

public record FaturaResumo(
  long codigo,
  LocalDateTime dataCriacao,
  long quantidadeItens,
  @JsonInclude(JsonInclude.Include.NON_NULL) Long codigoCliente,
  @JsonInclude(JsonInclude.Include.NON_NULL) FormaPagamento formaPagamento
) {
}
