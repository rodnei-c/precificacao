package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.clientes.Cep;
import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Cpf;
import br.com.modelagemdominio.rico.clientes.Email;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.LocalizacaoEndereco;
import br.com.modelagemdominio.rico.clientes.NomeCliente;
import br.com.modelagemdominio.rico.clientes.Telefone;
import br.com.modelagemdominio.rico.http.CorpoJson;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.rs.RsWithStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public final class TkCadastroCliente implements TkRegex {

    private final Clientes clientes;
    private final JsonMapper json;
    private final RespostasCliente respostas;

    public TkCadastroCliente(Clientes clientes) {
        this.clientes = clientes;
        this.json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        this.respostas = new RespostasCliente(this.json);
    }

    @Override
    public Response act(RqRegex requisicao) throws IOException {
        CorpoJson corpo = new CorpoJson(requisicao);
        if (!corpo.temTipoJson()) {
            return new RsWithStatus(415);
        }
        String caminho = requisicao.matcher().group();
        Cadastro entrada;
        try {
            entrada = this.json.readValue(corpo.bytes(), Cadastro.class);
        } catch (JacksonException | IOException falha) {
            return this.respostas.formatoInvalido(caminho);
        }
        if (entrada == null) {
            return this.respostas.formatoInvalido(caminho);
        }
        return this.cadastrar(entrada, caminho);
    }

    private Response cadastrar(Cadastro entrada, String caminho) {
        IdentificacaoCliente identificacao;
        ContatoCliente contato;
        Optional<Endereco> endereco;
        try {
            identificacao = new IdentificacaoCliente(new NomeCliente(entrada.nome()), new Cpf(entrada.cpf()));
            contato = new ContatoCliente(new Email(entrada.email()), Telefone.opcional(entrada.telefone()));
            endereco = Optional.ofNullable(entrada.endereco()).map(EnderecoJson::endereco);
        } catch (IllegalArgumentException falha) {
            return this.respostas.dadosInvalidos(falha.getMessage(), caminho);
        }
        try {
            return this.respostas.cadastrado(this.clientes.cadastrar(identificacao, contato, endereco));
        } catch (FalhaCliente falha) {
            return this.respostas.falha(falha, caminho);
        }
    }

    private record Cadastro(String nome, String cpf, String email, String telefone, EnderecoJson endereco) {
    }

    private record EnderecoJson(
        String cep, String logradouro, String numero, String bairro, String complemento, BigDecimal codigoCidade
    ) {

        Endereco endereco() {
            return new Endereco(
                new Cep(this.cep),
                new LocalizacaoEndereco(this.logradouro, this.numero, this.bairro, Optional.ofNullable(this.complemento)),
                new CodigoCidade(this.codigoCidade)
            );
        }
    }
}
