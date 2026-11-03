package br.com.modelagemdominio.rico.produtos;

public record Precificacao(
    Dinheiro custo,
    Dinheiro precoVenda,
    Percentual percentual,
    ModalidadePrecificacao modalidade
) {
}
