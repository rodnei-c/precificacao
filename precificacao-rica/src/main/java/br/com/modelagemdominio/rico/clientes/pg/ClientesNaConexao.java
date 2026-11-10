package br.com.modelagemdominio.rico.clientes.pg;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Verifica referências na conexão de uma operação composta.
 * Quem fornece a conexão mantém sua propriedade, o encerramento e a transação.
 * As implementações não devem abrir outra conexão, fechar a recebida, confirmar ou desfazer a operação.
 */
public interface ClientesNaConexao {

  /**
   * @param conexao conexão da transação do chamador
   * @param codigo código positivo do cliente
   * @return se o cliente existe, protegendo sua referência até o fim da transação
   * @throws SQLException quando a consulta falhar
   */
  boolean existe(Connection conexao, long codigo) throws SQLException;

  /**
   * @param conexao conexão da leitura coerente do chamador
   * @param codigo código positivo do cliente
   * @return se o cliente existe, sem bloquear sua linha
   * @throws SQLException quando a consulta falhar
   */
  boolean existeParaConsulta(Connection conexao, long codigo) throws SQLException;
}
