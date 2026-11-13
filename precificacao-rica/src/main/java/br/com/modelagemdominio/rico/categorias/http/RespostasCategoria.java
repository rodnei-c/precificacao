package br.com.modelagemdominio.rico.categorias.http;

import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import br.com.modelagemdominio.rico.http.ProblemasHttp;
import java.util.List;
import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

final class RespostasCategoria {

    private final JsonMapper json;
    private final ProblemasHttp problemas;

    RespostasCategoria(JsonMapper json) {
        this.json = json;
        this.problemas = new ProblemasHttp(json);
    }

    Response cadastrada(Categoria categoria) {
        return new RsWithStatus(this.categoria(categoria), 201);
    }

    Response categoria(Categoria categoria) {
        return this.json(new CategoriaResposta(categoria));
    }

    Response lista(List<Categoria> categorias) {
        return this.json(categorias.stream().map(CategoriaResposta::new).toList());
    }

    Response dadosInvalidos(String detalhe, String caminho) {
        return this.problemas.resposta(400, "Dados inválidos", detalhe, caminho);
    }

    Response formatoInvalido(String caminho) {
        return this.problemas.resposta(
            400, "Formato inválido", "Confira o formato do JSON e os parâmetros da URL.", caminho
        );
    }

    Response falha(FalhaCategoria falha, String caminho) {
        return switch (falha.motivo()) {
            case NOME_DUPLICADO -> this.dadosInvalidos("Já existe uma categoria com esse nome.", caminho);
            case CATEGORIA_AUSENTE -> this.problemas.resposta(
                404, "Registro não encontrado", "Categoria não encontrada.", caminho
            );
            case PERSISTENCIA_INDISPONIVEL -> this.problemas.resposta(
                503, "Persistência indisponível", "Não foi possível concluir a operação no banco de dados.", caminho
            );
        };
    }

    private Response json(Object conteudo) {
        return new RsWithType(new RsWithBody(this.json.writeValueAsBytes(conteudo)), "application/json");
    }

    private record CategoriaResposta(long codigo, String nome) {

        CategoriaResposta(Categoria categoria) {
            this(categoria.codigo(), categoria.nome().valor());
        }
    }
}
