package br.com.modelagemdominio.rico.clientes;

import java.util.Objects;
import java.util.Optional;

public record AlteracaoCampo<T>(boolean informada, Optional<T> valor) {

    public AlteracaoCampo {
        Objects.requireNonNull(valor);
        if (!informada && valor.isPresent()) {
            throw new IllegalArgumentException("Um campo omitido não pode ter valor.");
        }
    }

    public static <T> AlteracaoCampo<T> preservar() {
        return new AlteracaoCampo<>(false, Optional.empty());
    }

    public static <T> AlteracaoCampo<T> remover() {
        return new AlteracaoCampo<>(true, Optional.empty());
    }

    public static <T> AlteracaoCampo<T> substituir(T valor) {
        return new AlteracaoCampo<>(true, Optional.of(valor));
    }
}
