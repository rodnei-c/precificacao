package br.com.modelagemdominio.rico;

import br.com.modelagemdominio.rico.categorias.CategoriaConstante;
import br.com.modelagemdominio.rico.categorias.NomeCategoria;
import br.com.modelagemdominio.rico.cidades.CidadeConstante;
import br.com.modelagemdominio.rico.cidades.NomeCidade;
import br.com.modelagemdominio.rico.cidades.UnidadeFederativa;
import br.com.modelagemdominio.rico.clientes.ClienteConstante;
import br.com.modelagemdominio.rico.clientes.ContatoCliente;
import br.com.modelagemdominio.rico.clientes.Cpf;
import br.com.modelagemdominio.rico.clientes.Email;
import br.com.modelagemdominio.rico.clientes.IdentificacaoCliente;
import br.com.modelagemdominio.rico.clientes.NomeCliente;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntidadesMaterializadasTest {

  @Test
  void exigeIdentidadeEComposicaoDoClienteComAusenciaExplicitaDeEndereco() {
    var identificacao = new IdentificacaoCliente(new NomeCliente("Cliente de teste"), new Cpf("12345678909"));
    var contato = new ContatoCliente(new Email("cliente@example.com"), Optional.empty());

    assertAll(
      () -> assertThrows(IllegalArgumentException.class,
        () -> new ClienteConstante(0, identificacao, contato, Optional.empty())),
      () -> assertThrows(NullPointerException.class,
        () -> new ClienteConstante(1, null, contato, Optional.empty())),
      () -> assertThrows(NullPointerException.class,
        () -> new ClienteConstante(1, identificacao, null, Optional.empty())),
      () -> assertThrows(NullPointerException.class,
        () -> new ClienteConstante(1, identificacao, contato, null))
    );
    assertTrue(new ClienteConstante(1, identificacao, contato, Optional.empty()).endereco().isEmpty());
  }

  @Test
  void protegeOsDadosDaCategoriaMaterializada() {
    var nome = new NomeCategoria("Alimentos");

    assertThrows(IllegalArgumentException.class, () -> new CategoriaConstante(0, nome));
    assertThrows(NullPointerException.class, () -> new CategoriaConstante(1, null));
    assertEquals(1L, new CategoriaConstante(1, nome).codigo());
  }

  @Test
  void protegeOsDadosDaCidadeMaterializada() {
    var nome = new NomeCidade("Curitiba");
    var uf = UnidadeFederativa.PR;

    assertThrows(IllegalArgumentException.class, () -> new CidadeConstante(-1, nome, uf));
    assertThrows(NullPointerException.class, () -> new CidadeConstante(1, null, uf));
    assertThrows(NullPointerException.class, () -> new CidadeConstante(1, nome, null));
    assertEquals(1L, new CidadeConstante(1, nome, uf).codigo());
  }
}
