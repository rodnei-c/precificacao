package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.cidades.Cidade;
import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.cidades.http.TkListaCidades;
import br.com.modelagemdominio.rico.testes.DataSourceSemConexao;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.ds.PGSimpleDataSource;
import org.takes.Take;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rq.RqFake;
import org.takes.rs.RsPrint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaCidadesTest {

    private final JsonMapper json = new JsonMapper();

    @Test
    void respondeComArrayVazioQuandoNaoHaCidades() throws Exception {
        Cidades vazias = new Cidades() {
            @Override
            public Cidade cadastrar(NomeCidade nome, UnidadeFederativa uf) {
                throw new AssertionError("A listagem não deve cadastrar uma cidade.");
            }

            @Override
            public List<Cidade> listar(FiltrosCidades filtros) {
                return List.of();
            }

            @Override
            public boolean existe(CodigoCidade codigo) {
                throw new AssertionError("A listagem não deve consultar uma cidade por código.");
            }
        };
        Take api = new TkFork(new FkRegex("/api/cidades", new TkListaCidades(vazias)));

        var resposta = consultar(api);

        assertTrue(resposta.printHead().lines().anyMatch("Content-Type: application/json"::equalsIgnoreCase));
        assertEquals(this.json.readTree("[]"), this.corpo(200, resposta));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RICO_BANCO_TESTE_URL", matches = "jdbc:postgresql:.*")
    void listaPorCodigoEAplicaFiltrosDeNomeEUf() throws Exception {
        var banco = new PGSimpleDataSource();
        banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        UUID sufixo = UUID.randomUUID();
        String primeiroNome = "Zabelê " + sufixo + " %_";
        String segundoNome = "Água Branca " + sufixo + " XY";
        Take api = AplicacaoRica.rotas(banco);

        try (Connection conexao = banco.getConnection()) {
            long primeiro = reservarCodigo(conexao);
            long segundo = reservarCodigo(conexao);
            try {
                inserir(conexao, segundo, segundoNome, "AL");
                inserir(conexao, primeiro, primeiroNome, "PB");

                JsonNode cidades = this.corpo(200, consultar(api));

                assertTrue(cidades.isArray());
                List<JsonNode> cidadesDoTeste = new ArrayList<>();
                for (JsonNode cidade : cidades) {
                    long codigo = cidade.path("codigo").asLong();
                    if (codigo == primeiro || codigo == segundo) {
                        cidadesDoTeste.add(cidade);
                    }
                }
                JsonNode primeiraCidade = this.json.readTree("""
                    {"codigo":%d,"nome":"%s","uf":"PB"}
                    """.formatted(primeiro, primeiroNome));
                JsonNode segundaCidade = this.json.readTree("""
                    {"codigo":%d,"nome":"%s","uf":"AL"}
                    """.formatted(segundo, segundoNome));
                assertEquals(List.of(primeiraCidade, segundaCidade), cidadesDoTeste);

                this.conferirFiltroPorNome(api, cidades, sufixo, primeiraCidade, segundaCidade);
                this.conferirFiltroPorUf(api, cidades, sufixo, primeiraCidade);
            } finally {
                try (var exclusao = conexao.prepareStatement("DELETE FROM public.cidade WHERE codigo IN (?, ?)")) {
                    exclusao.setLong(1, primeiro);
                    exclusao.setLong(2, segundo);
                    exclusao.executeUpdate();
                }
            }
        }
    }

    @Test
    void informaIndisponibilidadeEmVezDeResponderComListaVazia() throws Exception {
        var banco = new HikariDataSource();
        banco.close();

        JsonNode problema = this.corpo(503, consultar(AplicacaoRica.rotas(banco)));

        assertEquals("Persistência indisponível", problema.path("title").asString());
        assertEquals("/api/cidades", problema.path("instance").asString());
    }

    @Test
    void recusaFiltroAcimaDoLimiteAntesDeAcessarOBanco() throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        var problema = this.corpo(400, consultarPorNome(api, "x".repeat(201)));

        assertEquals("O nome da cidade deve ter no máximo 200 caracteres.", problema.path("detail").asString());
    }

    @Test
    void recusaUfInvalidaAntesDeAcessarOBanco() throws Exception {
        Take api = AplicacaoRica.rotas(new DataSourceSemConexao());

        var problema = this.corpo(400, consultar(api, "?nome=Curitiba&uf=XX"));

        assertEquals("Informe uma sigla de UF brasileira válida.", problema.path("detail").asString());
    }

    private void conferirFiltroPorNome(Take api, JsonNode semFiltro, UUID sufixo, JsonNode primeira, JsonNode segunda)
        throws Exception {
        String trecho = sufixo.toString();
        assertEquals(this.json.createArrayNode().add(primeira).add(segunda),
            this.corpo(200, consultarPorNome(api, trecho.toUpperCase(Locale.ROOT))));
        assertEquals(this.json.createArrayNode().add(segunda),
            this.corpo(200, consultarPorNome(api, "branca " + trecho)));
        assertEquals(this.json.createArrayNode().add(primeira),
            this.corpo(200, consultarPorNome(api, "  " + trecho + " %_  ")));
        assertEquals(this.json.createArrayNode(), this.corpo(200, consultarPorNome(api, trecho + " _")));
        assertEquals(semFiltro, this.corpo(200, consultarPorNome(api, "  ")));
        assertEquals(semFiltro, this.corpo(200, consultarPorNome(api, "")));
    }

    private void conferirFiltroPorUf(Take api, JsonNode semFiltro, UUID sufixo, JsonNode primeira)
        throws Exception {
        var esperadasNaParaiba = this.json.createArrayNode();
        for (JsonNode cidade : semFiltro) {
            if ("PB".equals(cidade.path("uf").asString())) {
                esperadasNaParaiba.add(cidade);
            }
        }

        assertEquals(esperadasNaParaiba, this.corpo(200, consultar(api, "?uf=+pb+")));
        assertEquals(this.json.createArrayNode().add(primeira),
            this.corpo(200, consultar(api, "?nome=" + sufixo + "&uf=PB")));
        assertEquals(this.json.createArrayNode(),
            this.corpo(200, consultar(api, "?nome=branca+" + sufixo + "&uf=PB")));
        assertEquals(this.corpo(200, consultarPorNome(api, sufixo.toString())),
            this.corpo(200, consultar(api, "?nome=" + sufixo + "&uf=++")));
        assertEquals(semFiltro, this.corpo(200, consultar(api, "?uf=")));
    }

    private static RsPrint consultarPorNome(Take api, String nome) throws Exception {
        String termo = URLEncoder.encode(nome, StandardCharsets.UTF_8);
        return consultar(api, "?nome=" + termo);
    }

    private static RsPrint consultar(Take api) throws Exception {
        return consultar(api, "");
    }

    private static RsPrint consultar(Take api, String parametros) throws Exception {
        return new RsPrint(api.act(new RqFake("GET", "/api/cidades" + parametros)));
    }

    private JsonNode corpo(int status, RsPrint resposta) throws Exception {
        assertTrue(resposta.head().iterator().next().startsWith("HTTP/1.1 " + status + " "), resposta::toString);
        return this.json.readTree(resposta.printBody());
    }

    private static long reservarCodigo(Connection conexao) throws SQLException {
        try (var consulta = conexao.prepareStatement(
            "SELECT nextval(pg_get_serial_sequence('public.cidade', 'codigo'))"
        ); var resultado = consulta.executeQuery()) {
            assertTrue(resultado.next());
            return resultado.getLong(1);
        }
    }

    private static void inserir(Connection conexao, long codigo, String nome, String uf) throws SQLException {
        try (var insercao = conexao.prepareStatement("INSERT INTO public.cidade (codigo, nome, uf) VALUES (?, ?, ?)")) {
            insercao.setLong(1, codigo);
            insercao.setString(2, nome);
            insercao.setString(3, uf);
            insercao.executeUpdate();
        }
    }
}
