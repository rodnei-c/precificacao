package br.com.modelagemdominio.rico.produtos;

import java.util.List;

public interface HistoricoDePrecificacao {

    List<AlteracaoPrecificacao> doProduto(long codigoProduto) throws FalhaProduto;
}
