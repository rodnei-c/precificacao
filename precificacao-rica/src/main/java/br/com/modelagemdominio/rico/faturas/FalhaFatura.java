package br.com.modelagemdominio.rico.faturas;

/** Falha de cadastro, consulta ou persistência de uma fatura. */
public final class FalhaFatura extends Exception {

  /** Motivos que poderão ser traduzidos pela borda HTTP. */
  public enum Motivo {
    CODIGO_INVALIDO,
    FATURA_AUSENTE,
    PRODUTO_AUSENTE,
    CLIENTE_AUSENTE,
    DADOS_INVALIDOS,
    PERSISTENCIA_INDISPONIVEL
  }

  private final Motivo motivo;

  /** @param motivo classificação da falha */
  public FalhaFatura(Motivo motivo) {
    this(motivo, null);
  }

  /**
   * @param motivo classificação da falha
   * @param causa falha de origem, quando houver
   */
  public FalhaFatura(Motivo motivo, Throwable causa) {
    super(switch (motivo) {
      case CODIGO_INVALIDO -> "O código da fatura deve ser maior que zero.";
      case FATURA_AUSENTE -> "Fatura não encontrada.";
      case PRODUTO_AUSENTE -> "Produto não encontrado.";
      case CLIENTE_AUSENTE -> "Cliente não encontrado.";
      case DADOS_INVALIDOS -> "Os dados da fatura são inválidos.";
      case PERSISTENCIA_INDISPONIVEL -> "Não foi possível acessar os dados da fatura.";
    }, causa);
    this.motivo = motivo;
  }

  /** @return classificação da falha */
  public Motivo motivo() {
    return this.motivo;
  }
}
