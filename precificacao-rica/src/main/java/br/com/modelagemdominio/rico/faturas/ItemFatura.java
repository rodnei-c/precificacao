package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.math.BigDecimal;

/** Item que relaciona um produto a uma quantidade e calcula seu subtotal. */
public final class ItemFatura {

  private final Produtos produtos;
  private final long codigoProduto;
  private final Quantidade quantidade;

  /**
   * Mantém a referência à coleção para consultar o produto a cada cálculo.
   * A construção não consulta o banco nem guarda uma cópia do preço unitário.
   *
   * @param produtos coleção usada para recuperar o estado atual do produto
   * @param codigoProduto identificação positiva do produto
   * @param quantidade quantidade validada do item
   * @throws FaturaInvalida quando faltar uma dependência ou o código não for positivo
   */
  public ItemFatura(Produtos produtos, long codigoProduto, Quantidade quantidade) {
    if (produtos == null || quantidade == null) {
      throw new FaturaInvalida("Informe a coleção de produtos e a quantidade do item.");
    }
    if (codigoProduto <= 0) {
      throw new FaturaInvalida("O código do produto deve ser maior que zero.");
    }
    this.produtos = produtos;
    this.codigoProduto = codigoProduto;
    this.quantidade = quantidade;
  }

  /** @return identificação do produto, sem consultar o banco */
  public long codigoProduto() {
    return this.codigoProduto;
  }

  /** @return quantidade do item */
  public Quantidade quantidade() {
    return this.quantidade;
  }

  /**
   * @return produto recuperado na consulta atual
   * @throws FalhaProduto quando o produto não existir ou sua leitura falhar
   */
  public Produto produto() throws FalhaProduto {
    return this.produtos.produto(this.codigoProduto);
  }

  /**
   * @return preço atual de venda multiplicado pela quantidade, sem arredondamento
   * @throws FalhaProduto quando o produto não existir ou sua leitura falhar
   */
  public BigDecimal subtotal() throws FalhaProduto {
    return produto().precificacao().precoVenda().valor().multiply(this.quantidade.valor());
  }
}
