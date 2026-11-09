package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.models.AlteracaoPrecificacao;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.HistoricoPrecificacaoRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.CategoriaService;
import br.com.precificacao.anemico.services.ProdutoService;
import br.com.precificacao.anemico.services.impl.PrecificacaoServiceImpl;
import br.com.precificacao.anemico.services.impl.ProdutoServiceImpl;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProdutoControllerTest {

  private final Map<Long, Produto> registros = new LinkedHashMap<>();
  private MockMvc api;

  @BeforeEach
  void prepararApi() {
    ProdutoRepository repositorio = new ProdutoRepository() {
      @Override
      public Produto salvar(Produto produto) {
        produto.setCodigo((long) registros.size() + 1);
        registros.put(produto.getCodigo(), produto);
        return produto;
      }

      @Override
      public Optional<Produto> buscarPorCodigo(Long codigo) {
        return Optional.ofNullable(registros.get(codigo));
      }

      @Override
      public Optional<Produto> buscarPorCodigoParaAtualizacao(Long codigo) {
        return buscarPorCodigo(codigo);
      }

      @Override
      public List<Produto> listar(String nome, ModalidadePrecificacao modalidade, Long categoria) {
        return registros.values().stream()
          .filter(produto -> nome == null
            || produto.getNome().toLowerCase(Locale.ROOT).contains(nome.toLowerCase(Locale.ROOT)))
          .filter(produto -> modalidade == null || produto.getModalidadePrecificacao() == modalidade)
          .filter(produto -> categoria == null || categoria.equals(produto.getCodigoCategoria()))
          .toList();
      }
    };
    HistoricoPrecificacaoRepository historico = new HistoricoPrecificacaoRepository() {
      @Override
      public void registrar(Long codigo, AlteracaoPrecificacao.Valores anteriores,
                            AlteracaoPrecificacao.Valores novos) {
        throw new AssertionError("Este teste não espera registrar histórico.");
      }

      @Override
      public List<AlteracaoPrecificacao> listarPorProduto(Long codigo) {
        return List.of();
      }
    };
    ProdutoService servico = new ProdutoServiceImpl(repositorio, new PrecificacaoServiceImpl(), historico, mock(CategoriaService.class));
    api = MockMvcBuilders.standaloneSetup(new ProdutoController(servico))
      .setControllerAdvice(new TratadorExcecoes()).build();
  }

  @Test
  void cadastraEConsultaProdutoPeloEnderecoRetornado() throws Exception {
    api.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content("""
      {
        "nome": "Produto de teste",
        "custo": 100,
        "percentual": 20,
        "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
      }
      """))
      .andExpect(status().isCreated())
      .andExpect(header().string("Location", "/api/produtos/1"))
      .andExpect(jsonPath("$.codigo").value(1))
      .andExpect(jsonPath("$.precoVenda").value(120.0));

    api.perform(get("/api/produtos/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.nome").value("Produto de teste"))
      .andExpect(jsonPath("$.percentual").value(20.0));
  }

  @Test
  void persisteCustoCalculadoNoCadastro() throws Exception {
    api.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content("""
      {
        "nome": "Custo calculado",
        "precoVenda": 125,
        "percentual": 20,
        "modalidadePrecificacao": "MARGEM_SOBRE_VENDA"
      }
      """))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.custo").value(100.0));

    api.perform(get("/api/produtos/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.custo").value(100.0));
  }

  @Test
  void recusaTresValoresSemPersistirProduto() throws Exception {
    api.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content("""
      {
        "nome": "Inválido",
        "custo": 100,
        "precoVenda": 120,
        "percentual": 20,
        "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
      }
      """))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.title").value("Dados inválidos"));

    assertEquals(0, registros.size());
  }

  @Test
  void recusaNomeVazio() throws Exception {
    api.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content("""
      {
        "nome": "  ",
        "custo": 100,
        "percentual": 20,
        "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
      }
      """))
      .andExpect(status().isBadRequest());

    assertEquals(0, registros.size());
  }

  @Test
  void informaRegistroAusente() throws Exception {
    api.perform(get("/api/produtos/999"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.detail").value("Produto não encontrado."));
  }

  @Test
  void recusaCodigoInvalido() throws Exception {
    api.perform(get("/api/produtos/0")).andExpect(status().isBadRequest());
    api.perform(get("/api/produtos/abc")).andExpect(status().isBadRequest());
  }

  @Test
  void recusaJsonInvalidoSemExporEntrada() throws Exception {
    api.perform(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content("""
      {"nome": "Inválido", "modalidadePrecificacao": "MODALIDADE_INEXISTENTE"}
      """))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.title").value("Formato inválido"));
  }
}
