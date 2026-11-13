package br.com.modelagemdominio.rico.testes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** Confere a execução completa, inclusive os casos PostgreSQL das classes mistas. */
public final class VerificarIntegracao {

  private VerificarIntegracao() {
  }

  /**
   * @param argumentos diretório dos relatórios, seguido de suítes ou casos no formato classe#método
   * @throws IOException quando os relatórios não puderem ser lidos
   * @throws ParserConfigurationException quando o leitor de XML não puder ser preparado
   * @throws SAXException quando um relatório contiver XML inválido
   */
  public static void main(String[] argumentos)
    throws IOException, ParserConfigurationException, SAXException {
    if (argumentos.length < 2) {
      throw new IllegalArgumentException("Informe o diretório e as suítes e casos obrigatórios da integração.");
    }
    Path diretorio = Path.of(argumentos[0]);
    DocumentBuilderFactory fabrica = DocumentBuilderFactory.newDefaultInstance();
    fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    var leitor = fabrica.newDocumentBuilder();
    Map<String, Set<String>> executados = new HashMap<>();
    int total = 0;
    try (var arquivos = Files.list(diretorio)) {
      for (Path arquivo : arquivos.filter(caminho -> caminho.getFileName().toString().startsWith("TEST-")
        && caminho.getFileName().toString().endsWith("-integracao.xml")).sorted().toList()) {
        Element suite = leitor.parse(arquivo.toFile()).getDocumentElement();
        int testes = Integer.parseInt(suite.getAttribute("tests"));
        int falhas = Integer.parseInt(suite.getAttribute("failures"));
        int erros = Integer.parseInt(suite.getAttribute("errors"));
        int ignorados = Integer.parseInt(suite.getAttribute("skipped"));
        if (!"testsuite".equals(suite.getTagName()) || testes <= 0 || falhas != 0 || erros != 0 || ignorados != 0) {
          throw new IllegalStateException("Integração incompleta em " + arquivo.getFileName()
            + ": testes=" + testes + ", falhas=" + falhas + ", erros=" + erros + ", ignorados=" + ignorados);
        }
        Set<String> metodos = new HashSet<>();
        var casos = suite.getElementsByTagName("testcase");
        for (int indice = 0; indice < casos.getLength(); indice++) {
          String nome = ((Element) casos.item(indice)).getAttribute("name");
          metodos.add(nome.split("[\\(\\[]", 2)[0]);
        }
        executados.put(arquivo.getFileName().toString(), metodos);
        total += testes;
      }
    }
    for (int indice = 1; indice < argumentos.length; indice++) {
      String[] obrigatorio = argumentos[indice].split("#", 2);
      Set<String> metodos = executados.get("TEST-" + obrigatorio[0] + "-integracao.xml");
      if (metodos == null || (obrigatorio.length == 2 && !metodos.contains(obrigatorio[1]))) {
        throw new IllegalStateException("Suíte ou caso obrigatório não executado: " + argumentos[indice]);
      }
    }
    System.out.println("Integração verificada: " + total + " testes, sem falhas, erros ou casos ignorados.");
  }
}
