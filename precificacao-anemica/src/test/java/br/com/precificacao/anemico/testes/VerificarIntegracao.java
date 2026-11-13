package br.com.precificacao.anemico.testes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** Confere os relatórios da execução completa solicitada pelo perfil Maven de integração. */
public final class VerificarIntegracao {

  private VerificarIntegracao() {
  }

  /**
   * @param argumentos diretório da execução seguido dos nomes completos das suítes obrigatórias
   * @throws IOException quando os relatórios não puderem ser lidos
   * @throws ParserConfigurationException quando o leitor de XML não puder ser preparado
   * @throws SAXException quando algum relatório contiver XML inválido
   */
  public static void main(String[] argumentos)
    throws IOException, ParserConfigurationException, SAXException {
    if (argumentos.length < 2) {
      throw new IllegalArgumentException("Informe o diretório dos relatórios e as suítes de integração obrigatórias.");
    }
    Path diretorio = Path.of(argumentos[0]);
    for (int indice = 1; indice < argumentos.length; indice++) {
      Path obrigatorio = diretorio.resolve("TEST-" + argumentos[indice] + "-integracao.xml");
      if (!Files.isRegularFile(obrigatorio)) {
        throw new IllegalStateException("A suíte obrigatória não foi executada: " + argumentos[indice]);
      }
    }

    DocumentBuilderFactory fabrica = DocumentBuilderFactory.newDefaultInstance();
    fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    var leitor = fabrica.newDocumentBuilder();
    List<Path> relatorios;
    try (var arquivos = Files.list(diretorio)) {
      relatorios = arquivos.filter(arquivo -> arquivo.getFileName().toString().startsWith("TEST-")
        && arquivo.getFileName().toString().endsWith(".xml")).sorted().toList();
    }

    int total = 0;
    for (Path relatorio : relatorios) {
      Element suite = leitor.parse(relatorio.toFile()).getDocumentElement();
      if (!"testsuite".equals(suite.getTagName())) {
        throw new IllegalStateException("Relatório de testes inválido: " + relatorio.getFileName());
      }
      int testes = Integer.parseInt(suite.getAttribute("tests"));
      int falhas = Integer.parseInt(suite.getAttribute("failures"));
      int erros = Integer.parseInt(suite.getAttribute("errors"));
      int ignorados = Integer.parseInt(suite.getAttribute("skipped"));
      if (testes <= 0 || falhas != 0 || erros != 0 || ignorados != 0) {
        throw new IllegalStateException("Integração incompleta em " + suite.getAttribute("name")
          + ": testes=" + testes + ", falhas=" + falhas + ", erros=" + erros + ", ignorados=" + ignorados);
      }
      total += testes;
    }
    System.out.println("Integração verificada: " + total + " testes, sem falhas, erros ou casos ignorados.");
  }
}
