package br.com.modelagemdominio.rico.faturas.http;

import br.com.modelagemdominio.rico.faturas.FalhaFatura;
import br.com.modelagemdominio.rico.faturas.Fatura;
import br.com.modelagemdominio.rico.faturas.FormaPagamento;
import br.com.modelagemdominio.rico.faturas.ResumoFatura;
import br.com.modelagemdominio.rico.faturas.ValoresFatura;
import br.com.modelagemdominio.rico.faturas.ValoresItemFatura;
import br.com.modelagemdominio.rico.faturas.VinculosFatura;
import br.com.modelagemdominio.rico.http.ProblemasHttp;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.takes.Response;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;
import tools.jackson.databind.json.JsonMapper;

final class RespostasFatura {

  private final JsonMapper json;
  private final ProblemasHttp problemas;

  RespostasFatura(JsonMapper json) {
    this.json = json;
    this.problemas = new ProblemasHttp(json);
  }

  Response cadastrada(Fatura fatura) throws FalhaFatura {
    return new RsWithStatus(this.fatura(fatura), 201);
  }

  Response fatura(Fatura fatura) throws FalhaFatura {
    ValoresFatura valores = fatura.valores();
    var vinculos = fatura.vinculos();
    var resposta = new FaturaResposta(fatura.codigo(), fatura.dataCriacao(),
      new ValoresResposta(valores), new VinculosResposta(vinculos));
    return this.json(resposta);
  }

  Response simulacao(ValoresFatura valores) {
    return this.json(new ValoresResposta(valores, valores.taxaPagamento()));
  }

  Response lista(List<ResumoFatura> faturas) {
    return this.json(faturas.stream().map(ResumoResposta::new).toList());
  }

  private Response json(Object dados) {
    return new RsWithType(new RsWithBody(this.json.writeValueAsBytes(dados)), "application/json");
  }

  Response dadosInvalidos(String detalhe, String caminho) {
    return this.problemas.resposta(400, "Dados inválidos", detalhe, caminho);
  }

  Response formatoInvalido(String caminho) {
    return this.problemas.resposta(400, "Formato inválido", "Confira o formato do JSON.", caminho);
  }

  Response codigoInvalido(String caminho) {
    return this.dadosInvalidos("Informe um código de fatura inteiro positivo válido.", caminho);
  }

  Response falha(FalhaFatura falha, String caminho) {
    return switch (falha.motivo()) {
      case CODIGO_INVALIDO -> this.codigoInvalido(caminho);
      case DADOS_INVALIDOS -> this.dadosInvalidos(falha.getMessage(), caminho);
      case FATURA_AUSENTE, PRODUTO_AUSENTE, CLIENTE_AUSENTE ->
        this.problemas.resposta(404, "Registro não encontrado", falha.getMessage(), caminho);
      case PERSISTENCIA_INDISPONIVEL -> this.problemas.resposta(503, "Persistência indisponível",
        "Não foi possível concluir a operação no banco de dados.", caminho);
    };
  }

  private record FaturaResposta(long codigo, LocalDateTime dataCriacao,
    @JsonUnwrapped ValoresResposta valores, @JsonUnwrapped VinculosResposta vinculos) {
  }

  private record ItemResposta(long codigoProduto, BigDecimal quantidade, BigDecimal precoVenda, BigDecimal subtotal) {
    ItemResposta(ValoresItemFatura item) {
      this(item.codigoProduto(), item.quantidade().valor(), item.precoVenda(), item.subtotal());
    }
  }

  private record ValoresResposta(List<ItemResposta> itens, BigDecimal subtotal, BigDecimal descontoAplicado,
    @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal taxaPagamento, BigDecimal total) {

    ValoresResposta(ValoresFatura valores) {
      this(valores, null);
    }

    ValoresResposta(ValoresFatura valores, BigDecimal taxaPagamento) {
      this(valores.itens().stream().map(ItemResposta::new).toList(),
        valores.subtotal(), valores.descontoAplicado(), taxaPagamento, valores.total());
    }
  }

  private record VinculosResposta(
    @JsonInclude(JsonInclude.Include.NON_NULL) Long codigoCliente,
    @JsonInclude(JsonInclude.Include.NON_NULL) FormaPagamento formaPagamento) {

    VinculosResposta(VinculosFatura vinculos) {
      this(vinculos.codigoCliente().orElse(null), vinculos.formaPagamento().orElse(null));
    }
  }

  private record ResumoResposta(long codigo, LocalDateTime dataCriacao, long quantidadeItens,
    @JsonUnwrapped VinculosResposta vinculos) {

    ResumoResposta(ResumoFatura resumo) {
      this(resumo.codigo(), resumo.dataCriacao(), resumo.quantidadeItens(), new VinculosResposta(resumo.vinculos()));
    }
  }
}
