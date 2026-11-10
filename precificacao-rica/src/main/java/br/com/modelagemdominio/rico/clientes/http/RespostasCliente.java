package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.Endereco;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.Telefone;
import br.com.modelagemdominio.rico.http.ProblemasHttp;
import java.util.List;
import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

final class RespostasCliente {

    private final JsonMapper json;
    private final ProblemasHttp problemas;

    RespostasCliente(JsonMapper json) {
        this.json = json;
        this.problemas = new ProblemasHttp(json);
    }

    Response cadastrado(Cliente cliente) {
        return new RsWithStatus(this.cliente(cliente), 201);
    }

    Response cliente(Cliente cliente) {
        return this.json(new ClienteResposta(cliente));
    }

    Response lista(List<Cliente> clientes) {
        return this.json(clientes.stream().map(ClienteResposta::new).toList());
    }

    Response dadosInvalidos(String detalhe, String caminho) {
        return this.problemas.resposta(400, "Dados inválidos", detalhe, caminho);
    }

    Response codigoInvalido(String caminho) {
        return this.dadosInvalidos("Informe um código de cliente inteiro positivo válido.", caminho);
    }

    Response formatoInvalido(String caminho) {
        return this.problemas.resposta(400, "Formato inválido", "Confira o formato do JSON.", caminho);
    }

    Response falha(FalhaCliente falha, String caminho) {
        return switch (falha.motivo()) {
            case CPF_OU_EMAIL_DUPLICADO ->
                this.dadosInvalidos("Já existe um cliente com o CPF ou o e-mail informado.", caminho);
            case EMAIL_DUPLICADO ->
                this.dadosInvalidos("Já existe um cliente com o e-mail informado.", caminho);
            case CIDADE_AUSENTE ->
                this.problemas.resposta(404, "Registro não encontrado", "Cidade não encontrada.", caminho);
            case CODIGO_INVALIDO -> this.codigoInvalido(caminho);
            case CLIENTE_AUSENTE ->
                this.problemas.resposta(404, "Registro não encontrado", "Cliente não encontrado.", caminho);
            case PERSISTENCIA_INDISPONIVEL -> this.problemas.resposta(
                503, "Persistência indisponível", "Não foi possível concluir a operação no banco de dados.", caminho
            );
        };
    }

    private Response json(Object dados) {
        return new RsWithType(new RsWithBody(this.json.writeValueAsBytes(dados)), "application/json");
    }

    private record ClienteResposta(
        long codigo, String nome, String cpf, String email, String telefone, EnderecoResposta endereco
    ) {

        ClienteResposta(Cliente cliente) {
            this(cliente.codigo(), cliente.identificacao().nome().valor(), cliente.identificacao().cpf().valor(),
                cliente.contato().email().valor(), cliente.contato().telefone().map(Telefone::valor).orElse(null),
                cliente.endereco().map(EnderecoResposta::new).orElse(null));
        }
    }

    private record EnderecoResposta(
        String cep, String logradouro, String numero, String bairro, String complemento, long codigoCidade
    ) {

        EnderecoResposta(Endereco endereco) {
            this(endereco.cep().valor(), endereco.localizacao().logradouro(), endereco.localizacao().numero(),
                endereco.localizacao().bairro(), endereco.localizacao().complemento().orElse(null),
                endereco.codigoCidade().valor());
        }
    }
}
