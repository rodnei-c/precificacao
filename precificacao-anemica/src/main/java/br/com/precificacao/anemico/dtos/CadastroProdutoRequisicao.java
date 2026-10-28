package br.com.precificacao.anemico.dtos;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import java.math.BigDecimal;

/**
 * Dados recebidos no cadastro; o serviço valida as regras de negócio.
 *
 * @param nome nome do produto
 * @param custo custo informado, ou ausente quando deve ser calculado
 * @param precoVenda venda informada, ou ausente quando deve ser calculada
 * @param percentual percentual informado, ou ausente quando deve ser calculado
 * @param modalidadePrecificacao base utilizada para interpretar o percentual
 */
public record CadastroProdutoRequisicao(
  String nome,
  BigDecimal custo,
  BigDecimal precoVenda,
  BigDecimal percentual,
  ModalidadePrecificacao modalidadePrecificacao
) {
}
