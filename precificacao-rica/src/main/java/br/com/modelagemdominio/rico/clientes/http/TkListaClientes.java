package br.com.modelagemdominio.rico.clientes.http;

import br.com.modelagemdominio.rico.clientes.Clientes;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import org.takes.Response;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.TkRegex;
import tools.jackson.databind.json.JsonMapper;

public final class TkListaClientes implements TkRegex {

    private final Clientes clientes;
    private final RespostasCliente respostas;

    public TkListaClientes(Clientes clientes) {
        this.clientes = clientes;
        this.respostas = new RespostasCliente(new JsonMapper());
    }

    @Override
    public Response act(RqRegex requisicao) {
        try {
            return this.respostas.lista(this.clientes.listar());
        } catch (FalhaCliente falha) {
            return this.respostas.falha(falha, requisicao.matcher().group());
        }
    }
}
