package br.com.modelagemdominio.rico.categorias.pg;

import br.com.modelagemdominio.rico.categorias.Categoria;
import br.com.modelagemdominio.rico.categorias.CategoriaConstante;
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
import javax.sql.DataSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

public final class PgCategorias implements Categorias {

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
                return categoriaDoResultado(resultado);
            }
        } catch (SQLException falha) {
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
                return categoriaDoResultado(resultado);
            }
        } catch (SQLException falha) {
            throw falhaAoRenomear(falha);
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
                categorias.add(categoriaDoResultado(resultado));
            }
            return List.copyOf(categorias);
        } catch (SQLException falha) {
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
            throw new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
        }
    }

    private static FalhaCategoria falhaAoRenomear(SQLException falha) {
        if (falha instanceof PSQLException postgres
            && PSQLState.UNIQUE_VIOLATION.getState().equals(postgres.getSQLState())
            && postgres.getServerErrorMessage() != null
            && "ux_categoria_nome".equals(postgres.getServerErrorMessage().getConstraint())) {
            return new FalhaCategoria(FalhaCategoria.Motivo.NOME_DUPLICADO, falha);
        }
        return new FalhaCategoria(FalhaCategoria.Motivo.PERSISTENCIA_INDISPONIVEL, falha);
    }

    private static Categoria categoriaDoResultado(ResultSet resultado) throws SQLException {
        return new CategoriaConstante(
            resultado.getLong("codigo"), new NomeCategoria(resultado.getString("nome"))
        );
    }
}
