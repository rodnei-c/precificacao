# Validação de equivalência dos modelos

Etapa 21 concluída em 10 de novembro de 2026, no checkout `C:\Java\Workspace\precificacao`.

Os fluxos comparados produziram os mesmos valores e campos de sucesso, com códigos e horários gerados de forma independente. As entradas inválidas comparadas tiveram os mesmos status HTTP. Há diferenças de redação em cinco cenários de erro, descritas abaixo. Esse resultado encerra a equiparação funcional prevista no [plano do rico](Documentacao-e-Plano-Modelo-Rico.md).

## Ambiente e isolamento

Foram usados Java 17.0.12, Maven 3.9.9, Node.js 12.22.12 e PostgreSQL 14 em um servidor Docker descartável. Os dois módulos mantiveram seus bancos e suas migrações independentes: V1 a V8 no anêmico e V1 a V10 no rico.

Os testes JUnit usaram `precificacao_anemica_teste` e `precificacao_rica_teste`. A comparação por HTTP usou outros dois bancos vazios, `precificacao_anemica_equivalencia` e `precificacao_rica_equivalencia`. As APIs criaram esses bancos, aplicaram as migrações e atenderam em portas locais temporárias. Os bancos de desenvolvimento não participaram.

As sequências do rico começaram em 1001; as do anêmico, em 1. As requisições seguintes usaram os códigos devolvidos por cada API. Cada rodada gerou nomes, e-mails e CPFs válidos novos. Isso verificou o fluxo com identidades diferentes, sem depender de códigos fixos ou de dados da rodada anterior.

## Testes executados

| Módulo | Modo | Casos | Suítes | Falhas / erros / ignorados |
| --- | --- | ---: | ---: | --- |
| Anêmico | Sem PostgreSQL | 84 | 5 | 0 / 0 / 0 |
| Anêmico | Completo, com PostgreSQL | 159 | 9 | 0 / 0 / 0 |
| Rico | Sem PostgreSQL | 256 | 22 | 0 / 0 / 0 |
| Rico | Completo, com PostgreSQL | 368 | 36 | 0 / 0 / 0 |

O modo completo inclui os casos sem banco. Portanto, são **527 casos na execução completa dos dois módulos**, sem somar novamente os unitários. As contagens descrevem a execução; não são percentuais de cobertura ou medidas de qualidade.

Os comandos foram executados na raiz, com `JAVA_HOME` apontando para o JDK 17 e as variáveis dos bancos de teste configuradas conforme os READMEs:

```powershell
mvn -o -s configuracao-maven.xml "-Dmaven.repo.local=C:\Java\Workspace\tmp\precificacao-maven-repository" -pl precificacao-anemica,precificacao-rica test
mvn -o -s configuracao-maven.xml "-Dmaven.repo.local=C:\Java\Workspace\tmp\precificacao-maven-repository" -pl precificacao-anemica,precificacao-rica -Pintegracao package
```

