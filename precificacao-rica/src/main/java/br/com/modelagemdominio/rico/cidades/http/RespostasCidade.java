package br.com.modelagemdominio.rico.cidades.http;

import br.com.modelagemdominio.rico.cidades.Cidade;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.http.ProblemasHttp;
import java.util.List;
import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

final class RespostasCidade {

    private final JsonMapper json;
    private final ProblemasHttp problemas;

    RespostasCidade(JsonMapper json) {
        this.json = json;
        this.problemas = new ProblemasHttp(json);
    }

    Response cadastrada(Cidade cidade) {
        return new RsWithStatus(this.json(new CidadeResposta(cidade)), 201);
    }

    Response lista(List<Cidade> cidades) {
        return this.json(cidades.stream().map(CidadeResposta::new).toList());
    }

    Response dadosInvalidos(String detalhe, String caminho) {
        return this.problemas.resposta(400, "Dados inválidos", detalhe, caminho);
    }

    Response formatoInvalido(String caminho) {
        return this.problemas.resposta(400, "Formato inválido", "Confira o formato do JSON.", caminho);
    }

    Response falha(FalhaCidade falha, String caminho) {
        return switch (falha.motivo()) {
            case NOME_DUPLICADO_NA_UF ->
                this.dadosInvalidos("Já existe uma cidade com esse nome na UF informada.", caminho);
            case PERSISTENCIA_INDISPONIVEL -> this.problemas.resposta(
                503, "Persistência indisponível", "Não foi possível concluir a operação no banco de dados.", caminho
            );
        };
    }

    private Response json(Object dados) {
        return new RsWithType(new RsWithBody(this.json.writeValueAsBytes(dados)), "application/json");
    }

    private record CidadeResposta(long codigo, String nome, String uf) {

        CidadeResposta(Cidade cidade) {
            this(cidade.codigo(), cidade.nome().valor(), cidade.uf().name());
        }
    }
}
