package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.CadastroCategoriaRequisicao;
import br.com.precificacao.anemico.dtos.RenomeacaoCategoriaRequisicao;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Categoria;
import br.com.precificacao.anemico.repositories.CategoriaRepository;
import br.com.precificacao.anemico.services.CategoriaService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CategoriaServiceImpl implements CategoriaService {

  private final CategoriaRepository categoriaRepository;

  public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
    this.categoriaRepository = categoriaRepository;
  }

  @Override
  public Categoria cadastrar(CadastroCategoriaRequisicao requisicao) {
    String nome = validarNome(requisicao == null ? null : requisicao.nome());
    return categoriaRepository.inserir(nome)
      .orElseThrow(() -> new ValidacaoException("Já existe uma categoria com esse nome."));
  }

  @Override
  public List<Categoria> listar() {
    return categoriaRepository.listar();
  }

  /** Reutiliza a validação do cadastro e traduz somente conflitos de unicidade. */
  @Override
  public Categoria renomear(Long codigo, RenomeacaoCategoriaRequisicao requisicao) {
    if (codigo == null || codigo <= 0) {
      throw new ValidacaoException("O código da categoria deve ser maior que zero.");
    }
    String nome = validarNome(requisicao == null ? null : requisicao.nome());
    try {
      return categoriaRepository.renomear(codigo, nome)
        .orElseThrow(() -> new RegistroNaoEncontradoException("Categoria não encontrada."));
    } catch (DuplicateKeyException excecao) {
      throw new ValidacaoException("Já existe uma categoria com esse nome.");
    }
  }

  private String validarNome(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new ValidacaoException("Informe o nome da categoria.");
    }
    String nome = valor.strip();
    if (nome.length() > 200) {
      throw new ValidacaoException("O nome da categoria deve ter no máximo 200 caracteres.");
    }
    if (nome.indexOf('\0') >= 0) {
      throw new ValidacaoException("Informe um nome de categoria válido.");
    }
    return nome;
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
