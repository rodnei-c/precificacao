package br.com.precificacao.anemico.controllers;

import br.com.precificacao.anemico.dtos.CadastroFaturaRequisicao;
import br.com.precificacao.anemico.dtos.FaturaResposta;
import br.com.precificacao.anemico.dtos.FaturaResumo;
import br.com.precificacao.anemico.dtos.ValoresFaturaResposta;
import br.com.precificacao.anemico.services.FaturaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faturas")
public class FaturaController {

  private final FaturaService faturaService;

  public FaturaController(FaturaService faturaService) {
    this.faturaService = Objects.requireNonNull(faturaService, "faturaService");
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FaturaResposta cadastrar(@RequestBody CadastroFaturaRequisicao requisicao) {
    return faturaService.cadastrar(requisicao);
  }

  @PostMapping("/simulacao")
  public ValoresFaturaResposta simular(@RequestBody CadastroFaturaRequisicao requisicao) {
    return faturaService.simular(requisicao);
  }

  @GetMapping
  public List<FaturaResumo> listar(
    @RequestParam(name = "inicio", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
    @RequestParam(name = "fim", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
    @RequestParam(name = "codigoCliente", required = false) Long codigoCliente
  ) {
    return faturaService.listar(inicio, fim, codigoCliente);
  }

  @GetMapping("/{codigo}")
  public FaturaResposta buscarPorCodigo(@PathVariable("codigo") Long codigo) {
    return faturaService.buscarPorCodigo(codigo);
  }
}
