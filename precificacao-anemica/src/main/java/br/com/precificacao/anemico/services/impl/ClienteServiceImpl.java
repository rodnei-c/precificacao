package br.com.precificacao.anemico.services.impl;

import br.com.precificacao.anemico.dtos.AlteracaoClienteRequisicao;
import br.com.precificacao.anemico.dtos.CadastroClienteRequisicao;
import br.com.precificacao.anemico.dtos.CadastroClienteRequisicao.EnderecoRequisicao;
import br.com.precificacao.anemico.enums.CampoCliente;
import br.com.precificacao.anemico.exceptions.RegistroNaoEncontradoException;
import br.com.precificacao.anemico.exceptions.ValidacaoException;
import br.com.precificacao.anemico.models.Cliente;
import br.com.precificacao.anemico.models.Endereco;
import br.com.precificacao.anemico.repositories.CidadeRepository;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import br.com.precificacao.anemico.services.ClienteService;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl implements ClienteService {

  private static final Pattern FORMATO_CPF = Pattern.compile("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}");
  private static final Pattern CPF_REPETIDO = Pattern.compile("([0-9])\\1{10}");
  private static final Pattern FORMATO_EMAIL = Pattern.compile("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+");
  private static final Pattern FORMATO_CEP = Pattern.compile("[0-9]{5}-?[0-9]{3}");
  private static final Pattern FORMATO_TELEFONE = Pattern.compile(
    "(?:\\+55 *)?(?:\\([0-9]{2}\\)|[0-9]{2}) *9?[0-9]{4}[- ]?[0-9]{4}"
  );

  private final ClienteRepository clienteRepository;
  private final CidadeRepository cidadeRepository;

  public ClienteServiceImpl(ClienteRepository clienteRepository, CidadeRepository cidadeRepository) {
    this.clienteRepository = clienteRepository;
    this.cidadeRepository = cidadeRepository;
  }

  @Override
  public Cliente cadastrar(CadastroClienteRequisicao requisicao) {
    String nome = textoObrigatorio("nome", requisicao.nome(), 200);
    String cpf = validarCpf(requisicao.cpf());
    String email = validarEmail(requisicao.email());
    String telefone = validarTelefone(requisicao.telefone());
    Endereco endereco = prepararEndereco(requisicao.endereco());
    Cliente cliente = new Cliente(null, nome, cpf, email, telefone, endereco);
    return clienteRepository.inserir(cliente)
      .orElseThrow(() -> new ValidacaoException("Já existe um cliente com o CPF ou o e-mail informado."));
  }

  @Override
  public List<Cliente> listar() {
    return clienteRepository.listar();
  }

  /** Valida o código e diferencia um cliente ausente de uma entrada inválida. */
  @Override
  public Cliente buscarPorCodigo(Long codigo) {
    validarCodigo(codigo);
    return clienteRepository.buscarPorCodigo(codigo)
      .orElseThrow(() -> new RegistroNaoEncontradoException("Cliente não encontrado."));
  }

  /** Valida a alteração e mantém leitura e gravação na mesma transação. */
  @Override
  @Transactional
  public Cliente alterar(Long codigo, AlteracaoClienteRequisicao requisicao) {
    validarCodigo(codigo);
    Set<CampoCliente> camposInformados = validarAlteracao(requisicao);
    Cliente atual = clienteRepository.buscarPorCodigoParaAtualizacao(codigo)
      .orElseThrow(() -> new RegistroNaoEncontradoException("Cliente não encontrado."));
    String email = requisicao.emailInformado() ? validarEmail(requisicao.email()) : atual.email();
    String telefone = requisicao.telefoneInformado() ? validarTelefone(requisicao.telefone()) : atual.telefone();
    Endereco endereco = requisicao.enderecoInformado()
      ? prepararEndereco(requisicao.endereco()) : atual.endereco();
    return gravarAtualizacao(new Cliente(
      atual.codigo(), atual.nome(), atual.cpf(), email, telefone, endereco
    ), camposInformados);
  }

  private Set<CampoCliente> validarAlteracao(AlteracaoClienteRequisicao requisicao) {
    if (requisicao == null) {
      throw new ValidacaoException("Informe os dados para atualizar o cliente.");
    }
    Set<CampoCliente> camposInformados = camposInformados(requisicao);
    if (camposInformados.isEmpty()) {
      throw new ValidacaoException("Informe ao menos um campo para atualizar o cliente.");
    }
    return camposInformados;
  }

  private Set<CampoCliente> camposInformados(AlteracaoClienteRequisicao requisicao) {
    Set<CampoCliente> campos = EnumSet.noneOf(CampoCliente.class);
    if (requisicao.emailInformado()) {
      campos.add(CampoCliente.EMAIL);
    }
    if (requisicao.telefoneInformado()) {
      campos.add(CampoCliente.TELEFONE);
    }
    if (requisicao.enderecoInformado()) {
      campos.add(CampoCliente.ENDERECO);
    }
    return campos;
  }

  private Cliente gravarAtualizacao(Cliente cliente, Set<CampoCliente> camposInformados) {
    try {
      return clienteRepository.atualizar(cliente, camposInformados)
        .orElseThrow(() -> new RegistroNaoEncontradoException("Cliente não encontrado."));
    } catch (DuplicateKeyException excecao) {
      throw new ValidacaoException("Já existe um cliente com o e-mail informado.");
    }
  }

  private String validarEmail(String valor) {
    String email = textoObrigatorio("email", valor, 254);
    if (!FORMATO_EMAIL.matcher(email).matches()) {
      throw new ValidacaoException("Informe um e-mail válido.");
    }
    return email;
  }

  private String validarTelefone(String valor) {
    String telefone = textoOpcional("telefone", valor, 30);
    if (telefone != null && !FORMATO_TELEFONE.matcher(telefone).matches()) {
      throw new ValidacaoException("Informe um telefone fixo ou celular no formato brasileiro, com DDD.");
    }
    return telefone;
  }

  private void validarCodigo(Long codigo) {
    if (codigo == null || codigo <= 0) {
      throw new ValidacaoException("O código do cliente deve ser maior que zero.");
    }
  }

  private String validarCpf(String valor) {
    String cpf = textoObrigatorio("CPF", valor, 14);
    if (!FORMATO_CPF.matcher(cpf).matches()) {
      throw new ValidacaoException("Informe um CPF válido.");
    }
    String digitos = cpf.replace(".", "").replace("-", "");
    if (CPF_REPETIDO.matcher(digitos).matches()
      || calcularDigitoCpf(digitos, 9) != digitos.charAt(9) - '0'
      || calcularDigitoCpf(digitos, 10) != digitos.charAt(10) - '0') {
      throw new ValidacaoException("Informe um CPF válido.");
    }
    return digitos;
  }

  private int calcularDigitoCpf(String cpf, int quantidade) {
    int soma = 0;
    for (int indice = 0; indice < quantidade; indice++) {
      soma += (cpf.charAt(indice) - '0') * (quantidade + 1 - indice);
    }
    int resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  }

  private Endereco prepararEndereco(EnderecoRequisicao requisicao) {
    if (requisicao == null) {
      return null;
    }
    String cep = textoObrigatorio("CEP", requisicao.cep(), 9);
    if (!FORMATO_CEP.matcher(cep).matches()) {
      throw new ValidacaoException("Informe um CEP com oito dígitos.");
    }
    String logradouro = textoObrigatorio("logradouro", requisicao.logradouro(), 200);
    String numero = textoObrigatorio("número", requisicao.numero(), 20);
    String bairro = textoObrigatorio("bairro", requisicao.bairro(), 100);
    String complemento = textoOpcional("complemento", requisicao.complemento(), 100);
    long codigoCidade = validarCidade(requisicao.codigoCidade());
    return new Endereco(cep.replace("-", ""), logradouro, numero, bairro, complemento, codigoCidade);
  }

  private long validarCidade(BigDecimal valor) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValidacaoException("Informe um código de cidade inteiro positivo válido.");
    }
    long codigo;
    try {
      codigo = valor.longValueExact();
    } catch (ArithmeticException excecao) {
      throw new ValidacaoException("Informe um código de cidade inteiro positivo válido.");
    }
    if (!cidadeRepository.existePorCodigo(codigo)) {
      throw new RegistroNaoEncontradoException("Cidade não encontrada.");
    }
    return codigo;
  }

  private String textoObrigatorio(String campo, String valor, int limite) {
    String texto = textoOpcional(campo, valor, limite);
    if (texto == null) {
      throw new ValidacaoException("O campo " + campo + " é obrigatório.");
    }
    return texto;
  }

  private String textoOpcional(String campo, String valor, int limite) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    String texto = valor.strip();
    if (texto.indexOf('\0') >= 0 || texto.length() > limite) {
      throw new ValidacaoException("O campo " + campo + " deve conter texto válido de até " + limite + " caracteres.");
    }
    return texto;
  }
}
