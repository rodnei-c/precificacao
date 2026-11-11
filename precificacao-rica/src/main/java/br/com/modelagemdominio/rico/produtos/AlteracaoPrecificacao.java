package br.com.modelagemdominio.rico.produtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Registro de uma alteração com os valores observados no momento da gravação. */
public record AlteracaoPrecificacao(
  Identidade identidade,
  LocalDateTime dataAlteracao,
  Valores anteriores,
  Valores novos
) {

  public AlteracaoPrecificacao {
    Objects.requireNonNull(identidade);
    Objects.requireNonNull(dataAlteracao);
    Objects.requireNonNull(anteriores);
    Objects.requireNonNull(novos);
  }

  public record Identidade(long codigo, long codigoProduto) {
  }

  public record Valores(BigDecimal custo, BigDecimal precoVenda, BigDecimal percentual) {

    public Valores {
      Objects.requireNonNull(custo);
      Objects.requireNonNull(precoVenda);
      Objects.requireNonNull(percentual);
    }
  }
}