O modo offline funcionou porque as dependências já estavam no cache indicado. Em outra máquina, omitir `-o` e a propriedade do cache local, se necessário. As variáveis exigidas estão nas instruções de testes do [anêmico](../precificacao-anemica/README.md#testes) e do [rico](../precificacao-rica/README.md#testes).

O perfil completo gerou relatórios novos em cada módulo, em `target/surefire-integracao/20261110-180049663`. A conferência obrigatória das suítes passou. Para o modo unitário, foram considerados somente os XML das classes listadas no log desta execução; arquivos de execuções antigas não entraram na soma.

## Comparação por HTTP

A [coleção do rico](postman/precificacao-rica.postman_collection.json) tem 75 requisições e 108 verificações `pm.test` por execução. Os scripts foram executados contra os dois JARs reais pelo [executor local](../verificacao/README.md), sem dependências npm. Ele implementa somente as APIs `pm` usadas nesta coleção. Não foi utilizado o aplicativo Postman nem o Newman.

| Execução | Requisições | Verificações `pm.test` | Resultado |
| --- | ---: | ---: | --- |
| Anêmico, rodada 1 | 75 | 108 | Passou |
| Rico, rodada 1 | 75 | 108 | Passou |
| Anêmico, rodada 2 | 75 | 108 | Passou |
| Rico, rodada 2 | 75 | 108 | Passou |
| Total | 300 | 432 | Passou |

Foram comparados **150 pares de respostas**: 75 operações em duas rodadas. As verificações `pm.test` conferem resultados esperados e status; a comparação adicional confere os corpos de sucesso entre os módulos.

| Assunto | Verificação no fluxo integrado |
| --- | --- |
| Categorias | Cadastro, listagem, renomeação, conflito de nome e preservação do vínculo com produto e histórico. |
| Produtos | As três combinações de valores de precificação, duas modalidades, categoria, consulta, filtros, alteração, histórico e repetição sem novo histórico. |
| Filtros | Nome literal, acentos e símbolos como `+`, `%`, `_`, `&` e `=`, além dos filtros combinados de cidades. |
| Clientes | Cadastro com e sem opcionais, consulta e listagem, alterações isoladas e conjuntas de contatos e endereço, omissão, remoção e rejeições sem alterar o estado anterior. |
| Faturas | Cadastro, simulação, consulta, opcionais e listagem resumida por período e cliente. |
| Preços atuais | Fatura inicialmente com subtotal 100 e desconto fixo 30; redução do preço para 12,34 aplica 12,34 de desconto e total zero; aumento para 50 retoma o desconto original 30 e total 20. |
| Precisão | Preço 1,23 e quantidade 0,1234 resultam em subtotal 0,151782. O desconto de 12,3456% resulta em 0,018738398592 e total final 0,13, na simulação, no cadastro e no GET. |
| Vínculos | Fatura conserva o cliente depois de alterações de contatos e endereço; produto conserva categoria e histórico depois da renomeação. |

A suíte JUnit completa complementou o fluxo HTTP nos cenários que exigem controle do banco:

| Risco | Verificações executadas |
| --- | --- |
| Escrita parcial | Falhas reais após UPDATE/INSERT, falha no histórico e falha em item da fatura, com desfazimento da operação e preservação dos registros anteriores. |
| Concorrência | Alterações de contatos sem perda, precificação com histórico consistente e criação simultânea de banco. Os casos coordenam bloqueios observados no PostgreSQL. |
| Leitura coerente | Preços atuais em nova consulta, valores de uma resposta materializados e simulação somente de leitura. No rico, os testes também exigem uma consulta para o GET e para o resumo de faturas. |
| Limites e precisão | Quantidade positiva, casas decimais, limites de armazenamento, fatura vazia, produto repetido, descontos inválidos e arredondamento somente no total. |
| Inicialização | Criação habilitada/desabilitada, permissões, senha incorreta, reinício, concorrência e migrações antes da API ficar disponível. |

Esses casos reutilizam os testes relevantes das etapas anteriores. O único caso JUnit acrescentado na etapa 21 foi a regressão de codificação descrita a seguir.

## Correção encontrada na revisão

`TkListaProdutos` ainda dependia da codificação padrão da JVM para interpretar a consulta da URL. O novo `FiltroProdutosUtf8Test` iniciou outra JVM com `-Dfile.encoding=windows-1252` e enviou o filtro `Café + Água %_ & =` codificado em UTF-8, junto com modalidade e categoria.

Antes da correção, o teste falhou porque o nome chegava corrompido. Depois da leitura explícita com `URLDecoder` e `StandardCharsets.UTF_8`, o mesmo caso passou, tanto no modo sem banco quanto no completo. A coleção também consultou esse nome pelas APIs reais com sucesso. O teste foi incluído na lista de suítes obrigatórias do perfil de integração.

A correção ficou na adaptação HTTP. Não foram alteradas fórmulas, objetos de negócio, migrações ou a arquitetura dos módulos.

## Critério de comparação e diferenças observadas

Os corpos de sucesso foram comparados por estrutura, campos, valores e ordem das listas. Os códigos foram associados à mesma operação de criação em cada modelo; datas geradas foram conferidas no formato esperado e desconsideradas na igualdade. Os números JSON foram comparados como valores decimais exatos, sem conversão para ponto flutuante: `100.00` e `100` representam o mesmo valor.

Nos erros, foram conferidos status HTTP, `application/problem+json` e os campos preenchidos `title`, `status`, `detail` e `instance`. A redação não precisou ser idêntica. Após normalizar os códigos das rotas e ignorar a ordem das propriedades JSON, apareceram estas diferenças nas duas rodadas:

| Entrada rejeitada | Anêmico | Rico | Status nos dois |
| --- | --- | --- | ---: |
| PATCH de cliente vazio | Solicita ao menos um campo. | Enumera e-mail, telefone ou endereço. | 400 |
| Campo desconhecido no PATCH | Orienta conferir JSON e parâmetros da URL. | Orienta conferir JSON. | 400 |
| Código de cliente zero | Informa que o código deve ser maior que zero. | Solicita inteiro positivo válido. | 400 |
| Código de cliente não numérico | Título `Formato inválido` e orientação genérica de formato. | Título `Dados inválidos` e orientação sobre código positivo. | 400 |
| Desconto fixo inicial acima do subtotal | Mensagem reúne precisão e limite do desconto. | Mensagem específica sobre o subtotal inicial. | 400 |

Essas diferenças não alteraram o resultado de negócio observado. Não se afirma igualdade textual dos erros. O relatório do executor preserva ambas as mensagens para consulta.

## Arquivos e evidências

- [Coleção Postman do rico](postman/precificacao-rica.postman_collection.json).
- [Executor e instruções de reprodução](../verificacao/README.md).
- [Resumo verificável desta execução](evidencias/etapa21-2026-11-10.json), com contagens, identificadores de relatórios e hashes dos artefatos.
- Logs locais em `target/validacao-etapa21/unidade.log` e `integracao-package.log`; comparação detalhada em `equivalencia.json`, no mesmo diretório. Esses arquivos gerados não são versionados.
- Reprodução da falha antes da correção em `precificacao-rica/target/etapa21-antes-utf8.log`.

Os processos e o servidor PostgreSQL descartáveis foram encerrados após a validação. Os relatórios permanecem no checkout; os bancos temporários não são necessários para consultá-los.

## Limites deste fechamento

A validação cobre os cenários descritos e os testes executados. Ela não demonstra ausência de falhas para todas as entradas possíveis, nem mede desempenho, esforço de manutenção ou superioridade de uma arquitetura. A importação visual no aplicativo Postman não foi realizada.

A regra atual de desconto percentual foi preservada. A exigência de subtotal mínimo de R$ 200,00 e sua avaliação no TCC continuam como trabalho posterior, com autorização própria.

<a id="validacao-final-modelagem"></a>

## Validação final da revisão de modelagem

A etapa 34 encerrou a revisão do rico em 10 de novembro de 2026. A execução usou JDK 24.0.2, Maven 3.9.9, PostgreSQL 15.1 descartável e Node.js 20.17.0. A suíte integrada usou um banco exclusivo. A comparação HTTP usou outra instância descartável com um banco para cada API.

| Modo do módulo rico | Casos | Suítes | Falhas / erros / ignorados |
| --- | ---: | ---: | --- |
| Sem PostgreSQL | 369 | 38 | 0 / 0 / 0 |
| Completo, com PostgreSQL | 491 | 53 | 0 / 0 / 0 |

A suíte completa inclui os casos sem banco. As verificações de método HTTP e cabeçalho `Allow` ficaram concentradas em `RotasDaAplicacaoRicaTest`, incluindo o GET rejeitado na rota literal da simulação. Os testes de regras verificam limites, precisão, desconto e mensagens. Os testes com banco verificam gravação parcial, rollback, concorrência, consistência das leituras e inicialização.

O [executor local](../verificacao/README.md) executou os scripts da [coleção Postman](postman/precificacao-rica.postman_collection.json) por HTTP, com duas rodadas para cada módulo. Foram **300 requisições, 432 verificações da coleção e 150 pares de respostas comparados**. Os corpos de sucesso coincidiram segundo os critérios descritos acima. Os cinco cenários de erro já registrados repetiram as diferenças de redação nas duas rodadas, com os mesmos status. O executor usou conexões HTTP sem reaproveitamento no Node.js 20.

### Exemplos de código para o estudo

| Ponto de comparação | Onde observar no código |
| --- | --- |
| Estrutura do anêmico | Os pacotes `controllers`, `services`, `repositories` e `models` separam as responsabilidades. [Módulo anêmico](../precificacao-anemica/src/main/java/br/com/precificacao/anemico) |
| Estrutura do rico | Os pacotes agrupam conceitos como produtos e faturas. Os subpacotes `http` e `pg` adaptam requisições e persistência. [Montagem das rotas](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/RotasDaAplicacaoRica.java#L54) |
| Precificação anêmica | `precificar(Produto)` lê os dados, escolhe a fórmula e grava o resultado no objeto pelos setters após a validação. [PrecificacaoServiceImpl](../precificacao-anemica/src/main/java/br/com/precificacao/anemico/services/impl/PrecificacaoServiceImpl.java#L29) |
| Strategy no rico | `Precificacoes` recebe `RegraDePrecificacao`. A modalidade seleciona `AcrescimoSobreCusto` ou `MargemSobreVenda`. [Seleção da estratégia](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/produtos/precificacao/ValoresParaPrecificar.java#L28) |
| Decorator no rico | `ProdutoConstante` mantém nome, categoria e valores da leitura, com identidade delegada. `DescontoReconstituido` delega cálculo, validação e registro ao desconto composto. [ProdutoConstante](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/produtos/ProdutoConstante.java), [DescontoReconstituido](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/descontos/DescontoReconstituido.java) |
| Specification no rico | `Precificacao` compõe a restrição de venda inferior ao custo com a exigência de venda superior para percentual positivo. [Composição das regras](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/produtos/Precificacao.java#L21) |
| Estado materializado e I/O | `CalculoFatura` valida e copia os itens durante a construção. A leitura dos preços ocorre ao calcular. [CalculoFatura](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/CalculoFatura.java) |

O inventário das classes do JAR encontrou apenas `AplicacaoRica.main` como método estático próprio. A verificação excluiu membros gerados pelo compilador. Os tipos com mais de quatro atributos são `TkCadastroProduto.Cadastro` (6), `TkCadastroCliente.Cadastro` (5) e `EnderecoJson` (6). São representações planas de entrada para o Jackson. Seus métodos compõem os valores e as operações de domínio.

### Evidências e alcance

- [Resumo da etapa 34](evidencias/modelagem-etapa34-2026-11-10.json), com as contagens, as execuções e os hashes SHA-256.
- Relatórios locais em `precificacao-rica/target/surefire-integracao/20261111-012930803` e `precificacao-rica/target/comparacao-modelagem-etapa-34.json`.
- O alcance da validação se restringe às regras e aos fluxos executados. As diferenças de organização podem orientar a análise de manutenibilidade do TCC. Medidas de esforço de manutenção e de desempenho exigem avaliação própria.
