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
import br.com.modelagemdominio.rico.numeros.InteiroExato;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Contrato JSON compartilhado pelo cadastro e pela simulação, sem acesso ao banco. */
record PedidoFatura(List<Item> itens, DescontoJson desconto, BigDecimal codigoCliente, FormaPagamento formaPagamento) {

  CalculoFatura calculo(Produtos produtos) {
    Desconto desconto = this.desconto == null ? new SemDesconto() : this.desconto.desconto();
    List<Item> entradas = this.itens == null ? List.of() : this.itens;
    List<ItemFatura> itens = entradas.stream().map(item -> {
      if (item == null) {
        throw new FaturaInvalida("Informe o código e a quantidade de cada item.");
      }
      return item.comProdutos(produtos);
    }).toList();
    return new CalculoFatura(itens, desconto);
  }

  VinculosFatura vinculos() {
    var cliente = Optional.ofNullable(this.codigoCliente)
      .map(valor -> new CodigoDoPedido(valor, "Informe um código de cliente inteiro positivo válido.").valor());
    return new VinculosFatura(cliente, Optional.ofNullable(this.formaPagamento));
  }

  private record Item(BigDecimal codigoProduto, BigDecimal quantidade) {
    ItemFatura comProdutos(Produtos produtos) {
      long codigo = new CodigoDoPedido(this.codigoProduto, "Informe um código de produto inteiro positivo válido.")
        .valor();
      return new ItemFatura(produtos, codigo, new Quantidade(this.quantidade));
    }
  }

  private record CodigoDoPedido(InteiroExato inteiro) {
    CodigoDoPedido(BigDecimal valor, String mensagem) {
      this(new InteiroExato(valor, mensagem));
    }

    long valor() {
      try {
        return this.inteiro.valor();
      } catch (IllegalArgumentException falha) {
        throw new FaturaInvalida(falha.getMessage());
      }
    }
  }

  private record DescontoJson(TipoDesconto tipo, BigDecimal valor) {
    Desconto desconto() {
      return new DescontoContratado(this.tipo, this.valor).desconto();
    }
  }
}
