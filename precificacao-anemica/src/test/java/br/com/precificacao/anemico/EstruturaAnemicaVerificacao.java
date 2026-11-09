package br.com.precificacao.anemico;

import br.com.precificacao.anemico.controllers.FaturaController;
import br.com.precificacao.anemico.controllers.ProdutoController;
import br.com.precificacao.anemico.dtos.CadastroCategoriaRequisicao;
import br.com.precificacao.anemico.dtos.FaturaResumo;
import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.enums.TipoDesconto;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.AlteracaoPrecificacao;
import br.com.precificacao.anemico.models.Categoria;
import br.com.precificacao.anemico.models.Cliente;
import br.com.precificacao.anemico.models.Fatura;
import br.com.precificacao.anemico.models.ItemFatura;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import br.com.precificacao.anemico.repositories.FaturaRepository;
import br.com.precificacao.anemico.repositories.HistoricoPrecificacaoRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.FaturaService;
import br.com.precificacao.anemico.services.CategoriaService;
import br.com.precificacao.anemico.services.ProdutoService;
import br.com.precificacao.anemico.services.impl.FaturaServiceImpl;
import br.com.precificacao.anemico.services.impl.PrecificacaoServiceImpl;
import br.com.precificacao.anemico.services.impl.ProdutoServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EstruturaAnemicaVerificacao {

    private EstruturaAnemicaVerificacao() {
    }

    public static void main(String[] args) {
        verificarModelos();
        verificarDependencias();
        verificarExcecoes();
        System.out.println("Verificacao estrutural concluida: modelos, dependencias e excecoes.");
    }

    private static void verificarModelos() {
        Produto produto = new Produto(1L, "Produto", new BigDecimal("100.00"),
                new BigDecimal("120.00"), new BigDecimal("20.0000"),
                ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO);

        verificar(produto.getCodigo().equals(1L), "Codigo do produto");
        verificar(produto.getNome().equals("Produto"), "Nome do produto");
        verificar(produto.getCusto().equals(new BigDecimal("100.00")), "Custo do produto");
        verificar(produto.getPrecoVenda().equals(new BigDecimal("120.00")), "Venda do produto");
        verificar(produto.getPercentual().equals(new BigDecimal("20.0000")), "Percentual do produto");
        verificar(produto.getModalidadePrecificacao() == ModalidadePrecificacao.ACRESCIMO_SOBRE_CUSTO,
                "Modalidade do produto");

        produto.setNome("Produto atualizado");
        produto.setModalidadePrecificacao(ModalidadePrecificacao.MARGEM_SOBRE_VENDA);
        verificar(produto.getNome().equals("Produto atualizado"), "Alteracao do nome");
        verificar(produto.getModalidadePrecificacao() == ModalidadePrecificacao.MARGEM_SOBRE_VENDA,
                "Alteracao da modalidade");

        ItemFatura item = new ItemFatura(produto, new BigDecimal("1.2500"));
        verificar(item.getProduto() == produto, "Produto do item");
        verificar(item.getQuantidade().equals(new BigDecimal("1.2500")), "Quantidade fracionaria");
        item.setQuantidade(new BigDecimal("2.5000"));
        verificar(item.getQuantidade().equals(new BigDecimal("2.5000")), "Alteracao da quantidade");

        LocalDateTime criacao = LocalDateTime.of(2026, 1, 1, 12, 0);
        Fatura fatura = new Fatura(2L, criacao, new ArrayList<>(List.of(item)),
                TipoDesconto.VALOR_FIXO, new BigDecimal("10.00"), null, null);
        verificar(fatura.getId().equals(2L), "Identificacao da fatura");
        verificar(fatura.getDataCriacao().equals(criacao), "Data da fatura");
        verificar(fatura.getItens().get(0) == item, "Item da fatura");
        verificar(fatura.getTipoDesconto() == TipoDesconto.VALOR_FIXO, "Tipo do desconto");
        verificar(fatura.getValorDesconto().equals(new BigDecimal("10.00")), "Valor do desconto");
        fatura.setTipoDesconto(TipoDesconto.PERCENTUAL);
        verificar(fatura.getTipoDesconto() == TipoDesconto.PERCENTUAL, "Alteracao do desconto");
        verificar(new Fatura().getItens().isEmpty(), "Colecao inicial de itens");
    }

    private static void verificarDependencias() {
        ProdutoRepository produtos = new ProdutoRepository() {
            public Produto salvar(Produto produto) { return produto; }
            public Optional<Produto> buscarPorCodigo(Long codigo) { return Optional.empty(); }
            public Optional<Produto> buscarPorCodigoParaAtualizacao(Long codigo) { return Optional.empty(); }
            public List<Produto> listar(String nome, ModalidadePrecificacao modalidade, Long categoria) { return List.of(); }
        };
        FaturaRepository faturas = new FaturaRepository() {
            public Fatura salvar(Fatura fatura) { return fatura; }
            public Optional<Fatura> buscarPorId(Long id) { return Optional.empty(); }
            public List<FaturaResumo> listar(LocalDateTime inicioInclusivo, LocalDateTime fimExclusivo, Long codigoCliente) {
                return List.of();
            }
        };

        HistoricoPrecificacaoRepository historico = new HistoricoPrecificacaoRepository() {
            public void registrar(Long codigo, AlteracaoPrecificacao.Valores anteriores,
                                  AlteracaoPrecificacao.Valores novos) { }
            public List<AlteracaoPrecificacao> listarPorProduto(Long codigo) { return List.of(); }
        };
        CategoriaService categorias = new CategoriaService() {
            public Categoria cadastrar(CadastroCategoriaRequisicao requisicao) {
                throw new UnsupportedOperationException();
            }
            public List<Categoria> listar() { return List.of(); }
            public long validarCategoria(BigDecimal codigo) {
                throw new UnsupportedOperationException();
            }
        };
        ProdutoService produtoService = new ProdutoServiceImpl(
                produtos, new PrecificacaoServiceImpl(), historico, categorias);
        ClienteRepository clientes = new ClienteRepository() {
            public Optional<Cliente> inserir(Cliente cliente) { return Optional.empty(); }
            public List<Cliente> listar() { return List.of(); }
            public boolean existePorCodigo(Long codigo) { return false; }
        };
        FaturaService faturaService = new FaturaServiceImpl(faturas, produtos, clientes);
        verificar(new ProdutoController(produtoService) != null, "Montagem do controller de produtos");
        verificar(new FaturaController(faturaService) != null, "Montagem do controller de faturas");

        boolean dependenciaAusenteRejeitada = false;
        try {
            new ProdutoController(null);
        } catch (NullPointerException exception) {
            dependenciaAusenteRejeitada = true;
        }
        verificar(dependenciaAusenteRejeitada, "Dependencia obrigatoria do controller");
    }

    private static void verificarExcecoes() {
        verificar(new ValidacaoException("Invalido").getMessage().equals("Invalido"),
                "Mensagem da excecao de validacao");
        verificar(new RegistroNaoEncontradoException("Ausente").getMessage().equals("Ausente"),
                "Mensagem da excecao de registro ausente");
    }

    private static void verificar(boolean condicao, String descricao) {
        if (!condicao) {
            throw new AssertionError(descricao);
        }
    }
}
