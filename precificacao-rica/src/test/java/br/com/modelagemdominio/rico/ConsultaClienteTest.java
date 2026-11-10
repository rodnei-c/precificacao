package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.takes.Take;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaClienteTest {

    private final JsonMapper json = new JsonMapper();

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808"})
    void rejeitaCodigoInvalidoAntesDeAcessarOBanco(String codigo) throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        JsonNode problema = this.problema(api, codigo, 400);

        assertEquals("Informe um código de cliente inteiro positivo válido.", problema.path("detail").asString());
    }

    @Test
    void informaIndisponibilidadeAoConsultarCliente() throws Exception {
        var banco = new HikariDataSource();
        banco.close();

        JsonNode problema = this.problema(AplicacaoRica.rotas(banco), "1", 503);

        assertEquals("Persistência indisponível", problema.path("title").asString());
    }

    private JsonNode problema(Take api, String codigo, int status) throws Exception {
        String caminho = "/api/clientes/" + codigo;
        var resposta = new RsPrint(api.act(new RqFake("GET", caminho)));
        assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
        JsonNode problema = this.json.readTree(resposta.printBody());
        assertEquals(caminho, problema.path("instance").asString());
        return problema;
    }
}
