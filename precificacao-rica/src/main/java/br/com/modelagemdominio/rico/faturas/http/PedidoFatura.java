package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.CalculoFatura;
import br.com.modelagemdominio.rico.faturas.FaturaInvalida;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.Quantidade;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.descontos.DescontoContratado;
import br.com.modelagemdominio.rico.faturas.descontos.SemDesconto;
import br.com.modelagemdominio.rico.faturas.descontos.TipoDesconto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/** Contrato JSON compartilhado pelo cadastro e pela simulação, sem acesso ao banco. */
record PedidoFatura(List<Item> itens, DescontoJson desconto, BigDecimal codigoCliente, FormaPagamento formaPagamento) {

  static JsonMapper json() {
    return JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      DeserializationFeature.FAIL_ON_TRAILING_TOKENS).enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
      .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();
  }

  CalculoFatura calculo(Produtos produtos) {
    Desconto desconto = this.desconto == null ? new SemDesconto() : this.desconto.desconto();
    List<ItemFatura> itens = this.itens == null ? List.of() : this.itens.stream().map(item -> item(item, produtos)).toList();
    return new CalculoFatura(itens, desconto);
  }

  VinculosFatura vinculos() {
    return new VinculosFatura(Optional.ofNullable(this.codigoCliente).map(valor -> codigo(valor, "cliente")),
      Optional.ofNullable(this.formaPagamento));
  }

  private static ItemFatura item(Item entrada, Produtos produtos) {
    if (entrada == null) {
      throw new FaturaInvalida("Informe o código e a quantidade de cada item.");
    }
    return new ItemFatura(produtos, codigo(entrada.codigoProduto(), "produto"), new Quantidade(entrada.quantidade()));
  }

  private static long codigo(BigDecimal valor, String entidade) {
    if (valor != null) {
      try {
        return valor.longValueExact();
      } catch (ArithmeticException falha) {
        throw new FaturaInvalida("Informe um código de " + entidade + " inteiro positivo válido.");
      }
    }
    throw new FaturaInvalida("Informe um código de " + entidade + " inteiro positivo válido.");
  }

  private record Item(BigDecimal codigoProduto, BigDecimal quantidade) {
  }

  private record DescontoJson(TipoDesconto tipo, BigDecimal valor) {
    Desconto desconto() {
      return new DescontoContratado(this.tipo, this.valor).desconto();
    }
  }
}
