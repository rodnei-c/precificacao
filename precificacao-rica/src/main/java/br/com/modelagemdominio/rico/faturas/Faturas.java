package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.util.List;

/**
 * Define as operações da coleção de faturas, seguindo o contrato de Produtos.
 * A implementação PostgreSQL ainda será criada.
 */
public interface Faturas {

  /**
   * @param codigo identificação positiva da fatura
   * @return fatura encontrada
   * @throws FalhaFatura quando o código for inválido, ausente ou a consulta falhar
   */
  Fatura fatura(long codigo) throws FalhaFatura;

  /**
   * Cadastra os itens como uma única operação e gera código e data de criação.
   * O cadastro deve rejeitar lista vazia e desconto fixo superior ao subtotal inicial.
   * As consultas posteriores devem acompanhar os preços atuais dos produtos.
   *
   * @param itens itens com quantidades positivas
   * @param desconto desconto único da fatura
   * @return fatura cadastrada
   * @throws FalhaFatura quando os dados forem inválidos ou a gravação falhar
   */
  Fatura cadastrar(List<ItemFatura> itens, Desconto desconto) throws FalhaFatura;

  /**
   * @return faturas cadastradas
   * @throws FalhaFatura quando a consulta falhar
   */
  List<Fatura> listar() throws FalhaFatura;
}
