package br.com.modelagemdominio.rico.cidades.pg;

import br.com.modelagemdominio.rico.cidades.Cidade;
import br.com.modelagemdominio.rico.cidades.Cidades;
import br.com.modelagemdominio.rico.cidades.CodigoCidade;
import br.com.modelagemdominio.rico.cidades.FalhaCidade;
import br.com.modelagemdominio.rico.cidades.FiltrosCidades;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class PgCidades implements Cidades {

  private static final Logger LOG = Logger.getLogger(PgCidades.class.getName());

  private final DataSource banco;

  public PgCidades(DataSource banco) {
    this.banco = banco;
  }

  @Override
  public Cidade cadastrar(NomeCidade nome, UnidadeFederativa uf) throws FalhaCidade {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement cadastro = conexao.prepareStatement("""
        INSERT INTO public.cidade (nome, uf) VALUES (?, ?)
        ON CONFLICT (LOWER(nome), uf) DO NOTHING
        RETURNING codigo, nome, uf
        """)) {
      cadastro.setString(1, nome.valor());
      cadastro.setString(2, uf.name());
      try (ResultSet resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaCidade(FalhaCidade.Motivo.NOME_DUPLICADO_NA_UF);
        }
        return new ResultadoSqlDeCidade(resultado).cidade();
      }
    } catch (SQLException falha) {
      LOG.severe("Falha ao cadastrar cidade (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCidade(FalhaCidade.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public List<Cidade> listar(FiltrosCidades filtros) throws FalhaCidade {
    var sql = new StringBuilder("SELECT codigo, nome, uf FROM public.cidade WHERE TRUE");
    List<String> parametros = new ArrayList<>(2);
    filtros.nome().ifPresent(nome -> {
      sql.append(" AND STRPOS(LOWER(nome), LOWER(?)) > 0");
      parametros.add(nome.valor());
    });
    filtros.uf().ifPresent(uf -> {
      sql.append(" AND uf = ?");
      parametros.add(uf.name());
    });
    sql.append(" ORDER BY codigo");
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(sql.toString())) {
      for (int indice = 0; indice < parametros.size(); indice++) {
        consulta.setString(indice + 1, parametros.get(indice));
      }
      return cidadesDaConsulta(consulta);
    } catch (SQLException falha) {
      LOG.severe("Falha ao listar cidades (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCidade(FalhaCidade.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public boolean existe(CodigoCidade codigo) throws FalhaCidade {
    try (Connection conexao = this.banco.getConnection()) {
      return this.existe(conexao, codigo);
    } catch (SQLException falha) {
      LOG.severe("Falha ao consultar a existência da cidade (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCidade(FalhaCidade.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  /**
   * Consulta a cidade na transação do chamador, que mantém a propriedade da conexão.
   */
  public boolean existe(Connection conexao, CodigoCidade codigo) throws SQLException {
    try (var consulta = conexao.prepareStatement("SELECT 1 FROM public.cidade WHERE codigo = ?")) {
      consulta.setLong(1, codigo.valor());
      try (var resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    }
  }

  private static List<Cidade> cidadesDaConsulta(PreparedStatement consulta) throws SQLException {
    try (ResultSet resultado = consulta.executeQuery()) {
      List<Cidade> cidades = new ArrayList<>();
      while (resultado.next()) {
        cidades.add(new ResultadoSqlDeCidade(resultado).cidade());
      }
      return List.copyOf(cidades);
    }
  }
}
