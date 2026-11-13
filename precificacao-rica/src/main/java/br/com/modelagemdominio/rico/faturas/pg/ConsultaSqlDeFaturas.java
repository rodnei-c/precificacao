package br.com.modelagemdominio.rico.faturas.pg;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.ItemFatura;
import br.com.modelagemdominio.rico.faturas.Quantidade;
import br.com.modelagemdominio.rico.produtos.pg.PgProdutosConstantes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

/** Materializa cabeçalho, itens e produtos na mesma consulta, inclusive dentro do cadastro. */
final class ConsultaSqlDeFaturas {

  private static final String CONSULTA = """
    SELECT f.id, f.data_criacao, f.tipo_desconto, f.valor_desconto, f.cliente_codigo, f.forma_pagamento,
      i.produto_codigo, i.quantidade, p.codigo, p.nome, p.custo, p.preco_venda, p.percentual,
      p.modalidade_precificacao, p.categoria_codigo
    FROM public.fatura f
    LEFT JOIN public.item_fatura i ON i.fatura_id = f.id
    LEFT JOIN public.produto p ON p.codigo = i.produto_codigo
    """;

  private final DataSource banco;

  ConsultaSqlDeFaturas(DataSource banco) {
    this.banco = banco;
  }

  Fatura fatura(Connection conexao, long codigo) throws SQLException, FalhaFatura {
    try (var consulta = conexao.prepareStatement(CONSULTA + " WHERE f.id = ? ORDER BY i.posicao")) {
      consulta.setLong(1, codigo);
      var produtos = new PgProdutosConstantes(this.banco);
      var itens = new ArrayList<ItemFatura>();
      try (var resultado = consulta.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaFatura(FalhaFatura.Motivo.FATURA_AUSENTE);
        }
        CabecalhoFatura cabecalho = new CabecalhoFatura(resultado);
        do {
          if (resultado.getObject("produto_codigo", Long.class) != null) {
            itens.add(new ItemFatura(produtos.daLinha(resultado), new Quantidade(resultado.getBigDecimal("quantidade"))));
          }
        } while (resultado.next());
        return cabecalho.fatura(itens);
      }
    }
  }
}
