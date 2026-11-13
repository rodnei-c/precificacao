package br.com.modelagemdominio.rico.categorias.pg;

import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.Categorias;
import br.com.modelagemdominio.rico.categorias.CodigoCategoria;
import br.com.modelagemdominio.rico.categorias.FalhaCategoria;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class PgCategorias implements Categorias {

  private static final Logger LOG = Logger.getLogger(PgCategorias.class.getName());

  private final DataSource banco;

  public PgCategorias(DataSource banco) {
    this.banco = banco;
  }

  @Override
  public Categoria cadastrar(NomeCategoria nome) throws FalhaCategoria {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement cadastro = conexao.prepareStatement("""
        INSERT INTO public.categoria (nome) VALUES (?)
        ON CONFLICT (LOWER(nome)) DO NOTHING
        RETURNING codigo, nome
        """)) {
      cadastro.setString(1, nome.valor());
      try (ResultSet resultado = cadastro.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaCategoria(FalhaCategoria.Motivo.NOME_DUPLICADO);
        }
        return new CategoriaDaLinha(resultado).categoria();
      }
    } catch (SQLException falha) {
      LOG.severe("Falha ao cadastrar categoria (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public Categoria renomear(CodigoCategoria codigo, NomeCategoria nome) throws FalhaCategoria {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement atualizacao = conexao.prepareStatement("""
        UPDATE public.categoria SET nome = ? WHERE codigo = ?
        RETURNING codigo, nome
        """)) {
      atualizacao.setString(1, nome.valor());
      atualizacao.setLong(2, codigo.valor());
      try (ResultSet resultado = atualizacao.executeQuery()) {
        if (!resultado.next()) {
          throw new FalhaCategoria(FalhaCategoria.Motivo.CATEGORIA_AUSENTE);
        }
        return new CategoriaDaLinha(resultado).categoria();
      }
    } catch (SQLException falha) {
      throw new FalhaNaRenomeacaoCategoria(falha).traduzida();
    }
  }

  @Override
  public List<Categoria> listar() throws FalhaCategoria {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(
        "SELECT codigo, nome FROM public.categoria ORDER BY codigo"
      );
      ResultSet resultado = consulta.executeQuery()) {
      List<Categoria> categorias = new ArrayList<>();
      while (resultado.next()) {
        categorias.add(new CategoriaDaLinha(resultado).categoria());
      }
      return List.copyOf(categorias);
    } catch (SQLException falha) {
      LOG.severe("Falha ao listar categorias (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }

  @Override
  public boolean existe(CodigoCategoria codigo) throws FalhaCategoria {
    try (Connection conexao = this.banco.getConnection();
      PreparedStatement consulta = conexao.prepareStatement(
        "SELECT 1 FROM public.categoria WHERE codigo = ?"
      )) {
      consulta.setLong(1, codigo.valor());
      try (ResultSet resultado = consulta.executeQuery()) {
        return resultado.next();
      }
    } catch (SQLException falha) {
      LOG.severe("Falha ao consultar a existência da categoria (SQLState: " + falha.getSQLState() + ").");
      throw new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }
  }
}
