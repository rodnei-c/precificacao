package br.com.modelagemdominio.rico.faturas;

import br.com.modelagemdominio.rico.faturas.descontos.Desconto;
import java.util.List;

/**
 * Define as operações da coleção de faturas, seguindo o contrato de Produtos.
 * O cadastro deve gravar cabeçalho e itens como uma única operação.
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
  default Fatura cadastrar(List<ItemFatura> itens, Desconto desconto) throws FalhaFatura {
    return this.cadastrar(new CalculoFatura(itens, desconto), new VinculosFatura());
  }

  /**
   * Deve preparar o cálculo e validar as referências antes de gravar cabeçalho e itens juntos.
   *
   * @param calculo composição validada de itens e desconto
   * @param vinculos cliente e modalidade de pagamento opcionais
   * @return fatura cadastrada com código e data gerados
   * @throws FalhaFatura quando as referências estiverem ausentes ou a gravação falhar
   * @throws FaturaInvalida quando o desconto inicial ultrapassar o subtotal
   */
  Fatura cadastrar(CalculoFatura calculo, VinculosFatura vinculos) throws FalhaFatura;

  /**
   * Prepara os mesmos valores do cadastro, sem gerar identidade ou persistir dados.
   *
   * @param calculo composição validada de itens e desconto
   * @param vinculos cliente e pagamento opcionais, sujeitos às mesmas validações
   * @return valores calculados com os preços observados na operação
   * @throws FalhaFatura quando uma referência estiver ausente ou a leitura falhar
   * @throws FaturaInvalida quando o desconto inicial ultrapassar o subtotal
   */
  ValoresFatura simular(CalculoFatura calculo, VinculosFatura vinculos) throws FalhaFatura;

  /**
   * @return resumos de todas as faturas, por data e código decrescentes
   * @throws FalhaFatura quando a consulta falhar
   */
  default List<ResumoFatura> listar() throws FalhaFatura {
    return this.listar(new FiltrosFaturas());
  }

  /**
   * @param filtros período e cliente, combinados quando informados
   * @return resumos ordenados por data e código decrescentes
   * @throws FalhaFatura quando a leitura falhar
   */
  List<ResumoFatura> listar(FiltrosFaturas filtros) throws FalhaFatura;
}
