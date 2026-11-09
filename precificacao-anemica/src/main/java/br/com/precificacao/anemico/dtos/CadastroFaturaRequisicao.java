package br.com.precificacao.anemico.dtos;

import br.com.precificacao.anemico.enums.FormaPagamento;
import br.com.precificacao.anemico.enums.TipoDesconto;
import java.math.BigDecimal;
import java.util.List;

public record CadastroFaturaRequisicao(
  List<Item> itens, Desconto desconto, BigDecimal codigoCliente, FormaPagamento formaPagamento
) {

  public record Item(BigDecimal codigoProduto, BigDecimal quantidade) {
  }

  public record Desconto(TipoDesconto tipo, BigDecimal valor) {
  }
}
