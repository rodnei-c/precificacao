package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de uma fatura identificada, com itens e um único desconto.
 * A coleção PostgreSQL materializa os valores de cada operação.
 */
public interface Fatura {

  /** @return código gerado no cadastro */
  long codigo();

  /**
   * @return data e hora de criação
   * @throws FalhaFatura quando os dados não puderem ser consultados
   */
  LocalDateTime dataCriacao() throws FalhaFatura;

  /**
   * @return itens da fatura, sem permitir alteração pela lista devolvida
   * @throws FalhaFatura quando os dados não puderem ser consultados
   */
  List<ItemFatura> itens() throws FalhaFatura;

  /**
   * @return desconto contratado, preservando seu valor original
   * @throws FalhaFatura quando os dados não puderem ser consultados
   */
  Desconto desconto() throws FalhaFatura;

  /**
   * @return cliente e modalidade de pagamento opcionais
   * @throws FalhaFatura quando os vínculos não puderem ser consultados
   */
  VinculosFatura vinculos() throws FalhaFatura;

  /**
   * Deve ser chamado uma vez para compor uma resposta com itens e totais da mesma leitura.
   *
   * @return valores do cálculo, sem nova consulta ao acessar seus campos
   * @throws FalhaFatura quando a fatura ou os produtos não puderem ser consultados
   */
  default ValoresFatura valores() throws FalhaFatura {
    return new CalculoFatura(this.itens(), this.desconto()).calcular();
  }

  /**
   * @return soma dos subtotais com os preços atuais, sem arredondamento
   * @throws FalhaFatura quando a fatura ou seus produtos não puderem ser consultados
   */
  default BigDecimal subtotal() throws FalhaFatura {
    return this.valores().subtotal();
  }

  /**
   * @return desconto aplicável aos preços atuais, sem arredondamento
   * @throws FalhaFatura quando a fatura ou seus produtos não puderem ser consultados
   */
  default BigDecimal descontoAplicado() throws FalhaFatura {
    return this.valores().descontoAplicado();
  }

  /**
   * @return subtotal menos o desconto aplicado, arredondado para duas casas com HALF_EVEN
   * @throws FalhaFatura quando a fatura ou seus produtos não puderem ser consultados
   */
  default BigDecimal total() throws FalhaFatura {
    return this.valores().total();
  }
}
