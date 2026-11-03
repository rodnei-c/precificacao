package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato de uma fatura identificada, com itens e um único desconto.
 * A implementação da entidade e sua persistência ficam para a próxima etapa.
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
   * @return soma dos subtotais com os preços atuais, sem arredondamento
   * @throws FalhaFatura quando a fatura ou seus produtos não puderem ser consultados
   */
  BigDecimal subtotal() throws FalhaFatura;

  /**
   * @return subtotal menos o desconto aplicado, arredondado para duas casas com HALF_EVEN
   * @throws FalhaFatura quando a fatura ou seus produtos não puderem ser consultados
   */
  BigDecimal total() throws FalhaFatura;
}
