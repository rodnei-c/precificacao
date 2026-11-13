package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.enums.CampoCliente;
import br.com.precificacao.anemico.models.Cliente;
import br.com.precificacao.anemico.repositories.CidadeRepository;
import br.com.precificacao.anemico.repositories.ClienteRepository;
import br.com.precificacao.anemico.services.impl.ClienteServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Confere o tratamento HTTP de falhas de persistência durante a atualização de e-mail. */
class ClienteControllerTest {

  @Test
  void falhaDePersistenciaNaAtualizacaoMantemResposta503() throws Exception {
    ClienteRepository repositorio = mock(ClienteRepository.class);
    when(repositorio.buscarPorCodigoParaAtualizacao(1L)).thenReturn(Optional.of(
      new Cliente(1L, "Cliente de teste", "12345678909", "anterior@example.com", null, null)
    ));
    when(repositorio.atualizar(any(Cliente.class), eq(Set.of(CampoCliente.EMAIL))))
      .thenThrow(new DataAccessResourceFailureException("Falha interna simulada."));
    MockMvc api = MockMvcBuilders.standaloneSetup(new ClienteController(
      new ClienteServiceImpl(repositorio, mock(CidadeRepository.class))
    )).setControllerAdvice(new TratadorExcecoes()).build();

    api.perform(patch("/api/clientes/1").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"novo@example.com\"}"))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.title").value("Persistência indisponível"))
      .andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação no banco de dados."));
  }
}
