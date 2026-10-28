package br.com.precificacao.anemico.dtos;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import java.math.BigDecimal;

/**
 * Dados do produto apresentados pela API.
 *
 * @param codigo identificação gerada pelo banco
 * @param nome nome do produto
 * @param custo custo cadastrado
 * @param precoVenda preço de venda cadastrado
 * @param percentual percentual cadastrado
 * @param modalidadePrecificacao base do percentual
 */
public record ProdutoResposta(
  Long codigo,
  String nome,
  BigDecimal custo,
  BigDecimal precoVenda,
  BigDecimal percentual,
  ModalidadePrecificacao modalidadePrecificacao
) {
}
