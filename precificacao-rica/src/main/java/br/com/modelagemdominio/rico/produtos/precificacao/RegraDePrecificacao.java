package br.com.modelagemdominio.rico.produtos.precificacao;

import br.com.modelagemdominio.rico.produtos.ModalidadePrecificacao;
import java.math.BigDecimal;

public interface RegraDePrecificacao {

    ModalidadePrecificacao modalidade();

    BigDecimal precoVenda(BigDecimal custo, BigDecimal percentual) throws PrecificacaoInvalida;

    BigDecimal custo(BigDecimal precoVenda, BigDecimal percentual) throws PrecificacaoInvalida;

    BigDecimal percentual(BigDecimal custo, BigDecimal precoVenda) throws PrecificacaoInvalida;
}
