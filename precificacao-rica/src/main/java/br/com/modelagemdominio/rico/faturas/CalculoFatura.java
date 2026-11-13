package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import br.com.modelagemdominio.rico.faturas.taxas.SemTaxa;
import br.com.modelagemdominio.rico.faturas.taxas.TaxaPagamento;
import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Compõe os itens e o desconto para preparar ou recalcular uma fatura. */
public final class CalculoFatura {

  private final List<ItemFatura> itens;
  private final Desconto desconto;

  /**
   * Valida a composição sem consultar os produtos.
   *
   * @param itens lista não vazia, sem itens nulos ou produtos repetidos
   * @param desconto desconto único, inclusive SemDesconto para ausência
   * @throws FaturaInvalida quando a composição não atender às regras
   */
  public CalculoFatura(List<ItemFatura> itens, Desconto desconto) {
    if (itens == null || itens.isEmpty()) {
      throw new FaturaInvalida("Informe pelo menos um item na fatura.");
    }
    if (desconto == null) {
      throw new FaturaInvalida("Informe o desconto ou a ausência de desconto da fatura.");
    }
    var produtos = new HashSet<Long>();
    for (ItemFatura item : itens) {
      if (item == null) {
        throw new FaturaInvalida("Informe o código e a quantidade de cada item.");
      }
      if (!produtos.add(item.codigoProduto())) {
        throw new FaturaInvalida("Informe cada produto uma única vez na fatura.");
      }
    }
    this.itens = List.copyOf(itens);
    this.desconto = desconto;
  }

  /** @return itens validados, em uma lista imutável e sem consultar produtos */
  public List<ItemFatura> itens() {
    return this.itens;
  }

  /** @return desconto contratado, preservado entre os cálculos */
  public Desconto desconto() {
    return this.desconto;
  }

  /**
   * Prepara cadastro ou simulação e rejeita desconto fixo superior ao subtotal inicial.
   *
   * @return valores calculados com o desconto inicial validado
   * @throws FalhaFatura quando um produto estiver ausente ou sua leitura falhar
   * @throws FaturaInvalida quando o desconto inicial ultrapassar o subtotal
   */
  public ValoresFatura preparar() throws FalhaFatura {
    return this.preparar(new SemTaxa());
  }

  /**
   * Prepara cadastro ou simulação, aplicando a taxa recebida e rejeitando desconto fixo superior
   * ao subtotal inicial.
   *
   * @param taxaPagamento regra de taxa ou ausência de taxa
   * @return valores calculados com o desconto inicial validado
   * @throws FalhaFatura quando um produto estiver ausente ou sua leitura falhar
   * @throws FaturaInvalida quando o desconto inicial ultrapassar o subtotal
   */
  public ValoresFatura preparar(TaxaPagamento taxaPagamento) throws FalhaFatura {
    ValoresFatura valores = this.calcular(taxaPagamento);
    this.desconto.validarSubtotalInicial(valores.subtotal());
    return valores;
  }

  /**
   * Consulta cada item uma vez e aplica o desconto aos preços observados.
   * A leitura coerente de vários produtos deve ser garantida pela coleção recebida nos itens.
   *
   * @return valores constantes desta operação; uma nova chamada consulta os produtos novamente
   * @throws FalhaFatura quando um produto estiver ausente ou sua leitura falhar
   */
  public ValoresFatura calcular() throws FalhaFatura {
    return this.calcular(new SemTaxa());
  }

  /**
   * Consulta cada item uma vez e aplica o desconto e a taxa aos preços observados.
   *
   * @param taxaPagamento regra de taxa ou ausência de taxa
   * @return valores constantes desta operação; uma nova chamada consulta os produtos novamente
   * @throws FalhaFatura quando um produto estiver ausente ou sua leitura falhar
   */
  public ValoresFatura calcular(TaxaPagamento taxaPagamento) throws FalhaFatura {
    var calculados = new ArrayList<ValoresItemFatura>(this.itens.size());
    try {
      for (ItemFatura item : this.itens) {
        calculados.add(item.valores());
      }
    } catch (FalhaProduto falha) {
      throw new FalhaFatura(falha.motivo() == FalhaProduto.Motivo.PRODUTO_AUSENTE
        ? FalhaFatura.Motivo.PRODUTO_AUSENTE : FalhaFatura.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
    return new ValoresFatura(calculados, this.desconto, taxaPagamento);
  }
}
