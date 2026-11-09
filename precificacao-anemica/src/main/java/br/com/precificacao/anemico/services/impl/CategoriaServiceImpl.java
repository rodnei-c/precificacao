package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.CadastroCategoriaRequisicao;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Categoria;
import br.com.precificacao.anemico.repositories.CategoriaRepository;
import br.com.precificacao.anemico.services.CategoriaService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoriaServiceImpl implements CategoriaService {

  private final CategoriaRepository categoriaRepository;

  public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
    this.categoriaRepository = categoriaRepository;
  }

  @Override
  public Categoria cadastrar(CadastroCategoriaRequisicao requisicao) {
    if (requisicao == null || requisicao.nome() == null || requisicao.nome().isBlank()) {
      throw new ValidacaoException("Informe o nome da categoria.");
    }
    String nome = requisicao.nome().strip();
    if (nome.length() > 200) {
      throw new ValidacaoException("O nome da categoria deve ter no máximo 200 caracteres.");
    }
    return categoriaRepository.inserir(nome)
      .orElseThrow(() -> new ValidacaoException("Já existe uma categoria com esse nome."));
  }

  @Override
  public List<Categoria> listar() {
    return categoriaRepository.listar();
  }

  @Override
  public long validarCategoria(BigDecimal valor) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValidacaoException("Informe um código de categoria inteiro positivo válido.");
    }
    long codigo;
    try {
      codigo = valor.longValueExact();
    } catch (ArithmeticException excecao) {
      throw new ValidacaoException("Informe um código de categoria inteiro positivo válido.");
    }
    if (!categoriaRepository.existePorCodigo(codigo)) {
      throw new RegistroNaoEncontradoException("Categoria não encontrada.");
    }
    return codigo;
  }
}
