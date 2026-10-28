package br.com.precificacao.anemico;

import br.com.precificacao.anemico.controllers.FaturaController;
import br.com.precificacao.anemico.controllers.ProdutoController;
import br.com.precificacao.anemico.enums.ModalidadePrecificacao;
import br.com.precificacao.anemico.enums.TipoDesconto;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Fatura;
import br.com.precificacao.anemico.models.ItemFatura;
import br.com.precificacao.anemico.models.Produto;
import br.com.precificacao.anemico.repositories.FaturaRepository;
import br.com.precificacao.anemico.repositories.ProdutoRepository;
import br.com.precificacao.anemico.services.FaturaService;
import br.com.precificacao.anemico.services.PrecificacaoService;
import br.com.precificacao.anemico.services.ProdutoService;

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
                TipoDesconto.VALOR_FIXO, new BigDecimal("10.00"));
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
            public List<Produto> listar() { return List.of(); }
        };
        FaturaRepository faturas = new FaturaRepository() {
            public Fatura salvar(Fatura fatura) { return fatura; }
            public Optional<Fatura> buscarPorId(Long id) { return Optional.empty(); }
            public List<Fatura> listar() { return List.of(); }
        };

        ProdutoService produtoService = new ProdutoService(produtos, new PrecificacaoService());
        FaturaService faturaService = new FaturaService(faturas, produtos);
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
