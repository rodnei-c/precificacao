package br.com.modelagemdominio.rico.produtos;

public record Precificacao(
    Dinheiro custo,
    Dinheiro precoVenda,
    Percentual percentual,
    ModalidadePrecificacao modalidade
) {

    public boolean mesmosValores(Precificacao outra) {
        return this.custo.valor().compareTo(outra.custo.valor()) == 0
            && this.precoVenda.valor().compareTo(outra.precoVenda.valor()) == 0
            && this.percentual.valor().compareTo(outra.percentual.valor()) == 0;
    }
}
