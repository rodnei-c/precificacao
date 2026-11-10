package br.com.modelagemdominio.rico.clientes;

import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import java.util.Objects;

public record Endereco(Cep cep, LocalizacaoEndereco localizacao, CodigoCidade codigoCidade) {

    public Endereco {
        Objects.requireNonNull(cep);
        Objects.requireNonNull(localizacao);
        Objects.requireNonNull(codigoCidade);
    }
}
