package br.com.modelagemdominio.rico.clientes.pg;

import br.com.modelagemdominio.rico.cidades.pg.ExistenciaDeCidade;
import br.com.modelagemdominio.rico.clientes.AlteracaoCliente;
import br.com.modelagemdominio.rico.clientes.Cliente;
import br.com.modelagemdominio.rico.clientes.FalhaCliente;
import br.com.modelagemdominio.rico.clientes.Telefone;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/** Valida e grava os campos informados na transação do chamador. */
final class AtualizacaoDoCliente {

  private final Connection conexao;

  AtualizacaoDoCliente(Connection conexao) {
    this.conexao = conexao;
  }

  Cliente atualizar(long codigo, AlteracaoCliente alteracao) throws SQLException, FalhaCliente {
    this.validarCidade(alteracao);
    var campos = new LinkedHashMap<String, String>();
    alteracao.email().ifPresent(email -> campos.put("email", email.valor()));
    alteracao.telefone().aplicar(campos, telefone -> {
      campos.put("telefone", telefone.map(Telefone::valor).orElse(null));
      return campos;
    });
    String atribuicoes = campos.keySet().stream()
      .map(campo -> campo + " = ?")
      .collect(Collectors.joining(", "));
    if (alteracao.endereco().informada()) {
      String endereco = "cep = ?, logradouro = ?, numero = ?, bairro = ?, complemento = ?, cidade_codigo = ?";
      atribuicoes = atribuicoes.isEmpty() ? endereco : atribuicoes + ", " + endereco;
    }
    try (var atualizacao = this.conexao.prepareStatement(
      "UPDATE public.cliente SET " + atribuicoes + " WHERE codigo = ? RETURNING " + ResultadoSqlDeCliente.COLUNAS
    )) {
      int indice = 1;
      for (String valor : campos.values()) {
        atualizacao.setString(indice++, valor);
      }
      if (alteracao.endereco().informada()) {
        new ParametrosSqlDoEndereco(alteracao.endereco().valor()).informar(atualizacao, indice);
        indice += 6;
      }
      atualizacao.setLong(indice, codigo);
      try (var resultado = atualizacao.executeQuery()) {
        return new ResultadoSqlDeCliente(resultado).encontrado();
      }
    }
  }

  private void validarCidade(AlteracaoCliente alteracao) throws SQLException, FalhaCliente {
    var endereco = alteracao.endereco().valor();
    if (endereco.isPresent()
      && !new ExistenciaDeCidade(this.conexao).existe(endereco.orElseThrow().codigoCidade())) {
      throw new FalhaCliente(FalhaCliente.Motivo.CIDADE_AUSENTE);
    }
  }
}
