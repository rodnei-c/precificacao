package br.com.modelagemdominio.rico.produtos.pg;

import br.com.modelagemdominio.rico.produtos.FalhaProduto;
import br.com.modelagemdominio.rico.produtos.Produto;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Leitura em conjunto na conexão de uma operação composta.
 * Quem fornece a conexão mantém sua propriedade, o encerramento e a transação.
 * As implementações não devem abrir outra conexão, fechar a recebida, confirmar ou desfazer a operação.
 */
public interface ProdutosNaConexao {

  /**
   * @param conexao conexão da transação do chamador
   * @param codigos códigos positivos e distintos dos produtos
   * @return produtos materializados e protegidos contra alteração até o fim da transação
   * @throws SQLException quando a consulta falhar
   * @throws FalhaProduto quando algum produto não existir
   */
  Map<Long, Produto> produtos(Connection conexao, List<Long> codigos) throws SQLException, FalhaProduto;

  /**
   * @param conexao conexão da leitura coerente do chamador
   * @param codigos códigos positivos e distintos dos produtos
   * @return produtos materializados sem bloquear suas linhas
   * @throws SQLException quando a consulta falhar
   * @throws FalhaProduto quando algum produto não existir
   */
  Map<Long, Produto> produtosParaConsulta(Connection conexao, List<Long> codigos) throws SQLException, FalhaProduto;
}
