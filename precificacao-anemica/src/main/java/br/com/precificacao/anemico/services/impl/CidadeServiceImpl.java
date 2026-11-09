package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.CadastroCidadeRequisicao;
import br.com.precificacao.anemico.enums.UnidadeFederativa;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Cidade;
import br.com.precificacao.anemico.repositories.CidadeRepository;
import br.com.precificacao.anemico.services.CidadeService;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CidadeServiceImpl implements CidadeService {

  private final CidadeRepository cidadeRepository;

  public CidadeServiceImpl(CidadeRepository cidadeRepository) {
    this.cidadeRepository = cidadeRepository;
  }

  @Override
  public Cidade cadastrar(CadastroCidadeRequisicao requisicao) {
    if (requisicao == null || requisicao.nome() == null || requisicao.nome().isBlank()) {
      throw new ValidacaoException("Informe o nome da cidade.");
    }
    String nome = requisicao.nome().strip();
    if (nome.length() > 200) {
      throw new ValidacaoException("O nome da cidade deve ter no máximo 200 caracteres.");
    }

    UnidadeFederativa uf = validarUf(requisicao.uf());
    return cidadeRepository.inserir(nome, uf)
      .orElseThrow(() -> new ValidacaoException("Já existe uma cidade com esse nome na UF informada."));
  }

  @Override
  public List<Cidade> listar() {
    return cidadeRepository.listar();
  }

  private UnidadeFederativa validarUf(String uf) {
    if (uf == null || uf.isBlank()) {
      throw new ValidacaoException("Informe a UF da cidade.");
    }
    try {
      return UnidadeFederativa.valueOf(uf.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException excecao) {
      throw new ValidacaoException("Informe uma sigla de UF brasileira válida.");
    }
  }
}
