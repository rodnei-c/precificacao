package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz falhas de validação, consulta e persistência para respostas HTTP.
 */
@RestControllerAdvice
public class TratadorExcecoes {

  private static final Logger LOG = LoggerFactory.getLogger(TratadorExcecoes.class);

  /**
   * @param excecao violação de regra de negócio
   * @return resposta 400 com a mensagem de validação
   */
  @ExceptionHandler(ValidacaoException.class)
  public ResponseEntity<ProblemDetail> tratarValidacao(ValidacaoException excecao) {
    LOG.debug("Requisição recusada: {}", excecao.getMessage());
    return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", excecao.getMessage());
  }

  /**
   * @param excecao registro não localizado
   * @return resposta 404 sem detalhes de infraestrutura
   */
  @ExceptionHandler(RegistroNaoEncontradoException.class)
  public ResponseEntity<ProblemDetail> tratarRegistroAusente(RegistroNaoEncontradoException excecao) {
    LOG.debug("Consulta sem resultado: {}", excecao.getMessage());
    return resposta(HttpStatus.NOT_FOUND, "Registro não encontrado", excecao.getMessage());
  }

  /**
   * @param excecao corpo JSON ou parâmetro da URL inválido
   * @return resposta 400 sem reproduzir o conteúdo da entrada
   */
  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ProblemDetail> tratarEntradaInvalida(Exception excecao) {
    LOG.debug("Formato de requisição inválido: {}", excecao.getClass().getSimpleName());
    return resposta(
      HttpStatus.BAD_REQUEST, "Formato inválido",
      "Confira o JSON, os valores numéricos, a modalidade de precificação e o código da URL."
    );
  }

  /**
   * @param excecao falha no acesso ao banco
   * @return resposta 503 sem expor SQL, credenciais ou parâmetros
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ProblemDetail> tratarPersistencia(DataAccessException excecao) {
    LOG.error("Falha ao acessar os dados de produtos: {}", excecao.getClass().getSimpleName());
    return resposta(
      HttpStatus.SERVICE_UNAVAILABLE, "Persistência indisponível",
      "Não foi possível concluir a operação no banco de dados."
    );
  }

  private ResponseEntity<ProblemDetail> resposta(HttpStatus status, String titulo, String detalhe) {
    ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
    problema.setTitle(titulo);
    return ResponseEntity.status(status).body(problema);
  }
}
