package br.com.modelagemdominio.rico.produtos;

import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import java.util.Optional;

public record FiltrosProdutos(
    Optional<String> nome,
    Optional<ModalidadePrecificacao> modalidade,
    Optional<CodigoCategoria> categoria
) {

    public FiltrosProdutos() {
        this(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
