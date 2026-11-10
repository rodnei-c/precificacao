package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.categorias.http.TkListaCategorias;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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

class ListaCategoriasTest {

    private final JsonMapper json = new JsonMapper();

    @Test
    void devolveUmArrayVazioQuandoNaoHaCategorias() throws Exception {
        Categorias vazias = new Categorias() {
            @Override
            public Categoria cadastrar(NomeCategoria nome) {
                throw new AssertionError("A listagem não deve cadastrar uma categoria.");
            }

            @Override
            public Categoria renomear(CodigoCategoria codigo, NomeCategoria nome) {
                throw new AssertionError("A listagem não deve renomear uma categoria.");
            }

            @Override
            public List<Categoria> listar() {
                return List.of();
            }

            @Override
            public boolean existe(CodigoCategoria codigo) {
                return false;
            }
        };
        Take api = new TkFork(new FkRegex("/api/categorias", new TkListaCategorias(vazias)));

        RsPrint resposta = consultar(api);

        assertResposta(200, "application/json", resposta);
        assertEquals(this.json.createArrayNode(), this.json.readTree(resposta.printBody()));
    }

    @Test
    @Tag("integracao")
    void listaPorCodigoMesmoQuandoOsRegistrosSaoInseridosEmOutraOrdem() throws Exception {
        PGSimpleDataSource banco = new PGSimpleDataSource();
        banco.setURL(System.getenv("RICO_BANCO_TESTE_URL"));
        banco.setUser(System.getenv().getOrDefault("RICO_BANCO_TESTE_USUARIO", "postgres"));
        banco.setPassword(System.getenv().getOrDefault("RICO_BANCO_TESTE_SENHA", ""));
        UUID sufixo = UUID.randomUUID();
        String primeiroNome = "Zebras " + sufixo;
        String segundoNome = "Antílopes " + sufixo;

        try (Connection conexao = banco.getConnection()) {
            try {
                long primeiroCodigo = reservarCodigo(conexao);
                long segundoCodigo = reservarCodigo(conexao);
                inserir(conexao, segundoCodigo, segundoNome);
                inserir(conexao, primeiroCodigo, primeiroNome);

                RsPrint resposta = consultar(AplicacaoRica.rotas(banco));

                assertResposta(200, "application/json", resposta);
                List<JsonNode> categoriasInseridas = new ArrayList<>();
                for (JsonNode categoria : this.json.readTree(resposta.printBody())) {
                    long codigo = categoria.path("codigo").asLong();
                    if (codigo == primeiroCodigo || codigo == segundoCodigo) {
                        categoriasInseridas.add(categoria);
                    }
                }
                assertEquals(List.of(
                    this.json.readTree("""
                        {"codigo":%d,"nome":"%s"}
                        """.formatted(primeiroCodigo, primeiroNome)),
                    this.json.readTree("""
                        {"codigo":%d,"nome":"%s"}
                        """.formatted(segundoCodigo, segundoNome))
                ), categoriasInseridas);
            } finally {
                try (var exclusao = conexao.prepareStatement(
                    "DELETE FROM public.categoria WHERE nome IN (?, ?)"
                )) {
                    exclusao.setString(1, primeiroNome);
                    exclusao.setString(2, segundoNome);
                    exclusao.executeUpdate();
                }
            }
        }
    }

    @Test
    void responde503QuandoOBancoEstaIndisponivel() throws Exception {
        HikariDataSource bancoFechado = new HikariDataSource();
        bancoFechado.close();

        RsPrint resposta = consultar(AplicacaoRica.rotas(bancoFechado));

        assertResposta(503, "application/problem+json", resposta);
        assertEquals(this.json.readTree("""
            {
                "title":"Persistência indisponível",
                "status":503,
                "detail":"Não foi possível concluir a operação no banco de dados.",
                "instance":"/api/categorias"
            }
            """), this.json.readTree(resposta.printBody()));
    }

    private static RsPrint consultar(Take api) throws Exception {
        return new RsPrint(api.act(new RqFake("GET", "/api/categorias")));
    }

    private static void assertResposta(int status, String tipo, RsPrint resposta) throws Exception {
        String primeiraLinha = resposta.head().iterator().next();
        assertEquals(Integer.toString(status), primeiraLinha.split(" ", 3)[1], resposta::toString);
        assertTrue(resposta.printHead().lines().anyMatch(
            ("Content-Type: " + tipo)::equalsIgnoreCase
        ));
    }

    private static long reservarCodigo(Connection conexao) throws SQLException {
        try (var consulta = conexao.prepareStatement(
            "SELECT nextval(pg_get_serial_sequence('public.categoria', 'codigo'))"
        ); var resultado = consulta.executeQuery()) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    private static void inserir(Connection conexao, long codigo, String nome) throws SQLException {
        try (var cadastro = conexao.prepareStatement("INSERT INTO public.categoria (codigo, nome) VALUES (?, ?)")) {
            cadastro.setLong(1, codigo);
            cadastro.setString(2, nome);
            cadastro.executeUpdate();
        }
    }
}
