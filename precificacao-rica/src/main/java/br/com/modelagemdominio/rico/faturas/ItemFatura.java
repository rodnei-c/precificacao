package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import br.com.modelagemdominio.rico.produtos.Produtos;
import java.math.BigDecimal;

/** Item que relaciona um produto a uma quantidade e calcula seu subtotal. */
public final class ItemFatura {

  private final ConsultaProduto consulta;
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
    this(produtos == null ? null : () -> produtos.produto(codigoProduto), codigoProduto, quantidade);
  }

  /**
   * Compõe um produto já materializado na leitura da operação, sem nova consulta.
   *
   * @param produto produto observado na operação
   * @param quantidade quantidade validada
   */
  public ItemFatura(Produto produto, Quantidade quantidade) {
    this(produto == null ? null : () -> produto, produto == null ? 0 : produto.codigo(), quantidade);
  }

  private ItemFatura(ConsultaProduto consulta, long codigoProduto, Quantidade quantidade) {
    if (consulta == null || quantidade == null) {
      throw new FaturaInvalida("Informe a coleção de produtos e a quantidade do item.");
    }
    if (codigoProduto <= 0) {
      throw new FaturaInvalida("O código do produto deve ser maior que zero.");
    }
    this.consulta = consulta;
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
    return this.consulta.produto();
  }

  /**
   * @return preço atual de venda multiplicado pela quantidade, sem arredondamento
   * @throws FalhaProduto quando o produto não existir ou sua leitura falhar
   */
  public BigDecimal subtotal() throws FalhaProduto {
    return this.valores().subtotal();
  }

  /**
   * Materializa o preço uma única vez para todos os valores deste cálculo do item.
   *
   * @return valores imutáveis, sem novas consultas ao acessar seus campos
   * @throws FalhaProduto quando o produto não existir ou sua leitura falhar
   */
  public ValoresItemFatura valores() throws FalhaProduto {
    return new ValoresItemFatura(this, this.produto().precificacao().precoVenda().valor());
  }

  @FunctionalInterface
  private interface ConsultaProduto {
    Produto produto() throws FalhaProduto;
  }
}
