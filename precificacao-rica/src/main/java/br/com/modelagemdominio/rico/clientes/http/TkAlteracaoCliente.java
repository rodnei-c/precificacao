package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.clientes.AlteracaoCampo;
import br.com.modelagemdominio.rico.clientes.AlteracaoContatos;
import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.Email;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.Telefone;
import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class TkAlteracaoCliente implements TkRegex {

    private static final Set<String> CAMPOS = Set.of("email", "telefone");

    private final Clientes clientes;
    private final JsonMapper json;
    private final RespostasCliente respostas;

    public TkAlteracaoCliente(Clientes clientes) {
        this.clientes = clientes;
        this.json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
        this.respostas = new RespostasCliente(this.json);
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        CorpoJson corpo = new CorpoJson(requisicao);
        if (!corpo.temTipoJson()) {
            return new RsWithStatus(415);
        }
        String caminho = requisicao.matcher().group();
        long codigo;
        JsonNode alteracao;
        try {
            codigo = Long.parseLong(requisicao.matcher().group(1));
            alteracao = this.json.readTree(corpo.bytes());
        } catch (NumberFormatException falha) {
            return this.respostas.codigoInvalido(caminho);
        } catch (JacksonException | IOException falha) {
            return this.respostas.formatoInvalido(caminho);
        }
        return this.alterar(codigo, alteracao, caminho);
    }

    private Response alterar(long codigo, JsonNode alteracao, String caminho) {
        if (alteracao == null || !alteracao.isObject()) {
            return this.respostas.formatoInvalido(caminho);
        }
        JsonNode email = alteracao.path("email");
        JsonNode telefone = alteracao.path("telefone");
        if (!CAMPOS.containsAll(alteracao.propertyNames()) || !campoDeTexto(email) || !campoDeTexto(telefone)) {
            return this.respostas.formatoInvalido(caminho);
        }
        AlteracaoContatos contatos;
        try {
            contatos = new AlteracaoContatos(alteracaoEmail(email), alteracaoTelefone(telefone));
        } catch (IllegalArgumentException falha) {
            return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
        }
        try {
            return this.respostas.cliente(this.clientes.alterarContatos(codigo, contatos));
        } catch (FalhaCliente falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private static boolean campoDeTexto(JsonNode campo) {
        return campo.isMissingNode() || campo.isNull() || campo.isString();
    }

    private static Optional<Email> alteracaoEmail(JsonNode email) {
        if (email.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(new Email(email.isNull() ? null : email.asString()));
    }

    private static AlteracaoCampo<Telefone> alteracaoTelefone(JsonNode telefone) {
        if (telefone.isMissingNode()) {
            return AlteracaoCampo.preservar();
        }
        if (telefone.isNull()) {
            return AlteracaoCampo.remover();
        }
        return AlteracaoCampo.substituir(new Telefone(telefone.asString()));
    }
}
