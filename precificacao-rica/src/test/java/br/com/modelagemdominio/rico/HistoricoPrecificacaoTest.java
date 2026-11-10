package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricoPrecificacaoTest {

    @Test
    void rejeitaCodigoInvalidoAntesDeConsultarOBanco() throws Exception {
        var api = AplicacaoRica.rotas(new DataSourceSemConexao());

        for (String codigo : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            var resposta = api.act(new RqFake("GET", "/api/produtos/" + codigo + "/historico-precificacao"));
            assertEquals(400, status(resposta.head().iterator().next()));
        }
    }

    @Test
    void informaIndisponibilidadeEmVezDeRetornarHistoricoVazio() throws Exception {
        var banco = new HikariDataSource();
        banco.close();
        String caminho = "/api/produtos/1/historico-precificacao";

        var resposta = new RsPrint(AplicacaoRica.rotas(banco).act(new RqFake("GET", caminho)));

        assertEquals(503, status(resposta.head().iterator().next()));
        assertEquals(caminho, new JsonMapper().readTree(resposta.printBody()).path("instance").asString());
    }

    private static int status(String linha) {
        return Integer.parseInt(linha.split(" ", 3)[1]);
    }
}
