# Precificação — estado atual e plano de implementação do modelo rico

Registro criado em 9 de novembro de 2026, com estado do rico conferido em 11 de novembro de 2026. Checkout atual em `C:\Java\workspace\modelagem-dominio`. Os módulos são `precificacao-anemica` e `precificacao-rica`.

Este documento reúne as funcionalidades entregues, as correções de modelagem já aplicadas e os próximos passos do rico. Foi atualizado em 11 de novembro de 2026, com o registro da revisão dos testes, da análise complementar de modelagem e das orientações sobre Postman. A etapa 34 registra o fechamento da rodada anterior de modelagem. Os registros anteriores foram mantidos para permitir a retomada sem repetir entregas.

**Situação atual: etapas 1 a 21 da equiparação funcional concluídas em 10 de novembro de 2026. A equivalência foi verificada nos fluxos documentados, com testes completos dos dois módulos e 150 pares de respostas HTTP comparados. A documentação e a coleção Postman do rico foram concluídas.**

**Situação da modelagem:** as 34 etapas da rodada anterior estão concluídas. A análise complementar identificou diferenças em relação à aplicação estrita de Elegant Objects. O autor decidiu conservar a validação nos construtores. Os demais achados e as decisões sobre records estão na seção 14. A proposta de desconto mínimo de R$ 200 permanece descartada.

### Como localizar o estado atual

| Frente | Situação | Onde consultar |
| --- | --- | --- |
| Equiparação funcional com o anêmico | 21 etapas concluídas: produtos, categorias, cidades, clientes, faturas, banco, testes e documentação. | Seções 1, 2 e histórico da seção 6. |
| Revisão de modelagem do rico | 34 etapas concluídas, resumidas abaixo e detalhadas com evidências. | Seção 12. |
| Evidências do encerramento | Testes, comparação HTTP e exemplos de código para o estudo. | [Validação final](Validacao-Equivalencia-Modelos.md#validacao-final-modelagem). |
| Revisões posteriores ao encerramento | Limpeza dos testes, decisões de modelagem e uso das coleções Postman. | Seção 14. |

A numeração recomeçou na revisão de modelagem. A etapa funcional 16 trata do cálculo de faturas e a etapa de modelagem 16 trata da leitura de cidades.

### O que já foi corrigido na modelagem

Todas as entregas desta tabela estão concluídas. As classes citadas já existem no checkout; as classes das próximas etapas serão definidas durante cada implementação autorizada.

| Etapa de modelagem | Correção aplicada | Objetos e efeito principal |
| --- | --- | --- |
| 1 | Valores de precificação passaram a proteger seus próprios limites e precisão. | `Dinheiro`, `Percentual` e `ValorInvalido`; comparação e conversão de percentual em fração no domínio. |
| 2 | Dependências de faturas passaram a ser compostas explicitamente. | `PgFaturas`, `ProdutosNaConexao` e `ClientesNaConexao`; operações compostas usam a conexão do chamador. |
| 3 | Validação estática de textos do cliente foi substituída por objetos. | `TextoCliente`, `TextoObrigatorio` e `TextoOpcional`, reutilizados pelos valores do cliente. |
| 4 | Fábricas estáticas de alteração de campo foram substituídas por operações polimórficas. | `AlteracaoCampo`, `CampoPreservado`, `CampoRemovido` e `CampoSubstituido`; omissão, remoção e substituição têm comportamentos próprios. |
| 5 | Telefone opcional passou a ser composto. | `TelefoneOpcional` substitui `Telefone.opcional`, preservando a diferença entre cadastro opcional e substituição no PATCH. |
| 6 | Conferência estática dos dígitos do CPF passou para objetos. | `DigitosVerificadoresCpf` e `DigitoVerificadorCpf`; `Cpf` conserva normalização e validação na construção. |
| 7 | Reconstituição estática do desconto passou a um decorator. | `DescontoReconstituido` substitui `Desconto.reconstituir` e delega à estratégia correspondente ao registro. |
| 8 | Cabeçalho da fatura passou a ser construído por composição. | `CabecalhoFatura` recebe a linha no construtor; removida `daLinha`, sem consulta adicional. |
| 9 | Leitura do vínculo opcional de categoria passou a um objeto. | `produtos.pg.ResultadoSqlDeCategoria`; ausência e código validado são usados nos dois caminhos de leitura do produto. |
| 10 | Conversão estática do código de categoria passou a comportamento. | `CodigoCategoria` compõe `InteiroExato` e mantém a exigência de código positivo. |
| 11 | Código de cidade passou a reutilizar a mesma conversão. | `CodigoCidade`; `InteiroExato` foi movido para `numeros` e é compartilhado, com mensagens próprias de cada código. |
| 12 | Interpretação estática da UF passou a um objeto. | `SiglaUf` substitui `UnidadeFederativa.daSigla` no cadastro e no filtro da listagem. |
| 13 | Materialização estática da cidade passou a composição. | `cidades.pg.ResultadoSqlDeCidade`, com três atributos finais, dois construtores e um método de comportamento. |
| 14 | Materialização estática da categoria passou a composição. | `categorias.pg.CategoriaDaLinha`, com dois atributos finais, dois construtores e um método de comportamento. |
| 15 | Classificação estática das falhas de renomeação passou a um objeto. | `FalhaNaRenomeacaoCategoria`; classificação específica, causa original e logs preservados. |
| 16 | Leitura da coleção de cidades passou a um objeto. | `CidadesDaConsulta` executa a consulta preparada e materializa uma lista imutável. |
| 17 | Invariantes passaram a ser protegidas na construção dos estados. | `Precificacao`, `ClienteConstante`, `CategoriaConstante` e `CidadeConstante` validam os valores compostos. |
| 18 | Regras de validade dos preços passaram a ser compostas com Specification. | `VendaNaoInferiorAoCusto` e `VendaSuperiorComPercentualPositivo` produzem a primeira violação. |
| 19 | O histórico passou a compor sua identidade e a leitura das alterações. | `AlteracaoPrecificacao` tem quatro componentes e `AlteracoesDaConsulta` materializa as linhas do histórico. |
| 20 | A leitura da coleção de produtos passou a um objeto. | `ProdutosDaConsulta` executa o comando preparado e usa o mapeamento fornecido pela coleção. |
| 21 | A alteração de preço passou a compor os comandos de gravação. | `AtualizacaoDaPrecificacao` e `RegistroDoHistoricoDePrecificacao` usam a conexão da transação. |
| 22 | A validação do código e a tradução de falhas de produto passaram a objetos. | `CodigoProduto` e `FalhaNaGravacaoProduto`. |
| 23 | A leitura do cliente passou a compor a materialização dos valores e do endereço. | `ResultadoSqlDeCliente` e `ResultadoSqlDeEndereco` usam a mesma linha JDBC. |
| 24 | A atualização parcial passou a compor o comando e as consultas de existência. | `AtualizacaoDoCliente`, `ParametrosSqlDoEndereco`, `ExistenciaDeCidade` e `ExistenciaDeCliente`. |
| 25 | A validação, a tradução de falhas e a reversão passaram a objetos. | `CodigoCliente`, `FalhaNaGravacaoCliente` e `ReversaoDaAlteracaoCliente`. |
| 26 | A gravação de fatura passou a compor os comandos do cabeçalho e dos itens. | Os dois comandos usam a conexão da transação coordenada por `PgFaturas`. |
| 27 | A tradução de falhas e o rollback de faturas passaram a objetos. | `FalhaNaLeituraDeFatura`, `FalhaNaGravacaoDeFatura` e `ReversaoDaOperacaoDeFatura` preservam a causa e distinguem referências ausentes de falhas de persistência. |
| 28 | A interpretação dos filtros HTTP passou a objetos. | `ParametrosDaConsulta` decodifica a URL em UTF-8. Cada listagem compõe os filtros do seu domínio. |
| 29 | A interpretação do nome de categoria e dos campos textuais do PATCH passou a objetos. | `NomeCategoriaJson` lê o nome para as duas rotas. `CampoDeTextoJson` compõe valores e alterações do cliente. |
| 30 | O pedido de fatura passou a compor a leitura JSON, os itens e a conversão exata de códigos. | `JsonDeFatura`, `PedidoFatura` e `InteiroExato` preservam a interpretação comum de cadastro e simulação. `DataDaConsultaDeFaturas` valida as datas da listagem. |
| 31 | As respostas HTTP passaram a compor grupos de dados do domínio. | Precificação, identificação, contato, localização, valores e vínculos são representados na borda HTTP. Os DTOs de entrada compõem os valores usados no cadastro. |
| 32 | A interpretação da configuração do PostgreSQL passou a objetos. | `ConfiguracaoPostgres`, `OpcaoCriacaoBanco`, `UrlPostgres` e `NomeBancoPostgres` compõem a preparação do banco. |
| 33 | A montagem da API passou a compor as rotas por recurso e por método. | `RotasDaAplicacaoRica` liga as coleções e os adaptadores. `RotaPorMetodo` conserva a resposta 405 e o cabeçalho `Allow`. |
| 34 | Revisão encerrada com inventário, testes e comparação HTTP. | A [validação final](Validacao-Equivalencia-Modelos.md#validacao-final-modelagem) reúne resultados, exemplos e limites. |

### Decisões de modelagem

A revisão concentrou o comportamento nos objetos que conhecem as regras e tornou explícita a composição das dependências. A tabela registra as decisões verificadas no fechamento.

| Tema | Estado conferido | Decisão |
| --- | --- | --- |
| Objetos com comportamento | Valores como dinheiro, percentual, CPF e quantidade já validam suas entradas; cálculos e descontos têm operações próprias. | Os tipos de domínio protegem as regras que conhecem, incluindo as invariantes da precificação. |
| Estruturas que apenas expõem dados | Existem records de leitura, como `ClienteConstante`, `CategoriaConstante` e `CidadeConstante`; `Precificacao` já possui `mesmosValores()`. | Os registros materializados conservam o estado observado e validam a composição exigida. |
| Imutabilidade | Campos finais, records e cópias de listas já são usados, inclusive nos cálculos de fatura. | Os testes distinguem os dados materializados da nova consulta e conferem as cópias defensivas. |
| Até quatro atributos | O inventário encontrou três tipos acima do limite, todos DTOs de entrada. | Os DTOs planos de produto, cliente e endereço preservam a desserialização do JSON. |
| Construtores e dependências | Os novos objetos usam construtores secundários que delegam ao primário. A composição de `PgFaturas` foi corrigida. | A montagem das rotas fornece os colaboradores usados na API. Os construtores secundários preservam formas úteis de construção. |
| Métodos estáticos próprios | O inventário do código de produção registra apenas o `main` exigido pela JVM. | O inventário do JAR desconsidera os métodos gerados pelo compilador. |
| Strategy | Existe em `RegraDePrecificacao`, `AcrescimoSobreCusto` e `MargemSobreVenda`, e nas alternativas de `Desconto`. | A escolha das fórmulas e do desconto ocorre na composição dos objetos de domínio. |
| Decorator | Existe em `ProdutoConstante` e `DescontoReconstituido`. | O produto conserva a leitura e delega a identidade. O desconto reconstituído delega à estratégia composta. |
| Specification | `Precificacao` compõe duas especificações que relacionam custo, venda e percentual. | A condição de ordem dos preços também é usada antes do cálculo do percentual. Etapa 18 concluída. |

**Última validação na etapa 34.** Passaram 369 testes em 38 suítes sem banco e 491 em 53 suítes no perfil completo, sem falhas, erros ou casos ignorados. O perfil completo inclui os unitários. Os relatórios estão registrados na seção 12.

Para retomar o trabalho, conferir este registro e o código do checkout atual.

### Histórico da retomada funcional — 10 de novembro de 2026

A conferência foi feita no código atual, incluindo as implementações do commit `31e8222` (`feito implementações das tabelas novas`), nas rotas de `AplicacaoRica`, nas migrações e nos testes. O plano anterior ainda descrevia uma versão anterior a essas entregas.

- **Já existe:** categorias e renomeação; categoria no produto; listagem e filtros de produtos; alteração e histórico de precificação; cidades com filtros por nome/UF; cadastro, listagem e consulta de clientes; alteração de e-mail, telefone e endereço, inclusive remoção dos opcionais e preservação dos campos omitidos.
- **Fechamento:** etapa 21 concluída; resultados e limites da comparação em [Validacao-Equivalencia-Modelos.md](Validacao-Equivalencia-Modelos.md).
- **Infraestrutura:** as etapas 1 e 2 foram concluídas após a conferência inicial. Há modos de teste separados, integração obrigatória quando solicitada e preparação do banco antes da abertura da API.
- **Faturas:** existem cálculos, valores materializados, vínculos opcionais, persistência Java, cadastro, consulta por código, simulação e listagem com filtros. O cadastro é atômico, a consulta acompanha os preços atuais, a simulação é somente de leitura e a listagem retorna resumos. Os fluxos integrados foram comparados com o anêmico na etapa 21.

**Verificação inicial, antes da conclusão da etapa 1.** Com Java 17 e Maven 3.9.9, o comando padrão sem variáveis de banco passou, mas deixou 22 casos ignorados: foram 148 casos reportados, dos quais 126 executados. Em seguida, foram aplicadas as dez migrações em um PostgreSQL 14 descartável e executada a suíte com as variáveis de integração: **150 casos, 17 suítes, zero falhas, erros ou ignorados**. A diferença de contagem decorre da descoberta dos casos parametrizados quando a suíte de PostgreSQL está habilitada. A validação posterior da etapa 1, registrada na seção 6, passou com 126 casos no modo unitário e 151 no completo, ambos sem ignorados. Os servidores temporários foram encerrados; o banco de desenvolvimento não foi usado.

O resultado da conferência inicial confirmou os cenários existentes e identificou verificações pendentes. As lacunas de falha na inserção do histórico e de alterações concorrentes da precificação foram cobertas na revisão da etapa 6, registrada abaixo. As demais etapas foram concluídas individualmente; seus registros preservam as contagens e decisões de cada entrega.

A revisão de modelagem foi concluída. A [validação final](Validacao-Equivalencia-Modelos.md#validacao-final-modelagem) registra a última execução.

## 1. O que já foi feito

### Modelo anêmico

A aplicação usa Spring Boot, JDBC, PostgreSQL e Flyway. Controllers recebem e respondem HTTP, services concentram as regras de negócio e repositories executam o SQL. Models representam os dados. Services e repositories possuem interfaces e implementações nos pacotes `impl`.

Estão implementados:

- Cadastro, consulta e listagem de produtos, com filtros combinados por nome, modalidade e categoria.
- Alteração da precificação com a modalidade cadastrada e registro dos valores anteriores e novos na mesma transação. Repetir os mesmos valores não acrescenta histórico.
- Cadastro, listagem e renomeação de categorias; categoria opcional no produto e troca desse vínculo.
- Cadastro de cidades e listagem com filtros por trecho do nome e UF.
- Cadastro, listagem e consulta de clientes por código; validação de CPF, e-mail, telefone e endereço com referência a cidade.
- Atualização parcial dos contatos e do endereço: campo omitido preserva o valor; telefone ou endereço nulo solicita remoção; e-mail nulo é rejeitado. A atualização grava somente as colunas solicitadas.
- Cadastro, consulta, simulação e listagem de faturas, com desconto, cliente e forma de pagamento opcionais. A consulta acompanha os preços atuais.
- Criação automática do banco configurado, quando habilitada, seguida das migrações Flyway.
- Documentação das rotas e coleção Postman com variáveis e preparação dos dados dos fluxos.

As migrações do anêmico vão de V1 a V8. As sete tabelas de negócio são `produto`, `categoria`, `historico_precificacao`, `cidade`, `cliente`, `fatura` e `item_fatura`. O Flyway mantém sua tabela de controle separadamente.

### Revisão dos testes do anêmico

Os ajustes recentes foram feitos com aprovação entre as etapas:

| Etapa concluída | Alteração |
| --- | --- |
| 1 | Remoção de `EstruturaAnemicaVerificacao`, um verificador manual de getters, setters e construção que não participava do Maven. |
| 2 | Remoção da verificação repetida de custo calculado em memória e conferência do cadastro, GET e valor efetivamente gravado no PostgreSQL. |
| 3 | Testes de precisão dos itens, descontos e total; arredondamento somente no final; conferência de simulação, cadastro e consulta. |
| 4 | Rejeição de quantidades e descontos inválidos, faturas vazias e produtos repetidos, com preservação dos registros existentes. Verificação dos limites válidos também. |
| 5 | Falha real provocada na inserção do histórico, confirmando o desfazimento de custo, preço e percentual e a preservação do histórico anterior. |
| 6 | Controle das operações simultâneas por bloqueios observados no PostgreSQL, tanto nos contatos quanto na criação concorrente do banco. |
| 7 | Verificação da causa específica de banco inexistente quando a criação está desativada. Senha incorreta e erro inesperado deixam de aprovar o cenário pelo motivo errado. |
| 8 | Separação do comando unitário e do perfil obrigatório de integração, com verificação de configuração, suítes presentes e ausência de testes ignorados. |

Na última execução registrada, passaram **84 casos unitários** no comando padrão e **159 casos na suíte completa** do anêmico, sem falhas, erros ou testes ignorados. A suíte completa inclui os 84 unitários. Não são 243 testes diferentes nem uma medida de cobertura.

Foram introduzidos erros somente em cópias temporárias para conferir o valor dos testes: arredondamentos antecipados, retirada da transação, perda de um contato e retirada do tratamento da criação concorrente. Os testes detectaram essas alterações. Também foram verificados o bloqueio de configuração ausente, de opções para pular testes, de caso ignorado e de suíte obrigatória ausente.

Os relatórios de execução ficam em `target`, fora do versionamento. O comando completo atual, após configurar as quatro variáveis descritas no README, é:

```bash
mvn -s configuracao-maven.xml -pl precificacao-anemica -Pintegracao test
```

### Modelo rico

O rico usa Takes para HTTP, JDBC e Hikari para conexão e um banco independente, `precificacao_rica`. O pacote principal é `br.com.modelagemdominio.rico`. A aplicação atende na porta `8081`; o anêmico usa `8080`.

Já existem:

- `Produto` e `Produtos`, como contratos da entidade e da coleção, incluindo categoria, listagem e alteração de precificação.
- `PgProduto`, `PgProdutos` e `PgProdutosConstantes`, com SQL parametrizado e adaptação dos resultados em objetos, além de `ConsultaSqlDeProdutos`, `AlteracaoSqlDePrecificacao` e `PgHistoricoDePrecificacao`.
- `ProdutoConstante`, um decorador que conserva os valores de uma leitura. Uma nova consulta à coleção carrega um novo estado.
- `NomeProduto`, `Dinheiro`, `Percentual`, `Precificacao` e o tipo `PrecificacaoParaCadastro` produzido pelo cálculo validado.
- `Precificacoes` e as estratégias `AcrescimoSobreCusto` e `MargemSobreVenda`.
- Cadastro e consulta de produtos, listagem com filtros por nome, modalidade e categoria, PATCH de categoria, PATCH de precificação e GET do histórico, com seus objetos `Tk` e respostas HTTP.
- `Categoria` / `Categorias`, nomes e códigos, implementação `PgCategorias` e rotas de cadastro, listagem e renomeação.
- `Cidade` / `Cidades`, nome, código e UF, implementação `PgCidades` e rotas de cadastro e listagem com os filtros combinados.
- `Cliente` / `Clientes`, valores de identificação, contatos e endereço, implementação `PgClientes` e rotas de cadastro, listagem, consulta e PATCH de e-mail, telefone e endereço. `AlteracaoCampo` distingue preservação, remoção e substituição dos opcionais.
- Os contratos `Fatura` e `Faturas`, `CalculoFatura`, `ValoresFatura`, `ValoresItemFatura`, `VinculosFatura` e `FormaPagamento`, além dos itens, quantidade e estratégias de desconto. `DescontoContratado` e `TipoDesconto` permitem gravar e reconstituir o desconto original sem confundir zero com ausência.
- `PgFatura`, `PgFaturas`, adaptação das linhas por `CabecalhoFatura`/`ConsultaSqlDeFaturas` e `FaturaConstante`. `TkCadastroFatura`, `TkConsultaFatura` e `TkSimulacaoFatura` expõem os fluxos existentes, com respostas compartilhadas em `RespostasFatura`. `PedidoFatura` compartilha o contrato JSON e sua conversão em objetos de domínio no cadastro e na simulação.
- `FiltrosFaturas`, `ResumoFatura`, `ConsultaSqlDeResumosFaturas` e `TkListaFaturas` completam a listagem resumida e filtrada, sem carregar produtos ou recalcular faturas.
- Testes de precificação, produtos, categorias, cidades, clientes e cálculo da fatura. O fake `DataSourceSemConexao` verifica cenários que precisam rejeitar a entrada antes de acessar o banco.

As migrações existentes no rico vão de V1 a V10 e criam as sete tabelas de negócio. A lista real está na seção 7. A etapa 17 reutilizou V5, V9 e V10; não houve nova migração.

A validação atual, após a etapa 20, aprovou **255 casos sem banco** e **367 na suíte completa**, sem falhas, erros ou ignorados. O perfil `integracao` exige `RICO_BANCO_TESTE_URL`, `RICO_BANCO_CRIACAO_TESTE_URL`, `RICO_BANCO_TESTE_USUARIO` e `RICO_BANCO_TESTE_SENHA`, aplica as migrações no banco de testes indicado e confere as suítes e casos obrigatórios em relatórios próprios da execução. As contagens anteriores, incluindo 241/351 na etapa 19, são registros históricos.

### Pontos de atenção após o fechamento

- As rotas da matriz foram entregues e os fluxos integrados foram comparados na etapa 21. O registro final distingue resultados equivalentes e diferenças de mensagens de erro; não afirma equivalência para toda entrada possível.
- `Dinheiro` e `Percentual` foram corrigidos na etapa 1 da revisão de modelagem, registrada na seção 12: agora validam os próprios limites e precisão e oferecem comparação e conversão de percentual em fração. A coerência entre custo, venda, percentual e modalidade continua sendo exigida pelo fluxo de precificação.
- A preparação automática do banco já foi entregue na etapa 2. O servidor PostgreSQL precisa estar ativo; criar um banco ausente exige `CREATEDB`, e as migrações exigem permissões no banco da aplicação.
- A proposta de subtotal mínimo de R$ 200 para desconto percentual foi descartada pelo usuário e permanece fora do escopo.
- `TkListaProdutos` passou a decodificar parâmetros em UTF-8 na etapa 21. O novo teste reproduziu a falha em Windows-1252 antes da correção e passou depois dela.

## 2. Contratos do anêmico alcançados pelo rico

Os contratos HTTP são a referência funcional. Cada implementação mantém seus próprios objetos e regras, sem depender do módulo vizinho.

| Recurso | Operação de referência | Estado no rico | Etapa funcional |
| --- | --- | --- | --- |
| Produto | `POST /api/produtos` | Implementado; categoria opcional revisada na etapa 4. | 4 |
| Produto | `GET /api/produtos/{codigo}` | Implementado; vínculo revisado na etapa 4. | 4 |
| Produto | `PATCH /api/produtos/{codigo}/categoria` | Implementado e revisado na etapa 4. | 4 |
| Produto | `GET /api/produtos?nome=&modalidadePrecificacao=&categoria=` | Implementado e revisado na etapa 5. | 5 |
| Produto | `PATCH /api/produtos/{codigo}/precificacao` | Implementado e revisado na etapa 6, com transação e concorrência verificadas. | 6 |
| Produto | `GET /api/produtos/{codigo}/historico-precificacao` | Implementado e revisado na etapa 6. | 6 |
| Categoria | `POST /api/categorias` e `GET /api/categorias` | Implementados e revisados na etapa 3. | 3 |
| Categoria | `PATCH /api/categorias/{codigo}` | Implementado e revisado na etapa 7. | 7 |
| Cidade | `POST /api/cidades` e `GET /api/cidades` | Cadastro e listagem sem filtros revisados na etapa 8. | 8 |
| Cidade | Filtro `nome` em `GET /api/cidades` | Implementado e revisado na etapa 9, com leitura UTF-8 da URL. | 9 |
| Cidade | Filtro `uf`, combinado com `nome` | Implementado e revisado na etapa 10. | 10 |
| Cliente | `POST /api/clientes` e `GET /api/clientes` | Implementados e revisados na etapa 11, com telefone e endereço opcionais. | 11 |
| Cliente | `GET /api/clientes/{codigo}` | Implementado e revisado na etapa 12. | 12 |
| Cliente | `PATCH /api/clientes/{codigo}` com e-mail | Implementado e revisado na etapa 13, preservando campos omitidos. | 13 |
| Cliente | Mesmo PATCH com telefone | Implementado e revisado na etapa 14, incluindo remoção por nulo e alterações concorrentes. | 14 |
| Cliente | Mesmo PATCH com endereço | Implementado na etapa 15: substituição completa, remoção por nulo e alteração conjunta atômica. | 15 |
| Fatura | Objetos de cálculo, valores e vínculos opcionais | Implementados na etapa 16; entidade PostgreSQL acrescentada na 17. | 16 |
| Fatura | `POST /api/faturas` | Implementado na etapa 17, com gravação atômica e opcionais. | 17 |
| Fatura | `GET /api/faturas/{codigo}` | Implementado na etapa 18, com preços atuais e uma consulta por resposta. | 18 |
| Fatura | `POST /api/faturas/simulacao` | Implementado na etapa 19, sem gravação ou geração de código. | 19 |
| Fatura | `GET /api/faturas?inicio=&fim=&codigoCliente=` | Implementado na etapa 20, com filtros combinados, ordenação e resumo. | 20 |

A [coleção Postman do rico](postman/precificacao-rica.postman_collection.json) foi entregue e verificada na etapa 21. A integração obrigatória e a preparação automática do banco das etapas 1 e 2 também estão concluídas.

### Entidades, valores e vínculos

| Conceito | Responsabilidade atual |
| --- | --- |
| `Categoria` / `Categorias` | Entidade identificada, coleção e operações de nome; referência opcional pelo produto. |
| Histórico de precificação | Registros identificados com data, produto e valores anteriores e novos; pertence ao assunto produtos. |
| `Cidade` / `Cidades` | Entidade identificada por código, com nome e UF; referência pelo endereço do cliente. |
| `Cliente` / `Clientes` | Identidade e operações sobre cadastro, contatos e endereço. |
| CPF, e-mail, telefone, CEP e endereço | Valores e comportamentos de validação e normalização. Endereço compõe o cliente. |
| `Fatura` / `Faturas` | Entidade identificada e coleção, composta por itens, desconto e vínculos opcionais. |
| `ItemFatura` / `Quantidade` | Relação com o produto e cálculo com sua quantidade, preservando a precisão. |
| Descontos | Comportamentos compostos pela fatura, mantendo o valor contratado e calculando o abatimento aplicável. |
| Forma de pagamento | Valor limitado às cinco modalidades aceitas. Não representa processamento de pagamento. |
| Valores da fatura | Resultado coerente de um cálculo, utilizável também na simulação, sem inventar código ou data de uma fatura persistida. |

Endereço continua persistido nas colunas de `cliente`; forma de pagamento é um valor da fatura. A criação de objetos Java não exige uma tabela por classe.

## 3. Regras comuns que devem ser preservadas

### Produtos e precificação

- Código gerado, nome obrigatório de até 200 caracteres e modalidade cadastrada.
- Informar exatamente dois valores não nulos entre custo, venda e percentual; calcular o terceiro. Rejeitar três valores, mesmo que sejam matematicamente compatíveis.
- Custo obrigatório no resultado e maior que zero; venda maior ou igual ao custo. Igualdade é aceita com percentual zero.
- Acréscimo sobre custo não negativo; margem sobre venda de zero até menos de 100%.
- Valores monetários com duas casas e percentuais com quatro, usando `BigDecimal` e o critério de arredondamento já adotado, `HALF_EVEN`. Preservar o cálculo antes do arredondamento do resultado.
- Rejeitar percentual positivo que produza venda igual ao custo após arredondar. O custo calculado deve ser gravado.
- Preservar os limites atuais de armazenamento: 17 dígitos inteiros para os valores monetários e 15 para o percentual.
- O PATCH de preço usa a modalidade já cadastrada e dois valores. Não alterar o contrato para receber uma terceira grandeza ou trocar a modalidade nessa operação.
- Atualização efetiva e histórico são uma única transação; repetição dos mesmos valores não gera outro registro. Comparar os valores numéricos sem tratar zeros finais como uma mudança.

### Cadastros e atualização parcial

- Categoria: código gerado, nome obrigatório até 200 caracteres e unicidade sem distinguir maiúsculas e minúsculas.
- Cidade: código gerado, nome obrigatório até 200 caracteres e UF brasileira em maiúsculas. Nome e UF, em conjunto, são únicos; a capitalização do nome não diferencia registros.
- Cliente: nome até 200 caracteres, CPF válido com dígitos verificadores e e-mail até 254 caracteres. CPF é gravado somente com dígitos; a unicidade de e-mail ignora capitalização.
- Telefone opcional, até 30 caracteres, no formato brasileiro com DDD e `+55` opcional, seguindo a regra atual. Texto em branco conserva o tratamento opcional do anêmico.
- Endereço opcional; quando presente, exige CEP válido de oito dígitos, logradouro, número textual, bairro e cidade existente. Complemento é opcional. Respeitar os limites atuais de cada campo.
- No PATCH, omissão preserva. E-mail nulo é inválido; telefone nulo remove; endereço nulo remove o conjunto inteiro. Um novo endereço substitui o anterior por completo.
- Rejeitar corpo vazio, nome e CPF enviados no PATCH, campos desconhecidos e atualização inválida. Quando vários campos vierem juntos, validar todos antes de gravar e preservar o estado anterior em caso de erro.
- O SQL de alteração inclui somente os campos solicitados. Tratar endereço como um conjunto de colunas, sem regravar contatos omitidos.

### Faturas

- Ao menos um item; cada produto aparece uma vez. Quantidade positiva, até quatro casas e até `999999999999999.9999`.
- Zeros decimais excedentes que não alteram o valor são aceitos, como nas regras atuais dos dois módulos.
- Cliente e forma de pagamento são opcionais. Cliente informado precisa existir. As formas são `PIX`, `DINHEIRO`, `CARTAO_DEBITO`, `CARTAO_CREDITO` e `BOLETO`.
- Aceitar um desconto percentual de 0 a 100%, com até quatro casas, ou desconto fixo não negativo com até duas casas. Na criação, o fixo não pode ultrapassar o subtotal.
- Preservar a precisão dos itens, da soma e do desconto. Arredondar somente o total final para duas casas. Total zero é permitido.
- Guardar produto, quantidade e desconto original. Em consultas posteriores, usar os preços atuais. Não gravar um preço unitário congelado como regra de faturamento.
- Se o subtotal cair, aplicar o menor valor entre desconto fixo original e subtotal atual. Quando subir, retomar o original até esse limite. Consultar não altera o desconto gravado.
- Cadastro de fatura e itens é atômico. Simulação não grava nem gera identidade de fatura.
- A listagem usa filtros combinados por data e cliente, com limite inicial inclusivo e final inclusivo para a data informada. O SQL pode representar o final pelo início exclusivo do dia seguinte.

### Contrato HTTP

Preservar nomes dos campos, formatos e respostas do anêmico. Cadastros retornam `201`, consultas e alterações bem-sucedidas retornam `200`, entradas inválidas retornam `400`, referências ou registros inexistentes retornam `404` e falhas de persistência retornam `503` com detalhe genérico. O cadastro de produto conserva o cabeçalho `Location` já existente.

Usar os mesmos critérios de busca e ordenação: produtos, cidades, clientes e categorias por código crescente; histórico do mais recente para o mais antigo; faturas por data e código decrescentes. Nome e UF combinam com `AND`. `%` e `_` informados como filtro de nome são texto literal.

## 4. Regras de implementação do rico

### Organização e comportamento

1. Manter os pacotes por assunto: `produtos`, `categorias`, `cidades`, `clientes` e `faturas`, com `pg` e `http` nos respectivos assuntos.
2. Usar contratos de entidade e coleção, seguindo `Produto` / `Produtos`. Os objetos do domínio expressam validações e operações; os `Pg...` executam a persistência e os `Tk...` adaptam HTTP.
3. Usar nomes em português e operações com significado de negócio. Preservar `codigo()`, `nome()`, `precificacao()` e as convenções `Pg` e `Tk`.
4. Usar objetos de valor imutáveis e composição. Os métodos do domínio devem expressar validações, cálculos e operações, sem getters/setters convencionais que permitam a um service central manipular livremente o estado. As consultas dos contratos existentes, como `codigo()` e `nome()`, permanecem compatíveis durante as alterações.
5. Os records de entrada e saída já usados na borda são aceitáveis. JSON, Takes e detalhes de serialização permanecem nessa borda; não transportar `JsonNode` para as regras de negócio.
6. Preservar Strategy e Decorator onde já estão aplicados. Introduzir Specification para condições de validade da precificação que serão compostas, conforme a etapa 18; manter cada padrão ligado a uma responsabilidade concreta.
7. Trabalhar com até quatro atributos de instância por objeto do domínio. Revisar os casos atuais acima do limite, começando por `AlteracaoPrecificacao`, e os DTOs maiores na etapa própria. Agrupar identificação, contatos ou endereço quando houver um conceito real, preservando o contrato externo.
8. Diferenciar um objeto de domínio sem proteção de suas regras de uma representação imutável de leitura ou de um DTO de fronteira. Um record não comprova riqueza de domínio nem é automaticamente um erro. A revisão deve demonstrar onde a regra e a validade do estado são garantidas.
9. Substituir auxiliares e fábricas estáticas próprios por comportamento de objetos. Evitar criar uma classe que apenas esconda o mesmo método e acrescente estado sem utilidade. Constantes, operações das bibliotecas e o ponto de entrada da JVM têm papéis distintos.

### Construção, consulta e composição

- Construtores recebem dependências e valores, sem abrir conexão, executar SQL ou iniciar transações. A validação e normalização de valores na construção, já presentes em `NomeProduto`, `Quantidade` e descontos, permanecem decisões admitidas pelo projeto.
- Com mais de um construtor, concentrar a inicialização no primário e delegar pelos secundários.
- Seguir a preferência por mais construtores úteis que métodos e por classes pequenas. Se um objeto acumular operações, rever sua responsabilidade e composição antes de acrescentar sobrecargas. Não criar construtores ou métodos artificiais para satisfazer uma contagem.
- Conservar atributos de estado finais e proteger coleções recebidas ou devolvidas quando houver risco de alteração externa. Alterações de negócio produzem novos valores ou uma nova leitura; não introduzir setters para facilitar a adaptação.
- Reaproveitar `PgProdutosConstantes`, `ProdutoConstante` e os adaptadores de resultado. Carregar os dados necessários em uma leitura coerente e evitar consultas repetidas para serializar cada atributo.
- Um objeto constante representa os valores de sua leitura. Uma nova operação consulta a coleção novamente. Após uma gravação, devolver o estado atualizado, sem devolver como atual um decorador que ainda guarda os valores antigos.
- Evoluir todos os implementadores de um contrato, inclusive decoradores, fábricas e fakes. Não deixar uma implementação aceitando a operação apenas para lançar uma exceção genérica de operação não suportada.

### Gravação, transações e erros

- SQL parametrizado, recursos fechados e restrições de banco para unicidade, chaves estrangeiras e limites. Identificadores dinâmicos de banco exigem tratamento próprio; parâmetros de valores não substituem a proteção desses identificadores.
- Operações compostas usam a mesma conexão e a mesma transação do início ao fim. Delegar a um objeto que abre outra conexão não participa automaticamente da transação anterior.
- A leitura que fundamenta uma alteração deve estar protegida contra atualização perdida. A comparação de preços e o histórico precisam considerar o estado efetivamente gravado.
- Manter o gerenciamento da conexão nos objetos de persistência ou de infraestrutura da operação, com regra de negócio delegada aos objetos responsáveis. Não introduzir Spring ou copiar o desenho de services do anêmico para coordenar o rico.
- Preservar a causa original das falhas, classificá-las de forma específica e traduzi-las na borda. Registrar informação útil para diagnóstico sem enviar SQL, credenciais ou detalhes internos no HTTP.
- Reutilizar as validações entre cadastro, alteração e simulação. Usar cláusulas de guarda e métodos focados; explicar regras longas por nomes do domínio.
- Código e documentação em português, UTF-8 conforme os arquivos do módulo e dois espaços nos arquivos criados ou alterados. Preservar mensagens existentes; evitar reformatação geral de arquivos alheios à etapa.
- O projeto não possui as bibliotecas internas do SGF. Usar JDK e dependências do próprio módulo; não importar `ValueUtil`, `Coalesce` ou outras utilitárias corporativas.

### Testes que sustentam a entrega

Testar regras com resultados esperados independentes, limites válidos e inválidos, precisão, persistência e erros reais. Reutilizar fakes quando houver comportamento útil para vários cenários; preservar `DataSourceSemConexao` para verificar ausência de I/O indevido na construção.

Cada etapa com SQL ou HTTP deve executar seus testes de integração em PostgreSQL separado. A etapa final reúne os fluxos; ela não substitui as verificações incrementais. Se a infraestrutura estiver indisponível, registrar a pendência e pedir orientação antes de considerar a etapa concluída ou avançar.

Evitar testes de getters, setters, construtores triviais, contagens arbitrárias de classes ou repetições de fórmulas sem um risco adicional. Testar criação sem conexão quando esse for o contrato arquitetural. Testar concorrência confirmando a sobreposição no banco, com espera limitada e limpeza, e testar transações provocando falha depois de uma gravação já iniciada.

## 5. Como executar o plano

**Uma etapa por vez, na ordem, com aprovação explícita entre elas. Dependências técnicas atendidas não autorizam antecipar outra etapa.**

Para cada etapa:

1. Confirmar que ela foi autorizada e ler seu escopo. Conferir o estado atual do checkout e as alterações já existentes.
2. Implementar somente a entrega descrita, incluindo a migração necessária e os testes relevantes.
3. Compilar, executar as verificações da etapa e revisar tanto o comportamento quanto o código.
4. Corrigir os problemas encontrados e repetir os cenários afetados. Atualizar os exemplos e a documentação do recurso.
5. Informar os arquivos alterados, os resultados reais e qualquer verificação pendente. Registrar o status da etapa neste plano.
6. **Parar e perguntar: “Etapa N concluída. Posso seguir para a etapa N+1?” Aguardar uma resposta afirmativa antes de continuar.**

“Pode seguir” autoriza a próxima etapa apresentada. Falta de resposta não é aprovação. Não executar o plano inteiro nem fazer commit/push sem instrução do usuário. A proposta de desconto mínimo de R$ 200 está arquivada na seção 9.

A retomada da modelagem segue a seção 13. Antes de implementar cada escopo, conferir novamente as classes e os testes, porque o checkout pode receber alterações feitas em outro computador. Se uma entrega precisar ser dividida, registrar as subetapas e manter o pedido de ok entre elas.

### Ordem da equiparação funcional — concluída

Status conferidos em 10 de novembro de 2026. **Implementada** indica que a funcionalidade existe e seus cenários atuais passaram; não atesta uma revisão exaustiva nem elimina critérios de aceite ainda sem teste. Os escopos detalhados abaixo são mantidos como referência. Não recriar uma entrega ou migração que já esteja presente.

| Etapa | Entrega | Status | Avaliação | Dependências principais |
| --- | --- | --- | --- | --- |
| 1 | Base de testes e integração obrigatória do rico | Concluída em 10/11/2026. | Média | Implementação existente |
| 2 | Preparação automática do banco e migrações na inicialização | Concluída em 10/11/2026. | Média | 1 |
| 3 | Cadastrar e listar categorias | Revisada e concluída em 10/11/2026. | Baixa a média | 1 e 2 |
| 4 | Categoria opcional no produto e alteração do vínculo | Revisada e concluída em 10/11/2026. | Média | 3 e produtos existentes |
| 5 | Listar e filtrar produtos | Revisada e concluída em 10/11/2026. | Média | 4 |
| 6 | Alterar precificação e consultar histórico | Revisada e concluída em 10/11/2026. | Alta, pela transação | 4 e precificação existente |
| 7 | Renomear categoria | Revisada e concluída em 10/11/2026. | Baixa a média | 3, 4 e 6 |
| 8 | Cadastrar e listar cidades | Revisada e concluída em 10/11/2026. | Baixa a média | 1 e 2 |
| 9 | Filtrar cidades por nome | Revisada e concluída em 10/11/2026. | Baixa | 8 |
| 10 | Filtrar cidades por UF e combinar filtros | Revisada e concluída em 10/11/2026. | Baixa | 9 |
| 11 | Valores, cadastro e listagem de clientes | Revisada e concluída em 10/11/2026. | Média | 8 e 10 |
| 12 | Consultar cliente pelo código | Revisada e concluída em 10/11/2026. | Baixa | 11 |
| 13 | Atualizar e-mail com presença explícita do campo | Revisada e concluída em 10/11/2026. | Média | 12 |
| 14 | Atualizar ou remover telefone | Revisada e concluída em 10/11/2026. | Baixa a média | 13 |
| 15 | Substituir ou remover endereço | Implementada e concluída em 10/11/2026. | Média | 14 e cidades |
| 16 | Completar os objetos de cálculo da fatura | Implementada e concluída em 10/11/2026. | Média | Base de itens, descontos e produtos |
| 17 | Persistir fatura e itens, com cliente e pagamento opcionais | Implementada e concluída em 10/11/2026. | Alta | 4, 11 e 16 |
| 18 | Consultar fatura com preços atuais e leitura coerente | Implementada e concluída em 10/11/2026. | Alta | 6 e 17 |
| 19 | Simular fatura sem gravar | Implementada e concluída em 10/11/2026. | Média | 16 a 18 |
| 20 | Listar faturas por período e cliente | Implementada e concluída em 10/11/2026. | Média | 17 e 18 |
| 21 | Validar equivalência e fechar README/Postman | Concluída em 10/11/2026; resultados reais registrados e coleção rica entregue. | Média | 1 a 20 |

## 6. Histórico da equiparação funcional — etapas 1 a 21 concluídas

### Etapa 1 — Preparar a base de testes do rico

**Entrega.** Uma referência executável do comportamento atual e um comando de integração que exija o banco de testes e as suítes correspondentes.

**Implementação.**

1. Executar e registrar os testes existentes. Conferir `POST` e `GET` de produtos com as duas modalidades, os valores calculados e as respostas de erro relevantes.
2. Separar testes unitários e de PostgreSQL, seguindo a finalidade do perfil `integracao` do anêmico. Preservar as variáveis existentes `RICO_BANCO_TESTE_URL`, `RICO_BANCO_TESTE_USUARIO` e `RICO_BANCO_TESTE_SENHA`.
3. Preparar as migrações no banco explicitamente indicado para testes. A preparação não pode recorrer por padrão ao banco de desenvolvimento.
4. Fazer o comando completo falhar por configuração ausente, teste ignorado ou suíte obrigatória não executada. Usar relatórios novos por execução e ampliar a lista de suítes obrigatórias conforme as próximas entregas.
5. Registrar os contratos de referência e aproveitar os fakes úteis, sem compartilhar implementação de negócio com o anêmico.

**Aceite.** Testes sem banco passam no modo unitário. O modo completo executa os casos atuais com PostgreSQL e não aprova a ausência de configuração ou uma suíte desabilitada. A contagem pode crescer nas próximas etapas; 151 é o registro completo ao concluir esta etapa, não uma meta fixa.

**Revisão e parada.** Conferir isolamento, migrações, relatórios e valor das verificações. Registrar os comandos entregues e pedir ok para a etapa 2.

**Encerramento — 10 de novembro de 2026. Status: concluída. A etapa 2 foi autorizada posteriormente e também concluída.**

- **Arquivos alterados:** `precificacao-rica/pom.xml`; nove classes de teste com dependência de PostgreSQL; `src/test/ant/integracao.xml`; ferramentas `PrepararBancoIntegracao` e `VerificarIntegracao`; README do módulo, README geral e este plano. As regras de negócio, a inicialização da API e as migrações existentes foram preservadas.
- **Separação:** `@Tag("integracao")` substituiu as condições que ignoravam testes na ausência de banco. No modo padrão, os casos de banco são excluídos da seleção. No perfil completo, todas as suítes são executadas e casos ignorados reprovam o comando.
- **Preparação:** antes dos testes completos, o Flyway de escopo de teste aplica as migrações exclusivamente na URL definida em `RICO_BANCO_TESTE_URL`, com o usuário e a senha próprios de teste. O banco precisa existir. Isso não implementa a inicialização automática da API prevista na etapa 2.
- **Contratos de referência:** cadastro e GET pelo `Location` passaram a ser verificados tanto para acréscimo sobre custo quanto para margem sobre venda. A suíte completa passou de 150 para 151 casos por essa segunda combinação, sem duplicar regras do anêmico.
- **Comandos entregues:** `mvn -s configuracao-maven.xml -pl precificacao-rica test` e `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, após definir as três variáveis exigidas nessa entrega. A etapa 2 acrescentou a quarta variável de testes de criação, conforme o README atual.
- **Resultados reais:** 126 casos em 15 suítes no modo padrão, sem falhas, erros ou ignorados; 151 casos em 17 suítes no completo, também sem falhas, erros ou ignorados. Dez migrações aplicadas a um PostgreSQL 14 descartável inicialmente vazio. O modo unitário também passou com variáveis de banco apontando para uma porta indisponível. No modo completo, valores inválidos em `RICO_DB_URL` e `FLYWAY_URL` não desviaram a preparação do banco de testes selecionado.
- **Verificações negativas:** 14 configurações ausentes, inválidas ou opções de seleção/omissão foram rejeitadas antes de acessar o banco. Em três cópias temporárias, um caso com `@Disabled`, uma suíte PostgreSQL excluída e um método PostgreSQL retirado de uma classe mista reprovaram pelo motivo esperado. Relatórios válidos de uma execução anterior estavam presentes nessas cópias e não supriram a ausência de resultados da execução atual.
- **Revisão:** conferidos, nessa entrega, o escopo de teste das dependências de Flyway, a ausência de valores de conexão padrão na preparação, o XML sem acesso a recursos externos, a distinção entre testes de classes mistas e os relatórios separados por execução. A lista obrigatória incluía 17 suítes e os 11 métodos PostgreSQL das classes mistas; deve acompanhar futuras mudanças de nomes e novos testes. As ferramentas de preparação e conferência não contam como casos JUnit.
- **Relatórios:** a execução completa fica em `precificacao-rica/target/surefire-integracao/<identificador-da-execucao>`. Resumo da validação local em `precificacao-rica/target/validacao-etapa1-20261110.json`. Arquivos de `target` não são versionados.
- **Próxima etapa proposta:** etapa 2, preparação do banco ao iniciar a aplicação. Pedir ok e aguardar antes de implementar.

### Etapa 2 — Preparar o banco ao iniciar a aplicação

**Entrega.** Comportamento de inicialização equivalente ao anêmico: criação do banco ausente quando habilitada, seguida das migrações do rico.

**Implementação.**

1. Criar objetos de infraestrutura para preparar o banco e aplicar o Flyway por uma operação explícita. A construção desses objetos não realiza I/O.
2. Preservar `RICO_DB_URL`, `RICO_DB_USER`, `DB_PASSWORD` e `RICO_SERVER_PORT`. Definir e documentar as opções de criação e banco de manutenção, sem mudar os padrões já usados para a API.
3. Acrescentar as dependências de execução do Flyway necessárias; o plugin Maven atual, sozinho, não executa migrações ao iniciar o JAR.
4. Tentar criar somente após a falha específica de banco inexistente. Proteger o identificador SQL, tratar a criação concorrente e manter dados de bancos existentes.
5. Acrescentar a configuração própria de testes de criação, por exemplo `RICO_BANCO_CRIACAO_TESTE_URL`, apontando para um servidor exclusivo.

**Aceite.** Banco ausente é criado e migrado; reiniciar conserva os dados. Com criação desativada e banco ausente, a falha informa a causa correta. Banco existente funciona sem `CREATEDB`. Falha de senha não provoca criação. Duas inicializações concorrentes terminam corretamente.

**Revisão e parada.** Conferir que a preparação usa o banco rico, ocorre antes das rotas e não é iniciada por construtores. Executar os testes em servidor descartável e pedir ok para a etapa 3.

**Encerramento — 10 de novembro de 2026. Status: concluída. A revisão da etapa 3 foi autorizada posteriormente e também concluída.**

- **Implementação:** `AplicacaoRica` chama `BancoPostgres.preparar()` antes de abrir o `ServerSocket` e montar as rotas. Os objetos de `infraestrutura/banco` separam configuração, abertura de conexões, criação e tratamento da falha de inicialização. Seus construtores não acessam o PostgreSQL.
- **Configuração preservada:** `RICO_DB_URL`, `RICO_DB_USER`, `DB_PASSWORD` e `RICO_SERVER_PORT`, com os padrões anteriores. Acrescentadas `RICO_DB_CRIAR_SE_AUSENTE=true` e `RICO_DB_MANUTENCAO=postgres`. Valores diferentes de `true`/`false` na opção de criação são rejeitados. Desativar a criação não desativa o Flyway em bancos existentes.
- **Criação:** ocorre somente após SQLState `3D000`. O nome é interpretado conforme o driver PostgreSQL, validado em bytes e escapado como identificador SQL. A conexão de manutenção preserva servidores e opções da URL; opções que desviam seu banco são rejeitadas. Uma colisão de criação só é aceita após confirmar que o banco esperado existe.
- **Migrações e recursos:** Flyway passou a ser dependência de execução e aplica as V1 a V10 antes da API ficar disponível. Não foi necessário criar ou modificar uma migração. O pool é fechado se a migração falhar e é administrado pelo `try-with-resources` da aplicação quando a preparação tem sucesso.
- **Testes adicionados:** 17 casos sem banco para URLs, codificação, nomes SQL, limites e construção sem conexão; sete casos de integração para criação pela API, reinício com dados preservados, criação desativada, permissões, senha incorreta, nome com aspas, concorrência e falha de migração. Alguns cenários relacionados são conferidos no mesmo caso de teste.
- **Resultados reais:** 143 casos em 16 suítes no modo padrão e 175 em 19 suítes no completo, sem falhas, erros ou ignorados. Usado PostgreSQL 14 descartável com autenticação por senha. As duas criações foram observadas simultaneamente bloqueadas no catálogo antes de serem liberadas; ambas terminaram com as dez migrações aplicadas.
- **Valor da verificação:** retirar o tratamento da colisão somente de uma cópia temporária fez o teste de concorrência falhar com erro PostgreSQL. A cópia principal conservou o tratamento e passou na validação final. A ausência de `RICO_BANCO_CRIACAO_TESTE_URL` também reprovou o perfil antes de acessar o banco.
- **Pacote executável:** `mvn -s configuracao-maven.xml -pl precificacao-rica package` gerou o JAR. Ao executá-lo com banco novo, `GET /api/categorias` respondeu `200` com `[]` após a preparação. Com banco ausente e `RICO_DB_CRIAR_SE_AUSENTE=false`, o JAR encerrou com falha específica e não publicou a API. Os processos e o servidor temporário foram encerrados após a validação.
- **Revisão:** conferidos o fechamento de conexões, a preservação da causa e SQLState, os logs sem credenciais, o desvio de URL, a exigência de permissões somente para banco ausente e a coordenação da concorrência. O perfil completo agora exige também o endereço de manutenção de um servidor exclusivo de testes e registra as duas novas suítes obrigatórias.
- **Documentação:** READMEs e este plano atualizados. Resumo local em `precificacao-rica/target/validacao-etapa2-20261110.json`; relatórios completos no diretório próprio da execução em `target/surefire-integracao`.
- **Próxima etapa proposta:** etapa 3, revisão e validação do cadastro e listagem de categorias já implementados. Aguardar ok antes de avançar.

### Etapa 3 — Cadastrar e listar categorias

**Entrega.** `POST /api/categorias` e `GET /api/categorias`.

**Implementação.**

1. Criar os contratos `Categoria` e `Categorias` e um valor que concentre a regra de nome.
2. Implementar os objetos `Pg...`, a adaptação dos resultados e a migração de `categoria`, com código gerado e índice único de nome sem distinguir capitalização.
3. Criar os `Tk...` de cadastro e listagem e registrar as rotas em `AplicacaoRica`. Devolver código e nome, com listagem por código crescente.

**Aceite.** Cadastro válido retorna `201`; listagem retorna `200`, inclusive vazia. Nome ausente, vazio, acima do limite ou duplicado com outra capitalização retorna `400`. Falha real de persistência é distinguida de duplicidade.

**Revisão e parada.** Conferir construção sem SQL, validação no objeto de valor, consulta parametrizada e restrição de unicidade. Pedir ok para a etapa 4.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída. A revisão da etapa 4 foi autorizada posteriormente e também concluída.**

- **Entrega aproveitada:** cadastro e listagem, seus contratos, `PgCategorias`, objetos `Tk`, respostas JSON e a migração V3 já existiam. Foram revisados; as rotas e a tabela não foram recriadas.
- **Arquitetura e código:** conferidas a validação em `NomeCategoria`, a ausência de SQL nos construtores, a separação entre HTTP e PostgreSQL, os parâmetros de consulta, as projeções de código/nome e a lista imutável. O cadastro usa uma operação SQL com `ON CONFLICT (LOWER(nome))`, e a listagem ordena por código, sem consulta adicional por categoria.
- **Defeito corrigido:** o nome com caractere nulo era aceito pelo objeto de valor e chegava ao acesso ao PostgreSQL. A validação agora rejeita esse nome antes da conexão e devolve `400`, seguindo a regra já presente no anêmico. Antes da correção, o teste novo falhou porque `DataSourceSemConexao` detectou a tentativa de conexão; depois passou.
- **Diagnóstico:** falhas de SQL no cadastro e na listagem passaram a registrar a operação e SQLState, sem registrar o nome informado ou credenciais. A causa continua preservada em `FalhaCategoria`, e a resposta HTTP continua genérica.
- **Testes relevantes:** mantidos os testes de limite de 200 caracteres, remoção de espaços, apóstrofo/acento, duplicidade sem distinguir capitalização, lista vazia, ordenação por código e indisponibilidade. Acrescentados o caractere nulo e corpos sem objeto JSON único: `null`, array e dois objetos consecutivos.
- **Falha de gravação:** um gatilho temporário em PostgreSQL provoca SQLState `23505` de outra restrição após a inserção. O teste exige resposta `503`, ausência do registro rejeitado e preservação da categoria anterior; essa falha não pode ser tratada como nome duplicado. O gatilho e sua função são removidos na limpeza. O novo método foi incluído na lista obrigatória de integração das classes mistas.
- **Resultados reais:** `mvn -s configuracao-maven.xml -pl precificacao-rica test` passou com 147 casos em 16 suítes. O comando com `-Pintegracao test` passou com 180 casos em 19 suítes, sem falhas, erros ou ignorados, em PostgreSQL 14 descartável. Não houve alteração de migrações nem do módulo anêmico nesta revisão.
- **Arquivos:** `NomeCategoria.java`, `PgCategorias.java`, `CadastroCategoriaTest.java`, registro obrigatório em `src/test/ant/integracao.xml`, README do rico e documentação do plano. Os três arquivos Java alterados foram ajustados para a indentação de dois espaços, preservando o texto das mensagens existentes e o UTF-8.
- **Registros locais:** resumo em `precificacao-rica/target/validacao-etapa3-20261110.json`; evidência do defeito anterior em `target/validacao-etapa3-regressao/antes-da-correcao.xml`; relatórios completos no diretório da execução em `target/surefire-integracao`.
- **Próxima etapa proposta:** revisão da etapa 4, vínculo opcional de categoria com produto. Aguardar ok antes de continuar.

### Etapa 4 — Vincular categoria ao produto

**Entrega.** Categoria opcional no cadastro e GET de produto e `PATCH /api/produtos/{codigo}/categoria`.

**Implementação.**

1. Acrescentar o vínculo opcional e sua chave estrangeira por nova migração.
2. Evoluir os contratos e objetos de produto, incluindo `PgProduto`, `ProdutoConstante`, as coleções, os adaptadores SQL e os fakes afetados.
3. Interpretar `codigoCategoria` na borda sem aceitar conversão silenciosa de um número fracionário em código inteiro. Validar o vínculo pela coleção de categorias.
4. Gravar somente o vínculo na alteração, preservando nome e precificação. Na resposta, omitir `codigoCategoria` quando ausente, como no anêmico.

**Aceite.** Cadastrar produto com e sem categoria, consultar e trocar o vínculo. Categoria inexistente retorna `404`; código inválido retorna `400`. O PATCH exige uma categoria válida, conforme o contrato atual, e mantém os valores de preço.

**Revisão e parada.** Conferir os implementadores dos contratos e a atualização sem sobrescrita de outros campos. Não acrescentar uma operação de remoção do vínculo que o contrato atual não oferece. Pedir ok para a etapa 5.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída. A revisão da etapa 5 foi autorizada posteriormente e também concluída.**

- **Entrega aproveitada:** vínculo opcional no cadastro e GET, PATCH de categoria, `CodigoCategoria`, adaptadores SQL, coleções e decorador de produto já existiam. Foram conferidos os contratos, a conversão numérica exata, a chave estrangeira da V4 e a atualização que grava somente `categoria_codigo`. As rotas e a migração não foram recriadas.
- **Defeito corrigido:** `PgProdutos` tratava qualquer SQLState `23503` como categoria ausente. Agora confere a falha PostgreSQL e a restrição `produto_categoria_codigo_fkey`; somente essa referência produz o motivo de categoria ausente. Outras falhas de integridade retornam `503`, com causa preservada e logs da operação/SQLState, sem registrar credenciais ou valores informados.
- **Reprodução:** antes da correção, dois testes de integração falharam ao exigir `503` no cadastro e no PATCH para uma violação de outra referência. A simulação usa um gatilho temporário que informa outra restrição. Após a correção, ambos passaram e os registros anteriores permaneceram iguais, sem produto residual.
- **Integridade real:** acrescentados dois casos em `PgProdutosTest`, para cadastro e alteração. Uma coleção de categorias de teste observa a referência existente e a remove antes da escrita, de forma controlada. A chave estrangeira real do PostgreSQL rejeita a operação; o teste exige SQLState `23503`, o nome correto da restrição e o motivo `CATEGORIA_AUSENTE`. Confere também que não houve gravação parcial. Não se usam esperas para provocar essa condição.
- **Leitura e campos opcionais:** ampliados testes existentes para cadastro com `codigoCategoria` omitido, além de nulo ou preenchido. O campo ausente continua omitido na resposta. Na troca, a resposta e o novo GET refletem a categoria atual, enquanto o `ProdutoConstante` obtido antes mantém sua leitura anterior; `PgProduto` e uma nova consulta à coleção acompanham o vínculo atualizado. Nome e precificação permanecem iguais.
- **Indisponibilidade:** dois casos sem banco verificam que falha na consulta da categoria retorna `503` no POST e no PATCH, em vez de virar categoria inexistente. A consulta de existência passou a registrar SQLState ao falhar. O helper de respostas dos testes agora compara diretamente o status HTTP para facilitar o diagnóstico.
- **Testes e resultados reais:** `mvn -s configuracao-maven.xml -pl precificacao-rica test` passou com 149 casos em 16 suítes; o comando com `-Pintegracao test` passou com 186 casos em 19 suítes. Zero falhas, erros ou ignorados na validação final, em PostgreSQL 14 descartável. Os novos métodos de integração foram incluídos no registro obrigatório. Não houve alteração de migração ou do módulo anêmico.
- **Arquivos:** `PgProdutos.java`, log da consulta em `PgCategorias.java`, `CategoriaDoProdutoTest.java`, `PgProdutosTest.java`, `src/test/ant/integracao.xml`, README do rico e documentação do plano. Os três arquivos que ainda usavam quatro espaços foram ajustados para dois, mantendo UTF-8 e as mensagens existentes.
- **Registros locais:** resumo em `precificacao-rica/target/validacao-etapa4-20261110.json`; evidência anterior em `target/validacao-etapa4-regressao/antes-da-correcao.xml`; relatórios completos no diretório da execução em `target/surefire-integracao`. O gatilho, a função e os dados temporários foram removidos e o servidor descartável foi encerrado.
- **Próxima etapa proposta:** revisão da etapa 5, listagem e filtros de produtos já implementados. Aguardar ok antes de continuar.

### Etapa 5 — Listar e filtrar produtos

**Entrega.** `GET /api/produtos` com `nome`, `modalidadePrecificacao` e `categoria` opcionais.

**Implementação.**

1. Acrescentar a consulta à coleção `Produtos` e aos implementadores existentes.
2. Representar os filtros com intenção clara, aplicando normalização e validação sem levar tipos HTTP ao domínio.
3. Combinar as condições informadas com SQL parametrizado e ordenar pelo código. Materializar os dados necessários para evitar um SELECT por atributo ou por serialização.

**Aceite.** Conferir filtros isolados e combinados, busca por trecho sem distinguir capitalização, espaços nas pontas, ausência de correspondências e listagem sem filtro. Tratar `%` e `_` literalmente. Modalidade e código de categoria inválidos retornam `400`.

**Revisão e parada.** Conferir combinação com `AND`, ordenação, formato da lista e quantidade de leituras. Pedir ok para a etapa 6.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída. A revisão da etapa 6 foi autorizada posteriormente e também concluída.**

- **Entrega aproveitada:** `GET /api/produtos`, `FiltrosProdutos`, leitura dos parâmetros em `TkListaProdutos` e consulta em `ConsultaSqlDeProdutos` já existiam. Conferidos os filtros isolados e combinados, o uso de `AND`, os parâmetros SQL, a busca sem distinguir capitalização e o tratamento literal de `%` e `_`. A rota não foi recriada.
- **Diferença corrigida:** a normalização do nome estava somente no HTTP. Ao consultar a coleção diretamente com o mesmo trecho cercado de espaços, a busca não encontrava os produtos. `FiltrosProdutos` agora normaliza o trecho na construção e representa um nome em branco como filtro ausente; o HTTP permanece responsável por interpretar os parâmetros. As três opções do objeto de filtros também têm contrato explícito de nulabilidade. O comportamento da API foi preservado.
- **Reprodução:** um teste existente foi ampliado para consultar diretamente `PgProdutosConstantes` com o nome cercado de espaços e com nome em branco. Antes da correção, falhou por retornar lista vazia em vez dos quatro produtos esperados. Depois passou, sem criar um teste separado que apenas confere atribuição de campos.
- **Leitura completa e ordenação:** acrescentado `ListaProdutosTest`, que insere dois produtos em ordem inversa à dos códigos e escolhe nomes que distinguem a ordenação por nome da ordenação por código. Confere a resposta inteira em ordem de código, com acento, apóstrofo, valores de precificação e categoria presente ou omitida. Cada resposta completa exige somente um comando JDBC da aplicação, inclusive durante a serialização. Também verifica nova leitura após alteração no banco e resposta `200` com `[]` quando não há correspondência.
- **Valor do teste de consultas:** a instrumentação `SqlContado` existe somente nos testes e observa comandos JDBC sem substituir o banco real. Em uma cópia temporária, trocar a coleção materializada por `PgProdutos` na montagem da API fez o teste detectar sete comandos para os dois produtos, em vez de um. A alteração ficou fora do checkout principal. A medição se limita aos comandos JDBC originados pela aplicação.
- **Código e diagnóstico:** `ConsultaSqlDeProdutos` passou a registrar SQLState ao falhar, sem registrar os parâmetros. Mantidos os construtores sem acesso a banco, a lista imutável, as projeções internas de colunas e o tratamento `400` para modalidade/categoria inválidas e `503` para indisponibilidade. A API continua usando `PgProdutosConstantes`; a variante de objetos com leitura direta foi preservada.
- **Resultados reais:** `mvn -s configuracao-maven.xml -pl precificacao-rica test` passou com 149 casos em 16 suítes. O comando com `-Pintegracao test` passou com 187 casos em 20 suítes, sem falhas, erros ou ignorados, em PostgreSQL 14 descartável. A nova suíte foi incluída no registro obrigatório e as verificações já existentes foram ampliadas.
- **Arquivos:** `FiltrosProdutos.java`, `TkListaProdutos.java`, `ConsultaSqlDeProdutos.java`, ampliação em `CategoriaDoProdutoTest.java`, novos `ListaProdutosTest.java` e `SqlContado.java`, registro em `src/test/ant/integracao.xml`, README do rico e documentação do plano. Os três arquivos de produção alterados foram ajustados para dois espaços e mantiveram UTF-8 e as mensagens existentes. Não houve alteração de migração ou do módulo anêmico.
- **Registros locais:** resumo em `precificacao-rica/target/validacao-etapa5-20261110.json`; evidência anterior em `target/validacao-etapa5-regressao/antes-da-correcao.xml`; relatórios completos no diretório da execução em `target/surefire-integracao`. Os registros temporários e o servidor descartável foram removidos após a validação.
- **Próxima etapa proposta:** revisão da etapa 6, alteração de precificação e histórico já implementados. Aguardar ok antes de continuar.

### Etapa 6 — Alterar precificação e disponibilizar histórico

**Entrega.** `PATCH /api/produtos/{codigo}/precificacao` e `GET /api/produtos/{codigo}/historico-precificacao`.

**Implementação.**

1. Definir a operação de reprecificação no desenho de produto e reutilizar `Precificacoes`, as estratégias e o tipo que representa cálculo validado. Garantir a regra de duas grandezas também fora do HTTP.
2. Criar os registros de histórico e sua coleção de consulta dentro do assunto produtos, com migração própria e valores anteriores e novos.
3. Em uma única conexão, iniciar a transação, ler o produto protegido contra concorrência, calcular pela modalidade cadastrada, comparar os valores e gravar produto e histórico quando houver mudança.
4. Confirmar a transação somente ao concluir as duas escritas. Em falha, desfazer ambas. Os colaboradores dessa operação devem usar a mesma conexão.
5. Adaptar as rotas e a resposta atualizada; manter nome, modalidade e categoria. Devolver histórico ordenado do mais recente para o mais antigo.

**Aceite.** Exercitar os três cálculos nas duas modalidades e o custo calculado persistido. Repetição dos valores não altera o histórico. Rejeitar três grandezas e resultados inválidos. Provocar falha na inserção do histórico e confirmar o estado anterior no banco. Verificar alterações concorrentes e produto ausente.

**Revisão e parada.** Conferir responsabilidade dos objetos, reutilização da regra e fronteira da transação. Garantir que os decoradores devolvam o estado correto e que uma chamada interna não abra outra conexão. Pedir ok para a etapa 7.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 7 autorizado pelo usuário.**

- **Código existente preservado:** cálculo pela modalidade cadastrada, `SELECT ... FOR UPDATE`, atualização e histórico na mesma conexão, confirmação após as duas escritas, desfazimento em falha e comparação numérica para evitar histórico repetido já estavam implementados. A revisão conferiu e testou essa estrutura. A consulta do histórico já distingue produto ausente de lista vazia com um `LEFT JOIN`, ordenando os registros pelo código decrescente.
- **Matriz de cálculos:** seis casos de PostgreSQL exercitam venda, custo e percentual calculados nas duas modalidades. Conferem resposta, novo GET, valores diretamente gravados e histórico com estados anterior e novo. Mantêm nome, modalidade e categoria. A instrumentação verifica uma conexão e três comandos JDBC por alteração efetiva; a repetição usa uma conexão e somente a leitura, sem `UPDATE` ou `INSERT`.
- **Entradas inválidas:** três casos unitários conferem a rejeição de zero, uma ou três grandezas diretamente em `ValoresParaPrecificar`, fora do HTTP. Dois casos sem banco rejeitam tentativa de enviar modalidade ou categoria no PATCH de preço. Quatro casos PostgreSQL preservam produto e histórico após três grandezas coerentes, custo negativo, percentual positivo sem aumento do preço arredondado e margem de 100%.
- **Desfazimento real:** um gatilho temporário `AFTER INSERT` no histórico confirma que o produto já tem os valores novos, avança uma sequência de teste e provoca a falha. Como o avanço da sequência é observável após o desfazimento, o teste comprova que as duas escritas foram alcançadas. Exige `503`, preço/custo/percentual anteriores e histórico antigo intactos. Após retirar a falha, a mesma operação é repetida com sucesso.
- **Concorrência observada:** duas requisições são mantidas simultaneamente bloqueadas por uma sessão independente antes da liberação. Com valores diferentes, o histórico deve formar a sequência estado inicial → primeira alteração confirmada → segunda alteração, e o produto corresponde ao último registro. Com valores iguais, as duas respostas concordam e existe somente um registro de alteração. O teste confere essas propriedades dos dados, sem depender da grafia da cláusula de bloqueio ou presumir a ordem de vitória das requisições.
- **Verificações negativas:** retirar o início e a confirmação explícitos da transação somente de uma cópia temporária, deixando as escritas em autocommit, fez falhar a preservação do preço no teste de desfazimento. Retirar somente `FOR UPDATE` de outra cópia fez os dois cenários de concorrência detectarem a quebra da sequência do histórico e o registro duplicado da mesma alteração. Essas alterações permaneceram fora do checkout principal.
- **Diagnóstico e revisão:** acrescentados logs da operação e SQLState para falha de alteração, de desfazimento e de consulta do histórico. A falha original é preservada; uma falha no próprio desfazimento é anexada como exceção suprimida. Nenhum parâmetro financeiro ou credencial é registrado nesses logs. Conferidas a reutilização das estratégias, a ausência de I/O em construtores, a materialização da resposta e a lista imutável de histórico.
- **Resultados reais:** `mvn -s configuracao-maven.xml -pl precificacao-rica test` passou com 154 casos em 17 suítes. O comando com `-Pintegracao test` passou com 205 casos em 22 suítes, sem falhas, erros ou ignorados. A suíte nova possui 13 casos de integração. Usado PostgreSQL 14 descartável; os gatilhos, a função, a sequência, os dados e o servidor temporários foram removidos após a validação. Não houve alteração de migração ou do módulo anêmico.
- **Arquivos:** logs em `AlteracaoSqlDePrecificacao.java` e `PgHistoricoDePrecificacao.java`; ampliação de `HistoricoPrecificacaoTest.java` e `SqlContado.java`; novos `ValoresParaPrecificarTest.java` e `ReprecificacaoPostgresTest.java`; registro obrigatório em `src/test/ant/integracao.xml`; README do rico e documentação do plano. Mantidos UTF-8, mensagens existentes e indentação de dois espaços nos arquivos alterados.
- **Registros locais:** resumo em `precificacao-rica/target/validacao-etapa6-20261110.json`; relatórios completos no diretório da execução em `target/surefire-integracao`; registros das duas verificações negativas nas cópias temporárias em `C:\Java\Workspace\tmp\precificacao-rico-etapa6-20261110`.
- **Próxima etapa proposta:** revisão da etapa 7, renomeação de categorias já implementada. Aguardar ok antes de continuar.

### Etapa 7 — Renomear categoria

**Entrega.** `PATCH /api/categorias/{codigo}`, mantendo o código e alterando somente o nome.

**Implementação.** Reutilizar o valor de nome do cadastro, acrescentar a operação ao contrato apropriado e aos objetos `Pg...` e `Tk...`. Gravar somente `nome`; traduzir o conflito específico do índice em `400` e categoria ausente em `404`.

**Aceite.** Renomear uma categoria vinculada a produto, confirmar a listagem e preservar o vínculo e o histórico de precificação. Repetir o próprio nome é permitido. Nome de outra categoria, inclusive com outra capitalização, é rejeitado sem alteração parcial.

**Revisão e parada.** Conferir a regra compartilhada, a manutenção da identidade e a tradução específica dos erros. Pedir ok para a etapa 8.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 8 autorizado pelo usuário.**

- **Implementação preservada:** `Categorias.renomear`, `PgCategorias`, `TkRenomeacaoCategoria` e a rota já existiam. Conferidas a reutilização de `NomeCategoria`, a rejeição de campos extras e códigos inválidos antes da conexão, a resposta materializada e a atualização parametrizada somente de `nome`. Uma única instrução SQL basta para essa alteração; o índice existente garante a unicidade sem uma consulta prévia sujeita a concorrência.
- **Identidade e vínculos:** ampliado o teste já existente em `CategoriaDoProdutoTest`, sem duplicar o fluxo. Agora ele também muda apenas a capitalização do próprio nome e usa texto com acento e apóstrofo. Confere o mesmo código, a repetição do nome, conflito com outra categoria sem distinguir capitalização, categoria ausente, listagem, produto vinculado e histórico anterior preservados.
- **Falha durante a escrita:** novo `RenomeacaoCategoriaPostgresTest` provoca uma violação de unicidade de outra restrição em um gatilho temporário `AFTER UPDATE`. Exige `503` com resposta genérica, conserva a listagem anterior e, após retirar o gatilho, repete a operação com sucesso e o mesmo código. O cenário protege a distinção entre nome duplicado e falha de persistência, sem acrescentar casos repetidos apenas para aumentar a contagem.
- **Verificação negativa:** em uma cópia temporária, retirar somente a conferência de `ux_categoria_nome` fez o novo teste detectar `400` no lugar de `503`. Os demais 205 casos passaram; o checkout principal conservou a conferência específica. O teste novo foi registrado como suíte obrigatória do perfil de integração.
- **Diagnóstico e estilo:** acrescentado log de falha de renomeação com operação e SQLState, preservando a causa e sem registrar nome, SQL ou credenciais. `TkRenomeacaoCategoria` passou a usar dois espaços. Mantidos UTF-8 e mensagens existentes; não houve mudança de regra, migração ou do módulo anêmico.
- **Resultados reais:** o comando unitário passou com 154 casos em 17 suítes; `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test` passou com 206 casos em 23 suítes, sem falhas, erros ou ignorados. Usado PostgreSQL 14 descartável. Conferidos zero registros de apoio e zero funções/gatilhos temporários antes de encerrar o servidor de testes.
- **Arquivos e registros:** `PgCategorias.java`, `TkRenomeacaoCategoria.java`, ampliação de `CategoriaDoProdutoTest.java`, novo `RenomeacaoCategoriaPostgresTest.java`, registro em `src/test/ant/integracao.xml`, README do rico e este plano. Resumo local em `precificacao-rica/target/validacao-etapa7-20261110.json`; relatórios finais em `target/surefire-integracao/20261110-131346301`; evidência negativa na cópia em `C:\Java\Workspace\tmp\precificacao-rico-etapa7-20261110\duplicidade-generica`.
- **Próxima etapa proposta:** revisão da etapa 8, cadastro e listagem de cidades já implementados. Aguardar ok antes de continuar.

### Etapa 8 — Cadastrar e listar cidades

**Entrega.** `POST /api/cidades` e `GET /api/cidades`.

**Implementação.** Criar `Cidade`, `Cidades` e os valores necessários de nome e UF, com objetos `Pg...` e `Tk...`. Criar a migração com código gerado e unicidade de nome e UF. Normalizar UF em maiúsculas e manter a listagem por código.

**Aceite.** Cadastrar cidade com UF em minúsculas, rejeitar UF inválida e nome vazio. Aceitar o mesmo nome em UFs diferentes e rejeitar repetição na mesma UF sem distinguir a capitalização do nome. Conferir a listagem vazia.

**Revisão e parada.** Conferir normalização no objeto responsável, restrição de banco e construção sem conexão. Pedir ok para a etapa 9.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 9 autorizado pelo usuário.**

- **Implementação existente:** conferidos `Cidade`/`Cidades`, `CidadeConstante`, `NomeCidade`, `UnidadeFederativa`, `PgCidades`, adaptadores HTTP e migração V7. Mantidas a UF normalizada por `UnidadeFederativa.daSigla`, a unicidade por `LOWER(nome)` e UF, a inserção parametrizada com conflito específico e a resposta materializada. Não foram recriados objetos ou migrações.
- **Correção de entrada:** `NomeCidade` agora rejeita o caractere nulo, que não pode ser armazenado como texto no PostgreSQL. A validação fica no valor de domínio e retorna `400` antes de pedir uma conexão. Antes da correção, o novo caso com `DataSourceSemConexao` falhou justamente por tentar acessar o banco; a evidência foi preservada em `target/validacao-etapa8-regressao/antes-da-correcao.xml`.
- **Cadastro e limites:** ampliado o teste existente para cadastrar um nome com exatamente 200 caracteres após remover espaços das pontas, incluindo acento e apóstrofo. Mantidas as verificações de UF em minúsculas, conflito na mesma UF sem distinguir a capitalização e aceitação do mesmo nome em outra UF. A rejeição de 201 caracteres já tinha teste. Acrescentado o corpo JSON `null` à verificação de formato inválido antes do banco.
- **Falha durante a gravação:** um novo caso em `CadastroCidadeTest` provoca uma violação de unicidade de outra restrição por gatilho temporário `AFTER INSERT`. Exige `503` genérico, compara a listagem completa com o estado anterior e repete o cadastro com sucesso depois de retirar a falha. O caso foi registrado como obrigatório no perfil de integração.
- **Listagem e construção:** o teste PostgreSQL existente de `ListaCidadesTest` continua conferindo os campos e a ordem por código com inserções em outra ordem. A instrumentação agora exige zero conexões e comandos durante a construção das rotas e uma conexão com um comando JDBC para montar a listagem. Permanecem os testes de resposta vazia `[]` e de indisponibilidade, que deve retornar `503` em vez de uma lista vazia.
- **Código e diagnóstico:** acrescentados logs com operação e SQLState nas falhas de cadastro e listagem, preservando a causa e sem registrar os dados recebidos ou credenciais. Ajustados para dois espaços `NomeCidade`, `PgCidades`, `TkCadastroCidade` e os dois arquivos de teste alterados. Mantidos UTF-8 e mensagens existentes. A revisão dos filtros por nome e UF continua nas etapas 9 e 10.
- **Resultados reais:** 156 casos em 17 suítes sem banco; 209 casos em 23 suítes no comando `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. Usado PostgreSQL 14 descartável; conferidos zero registros de cidades/clientes de apoio e zero funções/gatilhos da etapa antes de encerrar o servidor. Não houve alteração de migração ou do módulo anêmico.
- **Arquivos e registros:** `NomeCidade.java`, `PgCidades.java`, `TkCadastroCidade.java`, ampliação de `CadastroCidadeTest.java` e `ListaCidadesTest.java`, registro em `src/test/ant/integracao.xml`, README do rico e este plano. Resumo local em `precificacao-rica/target/validacao-etapa8-20261110.json`; relatórios completos em `target/surefire-integracao/20261110-132153930`.
- **Próxima etapa proposta:** revisão da etapa 9, filtro de cidades por nome já implementado. Aguardar ok antes de continuar.

### Etapa 9 — Filtrar cidades por nome

**Entrega.** `GET /api/cidades?nome=curitiba`, por trecho e sem distinguir capitalização.

**Implementação.** Acrescentar o filtro à coleção e à consulta PostgreSQL. Remover espaços das pontas, tratar branco como ausência e manter nome como parâmetro SQL. Aplicar os mesmos limites de entrada do anêmico.

**Aceite.** Testar trechos em maiúsculas e minúsculas, nenhum resultado, filtro em branco, ausência de filtro e `%` e `_` literais. Resposta sem correspondências é `200` com `[]`.

**Revisão e parada.** Conferir o tratamento literal do texto e a ordenação. Pedir ok para a etapa 10.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 10 autorizado pelo usuário.**

- **Revisão da regra e do SQL:** mantidos o limite e a remoção de espaços por `NomeCidade`, o filtro em branco como ausência, a busca por trecho com `STRPOS(LOWER(nome), LOWER(?))`, o parâmetro JDBC e a ordenação por código. A coleção continua materializando os dados em uma única consulta; construtores não acessam o banco. Conferidos os mesmos limites de entrada do anêmico.
- **Falha reproduzida e corrigida:** a busca por `D'ALIANÇA` retornava `[]` para um nome cadastrado com esse trecho. O relatório registra Windows 11, Java 17.0.12 e codificação padrão `Cp1252`. A inspeção do Takes 1.26.0 confirmou que `Href` usa essa codificação para decodificar os parâmetros; uma consulta direta confirmou que o PostgreSQL comparava corretamente as letras acentuadas. O caso falhou antes da correção e passou depois.
- **Correção no HTTP:** `TkListaCidades` agora lê a consulta bruta da URI e decodifica nomes e valores dos parâmetros uma única vez, com UTF-8 explícito. Separa os parâmetros antes de decodificar seus valores, preservando `%`, `+`, `&` e `=` quando fazem parte do nome. As validações de nome continuam no valor de domínio, sem alterar a configuração global da JVM.
- **Testes aproveitados:** ampliado o cenário PostgreSQL existente em `ListaCidadesTest`, sem criar uma segunda suíte de filtros. Confere nome com acento e apóstrofo, busca por trecho em maiúsculas/minúsculas, filtro de exatamente 200 caracteres cercado de espaços, `%` e `_` literais, símbolos codificados na URL, texto semelhante a SQL, ausência de correspondências, branco e vazio. A busca filtrada deve usar uma conexão e um comando JDBC e conservar a ordem e os campos da resposta.
- **Entrada inválida e falha de banco:** acrescentado um caso sem banco para caractere nulo recebido pela URL; a rejeição de 201 caracteres já estava coberta. O teste existente de indisponibilidade agora também envia um filtro, exigindo `503` em vez de `[]`. Mantidas as mensagens e a validação anterior à conexão.
- **Verificação negativa:** somente numa cópia temporária, substituir `STRPOS` por `LIKE` sem escape fez o cenário retornar também a segunda cidade ao buscar o texto literal com `%_`. O teste detectou essa correspondência indevida; os outros 209 casos passaram. A consulta do checkout principal foi preservada.
- **Resultados reais:** 157 casos em 17 suítes sem banco; 210 casos em 23 suítes no comando `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. Usado PostgreSQL 14 descartável. Conferidos zero registros de cidades/clientes e zero funções/gatilhos temporários antes de encerrar o servidor.
- **Arquivos e registros:** `TkListaCidades.java`, ampliação de `ListaCidadesTest.java`, README do rico e este plano. Mantidos UTF-8 e dois espaços. Não houve mudança de migração ou do módulo anêmico; o cenário PostgreSQL ampliado já integra o registro obrigatório. Resumo local em `precificacao-rica/target/validacao-etapa9-20261110.json`; evidência anterior em `target/validacao-etapa9-regressao/antes-da-correcao.xml`; relatórios finais em `target/surefire-integracao/20261110-133631711`; verificação negativa em `C:\Java\Workspace\tmp\precificacao-rico-etapa9-20261110\curingas-no-filtro`.
- **Próxima etapa proposta:** revisão da etapa 10, filtro de cidades por UF e combinação com o nome. Aguardar ok antes de continuar.

### Etapa 10 — Filtrar cidades por UF

**Entrega.** `GET /api/cidades?uf=PR` e combinação com `nome`.

**Implementação.** Reutilizar a regra de UF do cadastro, acrescentar a condição opcional e combinar os filtros com `AND`. Manter branco como filtro ausente e devolver os mesmos dados e ordem da listagem.

**Aceite.** Testar UF válida em minúsculas e com espaços, UF inválida, filtros isolados e combinação com nome. Conferir que os códigos retornados identificam cidades cadastradas.

**Revisão e parada.** Conferir a normalização compartilhada e as combinações de parâmetros. Pedir ok para a etapa 11.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 11 autorizado pelo usuário.**

- **Implementação preservada:** a leitura HTTP já reaproveita `UnidadeFederativa.daSigla`, a mesma regra do cadastro. Conferidos `strip`, maiúsculas com `Locale.ROOT`, validação das siglas brasileiras e UF vazia ou em branco como filtro ausente. `PgCidades` mantém os valores vinculados como parâmetros JDBC, combina nome e UF com `AND` e ordena por código. Não foi necessário alterar a regra ou o SQL.
- **Organização dos testes:** as verificações de UF saíram do fluxo que também exercitava a busca por nome e passaram a um cenário próprio em `ListaCidadesTest`. O cenário anterior foi renomeado para `listaPorCodigoEAplicaFiltroDeNome`. Atualizado o registro obrigatório dos dois métodos PostgreSQL em `src/test/ant/integracao.xml`, sem perder os critérios anteriores.
- **Códigos e campos reais:** o novo cenário cadastra três cidades pela API: duas com o mesmo nome em UFs diferentes e outra cidade em uma dessas UFs. Compara as respostas filtradas com os objetos retornados no cadastro, incluindo os códigos gerados. Confere filtro isolado, nome combinado com UF, ordem invertida dos parâmetros na URL e minúsculas com espaços nas extremidades.
- **Combinação e ausência:** uma busca com uma cidade correspondente só ao nome e outra só à UF precisa retornar `[]`. Nome em branco mantém somente a UF; UF em branco mantém somente o nome; UF vazia sem nome devolve a listagem completa. Cada consulta do cenário deve usar uma conexão e um comando JDBC, preservando a ordenação e os campos da resposta.
- **Entrada inválida:** ampliado o caso sem banco para rejeitar UF inválida tanto isoladamente quanto junto do nome. Ambos exigem `400` com a mensagem da validação compartilhada, antes de abrir uma conexão. Evitada uma sequência de casos equivalentes apenas com outras grafias de siglas inválidas.
- **Verificação negativa:** somente numa cópia temporária, a combinação dos dois filtros foi trocada de `AND` para `OR`, preservando os filtros isolados. O teste específico detectou a inclusão indevida de uma cidade de outra UF; os outros 211 casos passaram. O checkout principal conservou a consulta correta.
- **Código e resultados reais:** ajustada a indentação de `UnidadeFederativa` para dois espaços, mantendo UTF-8, mensagens e comportamento. Passaram 158 casos em 17 suítes sem banco e 212 casos em 23 suítes com `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. Não houve alteração de migração ou do módulo anêmico.
- **Arquivos e registros:** `UnidadeFederativa.java`, reorganização e ampliação de `ListaCidadesTest.java`, registro em `src/test/ant/integracao.xml`, README do rico e este plano. Resumo local em `precificacao-rica/target/validacao-etapa10-20261110.json`; relatórios finais em `target/surefire-integracao/20261110-134233232`; verificação negativa em `C:\Java\Workspace\tmp\precificacao-rico-etapa10-20261110\filtros-com-ou`. Usado PostgreSQL 14 descartável; conferidos zero registros de cidades/clientes e zero funções/gatilhos temporários antes de encerrar o servidor.
- **Próxima etapa proposta:** revisão da etapa 11, valores, cadastro e listagem de clientes já implementados. Aguardar ok antes de continuar.

### Etapa 11 — Criar os valores, cadastro e listagem de clientes

**Entrega.** `POST /api/clientes` e `GET /api/clientes`, com telefone e endereço opcionais.

**Implementação.**

1. Criar `Cliente` e `Clientes`. Modelar identificação, contatos e endereço com valores que validem e normalizem CPF, e-mail, telefone e CEP.
2. Endereço referencia uma cidade existente. Preparar todos os valores e conferir os vínculos antes da escrita; construir um objeto de persistência não consulta a cidade automaticamente.
3. Criar a migração de `cliente` já com telefone e colunas de endereço opcionais. Preservar unicidade do CPF e do e-mail sem distinguir capitalização e a integridade do conjunto de endereço.
4. Implementar os objetos `Pg...`, adaptação de resultados e `Tk...`. Devolver os mesmos campos do anêmico, incluindo `telefone: null` e `endereco: null` quando ausentes.

**Aceite.** Cadastrar e listar cliente com e sem endereço. Rejeitar CPF inválido ou repetido, e-mail inválido ou duplicado, telefone inválido, endereço incompleto e cidade inválida ou inexistente. Conferir normalização do CPF e CEP e conservação da capitalização informada no e-mail, aplicando a comparação sem capitalização para unicidade.

**Revisão e parada.** Conferir objetos de valor, tratamento de ausência, restrições, tamanho das leituras e resposta HTTP. Não aceitar qualquer erro de banco como se fosse duplicidade. Pedir ok para a etapa 12.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 12 autorizado pelo usuário.**

- **Domínio e responsabilidades:** revisados `NomeCliente`, `Cpf`, `Email`, `Telefone`, `Cep`, `LocalizacaoEndereco`, `Endereco`, identificação e contato. Mantidas a validação nos valores, a normalização de CPF/CEP, a capitalização do e-mail e a ausência representada por `Optional`. `TextoCliente` continua como apoio interno aos valores; Jackson permanece na borda HTTP. Os construtores não fazem consultas e todos os dados são preparados antes da verificação da cidade e da escrita.
- **Persistência e restrições:** conferidos `PgClientes`, os adaptadores de cadastro/listagem e a migração V8 existente. A inserção usa parâmetros JDBC; CPF e e-mail têm restrições próprias, e o endereço referencia uma cidade real. A classificação já confere SQLState e nome da restrição, distinguindo duplicidade, cidade ausente e indisponibilidade. Essas regras foram preservadas.
- **Entradas inválidas:** ampliado `CadastroClienteTest` para nome acima do limite ou com caractere nulo, complemento acima de 100 caracteres, código de cidade zero, fracionário, fora do intervalo de `long` ou nulo, corpo JSON nulo e campos extras no cliente ou no endereço. Todos exigem `400` antes de abrir conexão. Mantidos os casos existentes de CPF, e-mail, telefone, CEP e endereço incompleto.
- **Limites válidos e opcionais:** novo cenário PostgreSQL cadastra e relê um cliente com nome/logradouro de 200 caracteres, número de 20, bairro/complemento de 100 e e-mail de 254. Confere a remoção dos espaços das pontas, CPF/CEP normalizados, e-mail com capitalização preservada e telefone em branco devolvido como nulo. Os outros cenários também conferem telefone/endereço explicitamente nulos e os valores omitidos já cobertos pelo fluxo existente.
- **Erros que não são de negócio:** dois casos provocam falhas `AFTER INSERT` por outra unicidade e outra referência. Exigem `503` genérico, preservam a listagem anterior e repetem o cadastro com sucesso após retirar o gatilho. Assim, uma falha de outra restrição não vira CPF/e-mail duplicado nem cidade ausente.
- **Vínculo com cidade:** um teste consulta a cidade real e a remove em outra conexão antes de inserir o cliente. Exige `FalhaCliente.CIDADE_AUSENTE` com causa PostgreSQL `23503` e restrição `cliente_cidade_codigo_fkey`, confirma a remoção da cidade e a ausência de novo cliente. A validação prévia e a chave estrangeira cobrem momentos diferentes da operação.
- **Listagem e custo da leitura:** o fluxo já existente de cadastro/listagem foi aproveitado e instrumentado, sem reescrever suas verificações. Mantém os campos, códigos, ordem e valores nulos, com zero consultas durante a construção das rotas e uma conexão/um comando JDBC para montar a listagem. O cenário de limites também confere os dados efetivamente relidos em uma consulta. Os testes existentes de lista vazia e indisponibilidade foram mantidos.
- **Verificação negativa:** apenas numa cópia temporária, retirar a conferência do nome da restrição fez os dois casos de falha detectarem `400` e `404` indevidos no lugar de `503`. Os outros 223 casos passaram; o checkout principal manteve o tratamento específico. A nova suíte `CadastroClientePostgresTest`, com quatro casos, foi incluída no registro obrigatório.
- **Diagnóstico e estilo:** acrescentados logs da operação e SQLState para cadastro/listagem de clientes e consulta da existência da cidade. Na leitura de um registro inválido também é registrada a classe da falha. As causas são preservadas; os logs não incluem nome, CPF, e-mail, telefone, endereço ou credenciais. Ajustados para dois espaços `PgClientes`, `TkCadastroCliente`, `TkListaClientes` e o teste de cadastro alterado, mantendo UTF-8 e mensagens existentes.
- **Resultados e registros reais:** passaram 167 casos em 17 suítes sem banco e 225 casos em 24 suítes com `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. Resumo local em `precificacao-rica/target/validacao-etapa11-20261110.json`; relatórios finais em `target/surefire-integracao/20261110-135502357`; verificação negativa em `C:\Java\Workspace\tmp\precificacao-rico-etapa11-20261110\restricoes-genericas`. Usado PostgreSQL 14 descartável; conferidos zero clientes/cidades e zero funções/gatilhos temporários antes de encerrar o servidor. Não houve alteração de migração ou do módulo anêmico.
- **Próxima etapa proposta:** revisão da etapa 12, consulta de cliente pelo código já implementada. Aguardar ok antes de continuar.

### Etapa 12 — Consultar cliente pelo código

**Entrega.** `GET /api/clientes/{codigo}`, com os mesmos dados retornados pelo cadastro.

**Implementação.** Acrescentar a busca à coleção e aos objetos PostgreSQL, reutilizando a materialização do cadastro e da listagem. Criar o adaptador Takes de consulta e a tradução específica de código inválido, cliente ausente e indisponibilidade.

**Aceite.** Consultar cliente com e sem endereço; código inválido retorna `400`, positivo sem registro retorna `404` e falha de banco retorna `503`. Conferir que listar clientes continua funcionando.

**Revisão e parada.** Conferir parâmetros SQL, campos opcionais e ausência de leituras repetidas para montar uma resposta. Pedir ok para a etapa 13.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 13 autorizado pelo usuário.**

- **Implementação existente preservada:** revisados `TkConsultaCliente`, `Clientes.cliente`, `PgClientes.cliente` e a resposta compartilhada do cliente. O HTTP interpreta o código; a coleção rejeita valores não positivos antes da conexão. O SQL usa `WHERE codigo = ?` e materializa todos os campos em `ClienteConstante`, reutilizando o mesmo mapeamento do cadastro/listagem. Mantidas as respostas `400`, `404` e `503`, sem mudança de regra ou migração.
- **Organização dos testes:** as consultas básicas de cliente com/sem endereço e ausente saíram do fluxo de cadastro e passaram para `ConsultaClienteTest`. Foram mantidas as verificações de outros fluxos existentes. Os dois novos métodos PostgreSQL dessa classe foram registrados como obrigatórios em `src/test/ant/integracao.xml`, mantendo os seis casos sem banco fora da tag de integração.
- **Dados e custo da consulta:** o cenário cadastra dois clientes pela API e compara o GET com as respostas do cadastro, incluindo telefone/endereço nulos e endereço completo com complemento. Cada GET usa uma conexão e um comando JDBC. Uma alteração direta nos dados de teste é observada por uma nova consulta; o objeto materializado antes da alteração conserva e-mail e telefone anteriores.
- **Cliente realmente ausente e listagem:** após remover um dos clientes de teste, seu código positivo passa a retornar `404` com problema completo. Isso verifica a ausência real, sem depender de um identificador presumido livre. A listagem continua respondendo em uma consulta, conserva o outro cliente e deixa de incluir o removido.
- **Registro existente inválido:** outro cenário grava um telefone inválido diretamente na linha de teste, permitida pelo esquema atual, e exige `503` genérico no GET. Confere diretamente no PostgreSQL que a linha continua presente e que a consulta não modificou telefone ou e-mail. O cenário distingue falha de leitura/materialização de cliente inexistente e evita devolver dados internos no problema.
- **Código inválido e diagnóstico:** mantidos os casos de código zero, negativo, texto, fracionário e fora do intervalo de `long`, rejeitados antes do banco, e o teste de indisponibilidade. Acrescentado log de consulta com operação, classe da falha e SQLState, preservando a causa e sem registrar dados pessoais ou credenciais. `TkConsultaCliente` e o teste alterado passaram a usar dois espaços e mantiveram UTF-8 e mensagens existentes.
- **Verificação negativa:** somente numa cópia temporária, falhas técnicas da consulta foram classificadas como cliente ausente. O teste sem banco de indisponibilidade e o cenário PostgreSQL de registro inválido detectaram `404` indevido no lugar de `503`. Os outros 225 casos passaram; o checkout principal manteve a classificação correta.
- **Resultados e registros reais:** passaram 167 casos em 17 suítes sem banco e 227 casos em 24 suítes com `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. Arquivos: log em `PgClientes.java`, estilo em `TkConsultaCliente.java`, reorganização de `CadastroClienteTest.java`/`ConsultaClienteTest.java`, registro obrigatório e documentação. Resumo local em `precificacao-rica/target/validacao-etapa12-20261110.json`; relatórios finais em `target/surefire-integracao/20261110-160212363`; verificação negativa em `C:\Java\Workspace\tmp\precificacao-rico-etapa12-20261110\falha-como-ausencia`. Usado PostgreSQL 14 descartável; conferidos zero clientes/cidades e zero funções/gatilhos temporários antes de encerrar o servidor. O módulo anêmico não foi alterado.
- **Próxima etapa proposta:** revisão da etapa 13, atualização de e-mail do cliente já implementada. Aguardar ok antes de continuar.

### Etapa 13 — Atualizar o e-mail do cliente

**Entrega.** `PATCH /api/clientes/{codigo}` para atualização de e-mail.

**Implementação.**

1. Representar a presença do campo na borda. Preparar uma representação de intenção que permita estender o PATCH nas etapas seguintes: preservar, alterar e remover são situações diferentes. Um `Optional.empty()` isolado não distingue omissão de remoção.
2. Reutilizar o objeto de e-mail do cadastro e a operação de alteração do cliente. Campos nome e CPF continuam fora do PATCH.
3. Ler e alterar com proteção contra concorrência, gravando somente o e-mail solicitado. Usar a restrição única para resolver disputas de unicidade e traduzir somente esse conflito em `400`.
4. Retornar os dados atualizados e preparar a extensão futura para telefone e endereço sem setters no domínio ou dependência de Jackson nos objetos de negócio.

**Aceite.** Alterar e consultar o e-mail, mantendo nome, CPF, telefone e endereço. Rejeitar e-mail nulo, inválido, de outro cliente com capitalização diferente, corpo vazio e campo desconhecido. Um erro mantém o registro anterior; cliente ausente retorna `404`.

**Revisão e parada.** Conferir validação compartilhada, presença do campo, SQL restrito ao solicitado e falha real de persistência com `503`. Pedir ok para a etapa 14.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço para a etapa 14 autorizado pelo usuário.**

- **Implementação preservada:** revisados `Email`, `AlteracaoContatos`, a interpretação de presença em `TkAlteracaoCliente` e a transação de `PgClientes.alterarContatos`. O e-mail continua usando a mesma validação do cadastro. O HTTP prepara a intenção; o SQL monta as atribuições somente com os campos informados e vincula os valores por parâmetros. `UPDATE ... RETURNING` obtém a proteção da linha e o estado atualizado na mesma instrução; a confirmação ocorre depois da materialização do cliente.
- **Reorganização dos testes:** as verificações de alteração de e-mail e de reversão que estavam no fluxo de cadastro passaram para `AlteracaoEmailPostgresTest`. O teste de cadastro conserva suas regras próprias. A nova suíte, com três casos de integração, foi registrada como obrigatória. Os testes existentes de telefone foram mantidos; sua revisão continua na etapa 14.
- **Dados e colunas preservadas:** o cenário usa cliente com telefone/endereço completos e confere o mesmo código, nome, CPF e demais campos após alterar só o e-mail. Um gatilho temporário específico das outras colunas rejeita qualquer tentativa de incluí-las no `SET`, mesmo com valores iguais. Cada PATCH do cenário utiliza uma conexão; o teste não exige uma quantidade fixa de comandos SQL.
- **E-mail e conflitos:** aceito e-mail de 254 caracteres com apóstrofo e espaços nas extremidades, preservando a capitalização após remover os espaços. Alterar somente a capitalização do próprio e-mail é permitido. E-mail de outro cliente, inclusive com outra capitalização, retorna `400` e preserva os dois cadastros. Remover o cliente de teste e usar seu código positivo exige `404` sem alterar o outro cliente.
- **Falha de outra restrição:** um gatilho `AFTER UPDATE` provoca `23505` com outro nome de restrição. O PATCH exige `503` genérico e conserva os dados anteriores. Após retirar o gatilho, a mesma alteração é repetida com sucesso. A comparação específica com `ux_cliente_email` foi mantida.
- **Desfazimento depois da escrita:** o cenário prepara um telefone inválido no banco e tenta alterar o e-mail. Um marcador `AFTER UPDATE` avança uma sequência somente quando a linha já contém o e-mail solicitado. O avanço, observável após a reversão, comprova que o UPDATE foi alcançado antes da falha na materialização do cliente. Exigidos `503`, e-mail anterior preservado e telefone sem alteração. Depois de restaurar o dado de teste, a mesma operação é repetida com sucesso e o marcador avança novamente.
- **Verificação negativa:** somente numa cópia temporária, foram retirados o início e a confirmação explícitos da transação de contatos, deixando a escrita em autocommit. O teste recebeu `503`, mas detectou o e-mail novo indevidamente gravado no lugar do anterior. Os outros 231 casos passaram. A transação do checkout principal permaneceu intacta.
- **Entradas e diagnóstico:** acrescentados dois casos sem banco para e-mail acima de 254 caracteres e JSON com conteúdo após o objeto. Mantidas as rejeições de nulo, inválido, tipo incorreto, corpo vazio e campo desconhecido antes da conexão. Acrescentados logs de falha de alteração, materialização e reversão, preservando a causa e as exceções suprimidas, sem registrar contatos, dados pessoais ou credenciais. Ajustados para dois espaços `Email`, `AlteracaoContatos`, `TkAlteracaoCliente` e o teste de alteração, mantendo UTF-8 e mensagens existentes.
- **Resultados reais:** passaram 169 casos em 17 suítes sem banco e 232 casos em 25 suítes com `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. Não houve alteração de migração, regra de negócio ou do módulo anêmico. Usado PostgreSQL 14 descartável; conferidos zero clientes/cidades, funções, gatilhos e sequências temporárias da etapa antes de encerrar o servidor.
- **Arquivos e registros:** logs em `PgClientes.java`; estilo nos valores/adaptador; reorganização de `CadastroClienteTest.java` e ampliação de `AlteracaoClienteTest.java`; novo `AlteracaoEmailPostgresTest.java`; registro obrigatório e documentação. Resumo local em `precificacao-rica/target/validacao-etapa13-20261110.json`; relatórios finais em `target/surefire-integracao/20261110-161544958`; verificação negativa em `C:\Java\Workspace\tmp\precificacao-rico-etapa13-20261110\sem-transacao`.
- **Próxima etapa proposta:** revisão da etapa 14, atualização/remoção de telefone e alteração conjunta dos contatos. Aguardar ok antes de continuar.

### Etapa 14 — Atualizar ou remover telefone

**Entrega.** Extensão do PATCH para `telefone`, isolado ou junto com e-mail.

**Implementação.** Reutilizar a validação do telefone do cadastro, interpretar `null` como remoção e omissão como preservação. Preparar os valores informados antes de gravar e incluir somente as colunas solicitadas na atualização.

**Aceite.** Alterar, remover e omitir telefone; atualizar e-mail e telefone juntos. Telefone inválido impede também a gravação do e-mail. Confirmar no banco a sobreposição de duas requisições, uma para cada contato, e verificar que nenhuma alteração se perde.

**Revisão e parada.** Conferir ausência de sobrescrita com valores de uma leitura antiga, atomicidade dos campos enviados juntos e controle da concorrência no teste. Pedir ok para a etapa 15.

**Encerramento da revisão — 10 de novembro de 2026. Status: concluída; avanço à etapa 15 autorizado posteriormente.**

- **Implementação preservada:** revisados `Telefone`, `AlteracaoCampo`, `AlteracaoContatos`, `TkAlteracaoCliente` e o SQL dos contatos. Mantidas a validação compartilhada com o cadastro, a remoção dos espaços das pontas e as três intenções: omitir conserva, nulo remove e texto válido substitui. A alteração prepara os dois valores antes de acessar o banco e inclui no `SET` somente os campos informados. Não houve alteração de regra ou migração.
- **Organização dos testes:** as verificações PostgreSQL saíram de `AlteracaoClienteTest` e passaram para `AlteracaoTelefonePostgresTest`, com cinco casos. O primeiro arquivo ficou responsável pelos casos sem banco. A nova suíte foi registrada como obrigatória, e o registro do método antigo foi retirado. `Telefone` e `AlteracaoCampo` passaram a usar dois espaços, mantendo UTF-8 e mensagens existentes.
- **Preservação e nulo real:** o cenário usa cliente com endereço completo e compara toda a resposta após trocar telefone, alterar só e-mail, remover telefone e enviar ambos os contatos. Cada alteração é conferida por um novo GET, preservando código, nome, CPF e endereço. A omissão é exercitada com telefone ainda preenchido; a remoção exige `null` também na leitura do banco. Os PATCHes sequenciais usam uma conexão por operação.
- **Validação conjunta:** dois casos com `DataSourceSemConexao` rejeitam e-mail válido com telefone inválido e a combinação inversa. Exigem `400` sem qualquer conexão, evitando uma escrita do primeiro campo antes de validar o segundo. A antiga verificação dessa entrada inválida com banco foi substituída por essa exigência mais direta.
- **Conflito de unicidade:** enviar novo telefone junto de e-mail já ocupado retorna `400` e conserva os dados completos dos dois clientes, incluindo o telefone original do cliente alterado. O teste usa a restrição real do PostgreSQL.
- **Falha após gravar ambos:** gatilhos temporários deixam o CPF inválido na linha retornada pelo UPDATE e avançam um marcador somente depois de os dois contatos novos estarem presentes. A materialização do cliente falha e o PATCH exige `503`; o marcador comprova a escrita alcançada, e o GET deve retornar todo o cadastro original. Após retirar a falha, a mesma alteração é repetida com sucesso, e o marcador avança novamente. A sequência, os gatilhos e a função são removidos ao terminar.
- **Concorrência observada:** uma sessão independente bloqueia a linha do cliente. As duas requisições são iniciadas, e o teste acompanha `pg_blocking_pids` recursivamente até comprovar que ambas aguardam essa sessão, direta ou indiretamente. A verificação substitui a comparação com o texto do SQL. Após liberar o bloqueio, confere diretamente os dois contatos gravados e o cadastro completo, sem presumir qual requisição termina primeiro. Exercitados telefone substituído e telefone removido junto de alteração concorrente do e-mail.
- **Verificação negativa isolada:** numa cópia temporária, o método passou a ler os valores antigos sem bloqueio e a gravar sempre os dois contatos. Foram selecionados somente os dois casos concorrentes nessa experiência; ambos detectaram perda real nos valores do PostgreSQL: telefone antigo em um caso e e-mail antigo no outro. Para essa seleção, a tag foi habilitada apenas no POM da cópia. O perfil obrigatório e o POM do checkout principal foram preservados; a validação principal continuou sendo a suíte completa.
- **Resultados e registros reais:** passaram 171 casos em 17 suítes sem banco e 238 casos em 26 suítes com `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`, sem falhas, erros ou ignorados. A nova suíte tem três casos individuais e duas variações de concorrência. Resumo local em `precificacao-rica/target/validacao-etapa14-20261110.json`; relatórios finais em `target/surefire-integracao/20261110-163249224`; experiência negativa em `C:\Java\Workspace\tmp\precificacao-rico-etapa14-20261110\sobrescrita-contatos`. Usado PostgreSQL 14 descartável; conferidos zero clientes/cidades, funções, gatilhos e sequências temporárias antes de encerrar o servidor. O módulo anêmico não foi alterado.
- **Próxima etapa proposta naquele encerramento:** implementar a etapa 15, substituição/remoção do endereço pelo PATCH do cliente. O usuário autorizou o avanço; a entrega está registrada abaixo.

### Etapa 15 — Substituir ou remover endereço

**Entrega.** Extensão do PATCH para `endereco`, isolado ou junto com os contatos.

**Implementação.** Reutilizar o objeto de endereço do cadastro e a coleção de cidades. Endereço omitido preserva; objeto recebido substitui o conjunto; `null` remove todas as suas colunas. Validar cidade e campos obrigatórios antes de escrever. A alteração deve preservar a identidade do cliente.

**Aceite.** Trocar o endereço e sua cidade, remover o endereço, alterar somente contatos mantendo endereço e rejeitar endereço parcial ou cidade inexistente sem modificar os demais campos enviados. A preservação do vínculo com fatura será exercitada depois da etapa 17, no fluxo integrado final.

**Revisão e parada.** Conferir escrita e remoção das seis colunas do endereço em conjunto, presença explícita do campo e mesma conexão na operação composta. Pedir ok para a etapa 16.

**Encerramento da implementação — 10 de novembro de 2026. Status: concluída; avanço à etapa 16 autorizado posteriormente.**

- **Contrato:** `PATCH /api/clientes/{codigo}` aceita `endereco` omitido para preservar, objeto completo para substituir e `null` para remover. Complemento omitido na substituição passa a nulo. Nome, CPF e código são preservados. A mesma requisição pode alterar e-mail e telefone.
- **Modelo e HTTP:** `AlteracaoCliente` substitui a antiga intenção limitada aos contatos e compõe `AlteracaoCampo<Endereco>`, mantendo a distinção entre omitir, remover e substituir. `EnderecoJson` é compartilhado por cadastro e alteração; os objetos de domínio continuam validando CEP, localização e código da cidade. Campos extras, tipos incompatíveis e endereço parcial são rejeitados antes da conexão. Não foram acrescentados serviços ou dependências de Jackson ao domínio.
- **Persistência:** a alteração inclui no SQL somente os campos enviados. As seis colunas do endereço são vinculadas em conjunto, com parâmetros tipados, no mesmo UPDATE dos contatos. `PgCidades.existe(Connection, CodigoCidade)` permite verificar a referência na conexão da própria transação, sem fechar a conexão recebida. O retorno é materializado antes do commit; falhas desfazem toda a operação. A chave estrangeira continua protegendo contra remoção da cidade depois da verificação. Nenhuma migração nova foi necessária: a V8 já contém as colunas e restrições.
- **Erros:** cidade ausente, inclusive durante a escrita, retorna `404`; e-mail duplicado retorna `400`; outra chave estrangeira e falha ao materializar o retorno permanecem `503`, com mensagem genérica. Logs preservam o diagnóstico sem dados pessoais.
- **Testes relevantes:** acrescentados onze casos sem banco para formato/endereço parcial, CEP e códigos de cidade inválidos. Os seis casos PostgreSQL cobrem substituição com troca de cidade, normalização, retirada do complemento, omissão, remoção das seis colunas, recriação e alterações conjuntas. Conferem também cliente ausente, duplicidade e cidade inexistente sem alterações parciais. Dois cenários provocam falha de materialização após UPDATE, com marcador que comprova a escrita, exigindo rollback de endereço e contatos, tanto na substituição quanto na remoção. Outros dois exercitam a chave estrangeira real após retirar a nova cidade em um gatilho e uma restrição desconhecida. Os cenários de sucesso e falha conferem uma única conexão por PATCH. Os testes anteriores de concorrência dos contatos e preservação dos campos omitidos continuaram passando.
- **Validação real:** passaram 182 casos em 17 suítes sem banco e 255 casos em 27 suítes na execução completa com PostgreSQL 14 descartável, sem falhas, erros ou ignorados. A nova suíte foi registrada como obrigatória no perfil de integração. Relatórios em `precificacao-rica/target/surefire-integracao/20261110-164439562`; resumo em `precificacao-rica/target/validacao-etapa15-20261110.json`. Conferidos zero clientes/cidades, funções, gatilhos e sequências temporárias após a execução. O módulo anêmico não foi alterado nesta etapa.
- **Próxima etapa naquele encerramento:** completar os objetos de cálculo da fatura, conforme a etapa 16. O usuário autorizou o avanço; a entrega está registrada abaixo. A preservação do vínculo cliente–fatura continua prevista para o fluxo integrado após a etapa 17.

### Etapa 16 — Completar os objetos de cálculo da fatura

**Entrega.** Implementação do comportamento que hoje está apenas nos contratos `Fatura` e `Faturas`, com cálculo reutilizável por consulta e simulação.

**Implementação.**

1. Evoluir os contratos existentes para contemplar valores calculados, cliente e pagamento opcionais e os próximos casos de uso. Manter faturas no mesmo nível de produtos.
2. Criar um objeto de cálculo ou de valores da fatura, composto por itens e desconto. Uma simulação não deve precisar de código fictício ou data inventada para satisfazer o contrato de uma entidade persistida.
3. Reutilizar `ItemFatura`, `Quantidade` e os objetos de desconto. Acrescentar o limite de 15 dígitos inteiros à quantidade e validar lista não vazia e produtos distintos.
4. Somar valores exatos, calcular o abatimento e arredondar somente o total final. A limitação do desconto fixo após redução de preço preserva seu original. Rejeitar fixo maior que subtotal na preparação do cadastro e da simulação.
5. Definir a representação do desconto necessária para gravação e reconstituição, mantendo seu comportamento polimórfico. Preservar a distinção entre ausência de desconto e um desconto de valor zero.

**Aceite.** Exercitar quantidades e descontos nos limites, ausência, valores inválidos, produtos repetidos e resultados com total zero. Conferir, por exemplo, `1.23 × 0.1234 = 0.151782`; desconto de `12.3456%` resulta em `0.018738398592` e total `0.13`. Somar também os itens `1.01 × 0.5` e `1.05 × 0.5`, cujo total correto é `1.03`, sem arredondar cada item.

**Revisão e parada.** Conferir cálculo fora de HTTP e SQL, reutilização dos objetos existentes, precisão e ausência de I/O na construção. Os testes devem detectar o arredondamento antecipado. Pedir ok para a etapa 17.

**Encerramento da implementação — 10 de novembro de 2026. Status: concluída; avanço à etapa 17 autorizado posteriormente.**

- **Composição:** criado `CalculoFatura`, que valida lista não vazia, itens presentes e produtos distintos sem consultar o banco. A lista recebida é copiada e não pode ser alterada pelos acessores. `Quantidade` agora também limita o valor a `999999999999999.9999`, incluindo valores em notação científica e zeros excedentes.
- **Cálculo e leitura:** `ItemFatura.valores()` materializa preço e quantidade em `ValoresItemFatura`. `ValoresFatura` soma os subtotais exatos, delega o abatimento ao desconto e arredonda somente o total para duas casas com o critério existente. Acessar o resultado não volta à coleção de produtos; uma nova chamada de cálculo consulta os produtos novamente. O resultado é utilizável para simulação sem código ou data fictícios.
- **Preparação e consulta:** `preparar()` valida o desconto inicial contra o subtotal exato; `calcular()` aplica os preços observados e permite a limitação do desconto fixo em consultas posteriores. A validação adicional pertence a `DescontoFixo`, pelo contrato polimórfico, sem teste de classe no cálculo. O desconto contratado não é modificado quando o preço cai ou volta a subir.
- **Representação:** `DescontoContratado` guarda tipo e valor original validados pela estratégia; `TipoDesconto` reconstitui o comportamento correspondente. `registro()` e `Desconto.reconstituir()` preservam a diferença entre ausência de desconto, percentual zero e fixo zero. Nenhuma fórmula foi transferida para um serviço ou para SQL/HTTP.
- **Contratos:** `Fatura` compõe o cálculo e passa a expor valores e vínculos; `Faturas` recebe cálculo e vínculos no cadastro. `VinculosFatura` valida código positivo e aceita cliente/pagamento ausentes. `FormaPagamento` contém PIX, dinheiro, cartão de débito, cartão de crédito e boleto. Falha de produto ausente tem classificação própria em `FalhaFatura`; falhas de leitura preservam causa e classificação de infraestrutura.
- **Testes com propósito:** os exemplos `1.23 × 0.1234`, percentual de `12.3456%` e soma de `1.01 × 0.5 + 1.05 × 0.5` conferem resultados exatos antes do total. Dois empates verificam arredondamento para par. Outros casos cobrem total zero, limite de quantidade, repetição de produto, composição inválida antes de I/O, desconto inicial excedente, retomada do fixo original, falhas de produto, listas protegidas e resultado anterior preservado após mudança do preço. A representação dos descontos é reconstituída e exercitada, inclusive ausência e zeros. A nova suíte `CalculoFaturaTest` foi incluída no registro obrigatório.
- **Validação real:** passaram 206 casos em 18 suítes sem banco e 279 casos em 28 suítes no perfil completo com PostgreSQL 14 descartável, sem falhas, erros ou ignorados. Relatórios em `precificacao-rica/target/surefire-integracao/20261110-165309550`; resumo em `precificacao-rica/target/validacao-etapa16-20261110.json`. As migrações V1 a V10 foram preservadas. O módulo anêmico não foi alterado nesta etapa.
- **Próxima etapa naquele encerramento:** implementar o cadastro e a persistência da fatura/itens conforme a etapa 17. O usuário autorizou o avanço; a entrega está registrada abaixo. A leitura coerente continua sendo um requisito do fluxo de consulta da etapa 18.

### Etapa 17 — Cadastrar fatura e persistir seus itens

**Entrega.** `POST /api/faturas`, incluindo desconto, cliente e forma de pagamento opcionais.

**Implementação.**

1. Reutilizar as tabelas `fatura` e `item_fatura` das migrações V5, V9 e V10, que já contêm data gerada, desconto original, vínculos opcionais, quantidades e posição dos itens. Conferir as restrições reais antes de acrescentar outra migração; não recriar as tabelas.
2. Implementar `PgFatura`, `PgFaturas` e os adaptadores necessários, compondo os objetos de cálculo e as coleções de produtos e clientes.
3. Validar todos os itens, referências, desconto e pagamento antes da escrita. Diferenciar dado inválido, produto ou cliente ausente e falha de infraestrutura.
4. Gravar cabeçalho e itens na mesma conexão e transação. Nenhuma falha pode deixar uma fatura sem seus itens ou somente parte deles.
5. Criar o adaptador Takes, usando os mesmos campos de entrada e saída do anêmico. Omitir `codigoCliente` e `formaPagamento` na resposta quando ausentes.

**Aceite.** Criar fatura com e sem opcionais, com vários itens e quantidades fracionárias, com os dois tipos de desconto e cada forma de pagamento. Rejeitar referências ausentes, códigos fracionários, quantidade inválida e pagamento desconhecido. Provocar falha na gravação de um item e confirmar o desfazimento do cabeçalho e dos demais itens.

**Revisão e parada.** Conferir a conexão compartilhada, a persistência do desconto original e a ausência de um preço unitário congelado no item. A fronteira HTTP não contém as fórmulas da fatura. Pedir ok para a etapa 18.

**Encerramento da implementação — 10 de novembro de 2026. Status: concluída; avanço à etapa 18 autorizado posteriormente.**

- **HTTP:** exposto `POST /api/faturas` por `TkCadastroFatura`, com os mesmos campos do anêmico: itens, desconto, código do cliente e forma de pagamento. Códigos são convertidos sem truncar frações ou exceder `long`; objetos de domínio validam composição, quantidades, descontos e vínculos. Corpo inválido, campos extras e pagamento desconhecido são rejeitados antes da conexão. A resposta `201` preserva a ordem dos itens e omite cliente/pagamento quando ausentes. As fórmulas permanecem no domínio.
- **Transação:** `PgFaturas` valida cliente e carrega os produtos em conjunto, usando a conexão da gravação. A referência do cliente recebe proteção de chave; os produtos são lidos com bloqueio compartilhado até o fim da operação. Só depois de preparar o cálculo são inseridos o cabeçalho e o lote de itens. O retorno é lido e validado antes do commit. Falha de SQL, de negócio ou na materialização desfaz a operação inteira, preservando a causa e registrando falhas de infraestrutura sem dados pessoais.
- **Composição e leitura:** `ItemFatura` mantém a construção anterior por coleção e passa a aceitar também um produto já materializado, sem criar uma coleção falsa nem abrir outra conexão. `PgProdutosConstantes` reutiliza seu adaptador de linha na leitura em lote. `FaturaConstante` compõe itens e desconto da leitura, e `ConsultaSqlDeFaturas` carrega cabeçalho/itens/produtos em um único SELECT. Essa leitura atende o retorno do cadastro e os contratos Java existentes de `PgFatura`/`PgFaturas`, sem métodos não suportados. A rota GET, os cenários de mudança de preço e os filtros HTTP permanecem nas próximas etapas.
- **Persistência:** reutilizadas V5, V9 e V10, sem alteração de migração aplicada. Gravados tipo e valor original do desconto, vínculos opcionais, quantidades e posições. Nenhum preço unitário é salvo em `item_fatura`. A consulta usa os preços observados no produto, e os acessores da fatura devolvida não executam novas consultas.
- **Classificação de erros:** produto ou cliente ausente retorna `404`; entrada inválida e desconto fixo acima do subtotal inicial retornam `400`; falha de conexão, outra restrição e erro ao montar o retorno retornam `503` genérico. A análise de falhas do lote percorre as exceções JDBC encadeadas e confere os nomes exatos das chaves estrangeiras, sem classificar qualquer `23503` como referência ausente.
- **Testes sem banco:** 24 entradas inválidas cobrem composição, códigos fracionários/fora do limite, quantidades, descontos, pagamento e campos extras. Outros casos verificam tipo de conteúdo, método HTTP, indisponibilidade e construção sem I/O. A suíte nova tem 27 casos sem banco.
- **Testes PostgreSQL:** 18 casos conferem as cinco formas de pagamento, opcionais omitidos/nulos, desconto zero com tipo preservado, desconto fixo integral, dados gravados, ordem dos itens e precisão. Um gatilho impede INSERT de cabeçalho para comprovar que referência ausente e desconto inicial excedente são rejeitados antes da escrita. Falhas após o segundo INSERT avançam um marcador duas vezes e exigem ausência de cabeçalho/itens, incluindo uma chave estrangeira desconhecida. Também são exercitadas as chaves estrangeiras reais após remover cliente ou produto em gatilho, e uma falha de materialização após os itens serem escritos. Todos esses cenários permitem repetir a operação com sucesso após retirar a falha.
- **Conexão e custo:** cada POST usa uma única conexão, inclusive para validar referências e montar o retorno. Os cenários com dois itens exigem cinco chamadas de execução JDBC com cliente e quatro sem cliente: validação opcional, leitura em conjunto, INSERT do cabeçalho, execução do lote de itens e leitura do retorno. O lote contém um INSERT por item; o número de consultas de validação não cresce por produto.
- **Validação real:** passaram 233 casos em 19 suítes sem banco e 324 casos em 30 suítes no perfil completo com PostgreSQL 14 descartável, sem falhas, erros ou ignorados. As duas suítes novas foram registradas como obrigatórias. Relatórios em `precificacao-rica/target/surefire-integracao/20261110-170436118`; resumo em `precificacao-rica/target/validacao-etapa17-20261110.json`. Conferidos zero clientes/cidades, faturas/itens, funções, gatilhos e sequências temporárias ao finalizar. O módulo anêmico não foi alterado nesta etapa.
- **Próxima etapa naquele encerramento:** expor e validar `GET /api/faturas/{codigo}`, aproveitando a base SQL existente. O usuário autorizou o avanço; a entrega está registrada abaixo.

### Etapa 18 — Consultar fatura com preços atuais

**Entrega.** `GET /api/faturas/{codigo}`, com itens, preços atuais, subtotal, desconto aplicado, total e vínculos opcionais.

**Implementação.**

1. Reutilizar e revisar a base de leitura da etapa 17 (`PgFaturas.fatura`, `PgFatura` e `ConsultaSqlDeFaturas`) para expor o GET. Preservar a consulta conjunta de cabeçalho, itens e produtos, de modo que os campos da resposta correspondam ao mesmo estado observado.
2. Materializar os valores dessa consulta para a resposta. Chamar `produto()`, `subtotal()` e `total()` separadamente não pode buscar preços diferentes no meio da montagem do mesmo JSON.
3. Preservar a consulta dinâmica entre operações: uma nova requisição carrega novamente os preços. Um estado constante pode durar uma leitura, sem virar um cache global de preços.
4. Aplicar o desconto preservando o valor original e devolver os campos esperados. A consulta não escreve no banco.

**Aceite.** Criar fatura, alterar o preço do produto e consultar novamente. No desconto fixo, conferir redução do subtotal e recuperação do valor contratado após aumento de preço. Verificar custo, precisão, total zero, fatura ausente, código inválido e consistência dos campos de uma mesma resposta.

**Revisão e parada.** Conferir o alcance da leitura, o fechamento dos recursos e a inexistência de consultas repetidas por atributo. Pedir ok para a etapa 19.

**Encerramento da implementação — 10 de novembro de 2026. Status: concluída; avanço à etapa 19 autorizado posteriormente.**

- **HTTP e compartilhamento:** criado `TkConsultaFatura` e registrada a rota `GET /api/faturas/{codigo}`. `RespostasFatura` passa a ser usada tanto pelo POST (`201`) quanto pelo GET (`200`), preservando campos, ordem dos itens, data em ISO e omissão dos vínculos ausentes. A adaptação HTTP obtém um único `ValoresFatura` e não contém fórmulas ou SQL.
- **Leitura revisada:** preservados `PgFaturas.fatura`, `PgFatura.materializada` e o SELECT conjunto de `ConsultaSqlDeFaturas`. Cabeçalho, itens e preços são materializados antes do retorno. Uma nova requisição lê o estado atual; os campos de uma resposta usam os mesmos valores observados. Recursos JDBC permanecem fechados pela operação. Nenhuma alteração de esquema ou migração foi necessária.
- **Erros:** códigos zero, negativos, não numéricos, fracionários ou fora de `long` retornam `400` antes da conexão. Fatura removida retorna `404`. Banco indisponível e fatura armazenada sem itens retornam `503` genérico, mantendo os dados. A rota por código aceita somente GET e responde `405` com `Allow: GET` para outro método.
- **Testes de preços e precisão:** sete casos PostgreSQL conferem o GET com e sem vínculos; os valores exatos do item `1.23 × 0.1234` e do desconto `12.3456%`; o total `1.03` da soma de itens fracionários; fatura ausente e registro sem itens. O desconto fixo de `30.00` é limitado quando o subtotal cai de `100.00` para `12.34` e volta a `30.00` ao subir para `50.00`, com total `20.00`. A leitura anterior conserva seus valores, e o banco mantém o desconto contratado.
- **Consistência observável:** um adaptador de teste altera os dois preços no PostgreSQL depois de materializar a fatura e antes da serialização. A primeira resposta precisa permanecer igual à leitura anterior por inteiro; o GET seguinte precisa mostrar os novos preços e totais. Cada GET dos cenários executa uma única consulta em uma única conexão, incluindo respostas `404` e `503` após leitura.
- **Somente leitura:** os GETs de integração usam uma fonte de conexões com `default_transaction_read_only=on`, conferido por `SHOW transaction_read_only`. Gravações em tabelas persistentes seriam rejeitadas pelo servidor. O cenário de fatura sem itens confirma que o GET mantém o cabeçalho e não recria itens; após restaurar os dados de teste, a consulta volta a funcionar.
- **Organização dos testes:** sete novos casos sem banco tratam código, indisponibilidade e método HTTP. Extraído o cenário de montagem/limpeza do cadastro para `CenarioFatura`, reutilizado nas duas suítes; a limpeza inclui os históricos gerados pelo PATCH de preço. As comparações numéricas de JSON preservam o valor esperado e removem zeros excedentes, tratando `30` e `30.0` como equivalentes. As duas suítes de consulta foram registradas como obrigatórias no perfil completo.
- **Validação real:** passaram 240 casos em 20 suítes sem banco e 338 casos em 32 suítes no perfil completo com PostgreSQL 14 descartável, sem falhas, erros ou ignorados. Os testes anteriores do cadastro continuaram passando após a extração do cenário. Relatórios finais em `precificacao-rica/target/surefire-integracao/20261110-171643809`; resumo em `precificacao-rica/target/validacao-etapa18-20261110.json`. Conferidos zero clientes/cidades, faturas/itens, históricos e objetos temporários ao terminar. O módulo anêmico foi preservado nesta etapa.
- **Próxima etapa naquele encerramento:** implementar `POST /api/faturas/simulacao` sem gravar dados. O usuário autorizou o avanço; a entrega está registrada abaixo.

### Etapa 19 — Simular fatura sem gravar

**Entrega.** `POST /api/faturas/simulacao`, com o mesmo corpo do cadastro e resposta de valores sem identidade de fatura.

**Implementação.** Reutilizar a preparação e o objeto de cálculo do cadastro, validando também cliente e pagamento quando informados. Retornar `itens`, `subtotal`, `descontoAplicado` e `total`. A operação usa somente leituras, sem chamar o cadastro para depois desfazê-lo.

**Aceite.** Conferir valores esperados independentes e sua equivalência com o cadastro do mesmo corpo. Contagens e dados de `fatura` e `item_fatura` permanecem inalterados. Rejeitar as mesmas entradas inválidas e preservar a precisão dos itens e descontos.

**Revisão e parada.** Conferir que nenhuma fórmula foi copiada para o `Tk...`, que as referências são validadas e que não há INSERT nem geração de código. Registrar a rota literal `simulacao` sem confundi-la com a consulta por código. Pedir ok para a etapa 20.

**Encerramento da implementação — 10 de novembro de 2026. Status: concluída; avanço à etapa 20 autorizado posteriormente.**

- **HTTP e contrato compartilhado:** exposto `POST /api/faturas/simulacao` por `TkSimulacaoFatura`. O cadastro e a simulação passam a ler `PedidoFatura`, com o mesmo mapper estrito e a mesma conversão para cálculo e vínculos. A resposta `200` contém somente itens, subtotal, desconto aplicado e total, sem identidade, data, cliente ou pagamento. Cliente e pagamento informados continuam sendo validados. A rota literal foi registrada antes da rota genérica por código; GET na rota de simulação retorna `405` e `Allow: POST`.
- **Preparação comum:** acrescentado `Faturas.simular`, com implementação em `PgFaturas` e evolução do adaptador de teste que implementa a coleção. Os dois fluxos resolvem referências e materializam os produtos pela mesma preparação, depois chamam `CalculoFatura.preparar()`. A validação do desconto fixo inicial continua no domínio, assim como todas as fórmulas. Não foi criada entidade com código ou data fictícios.
- **Somente leitura:** a simulação abre uma transação marcada como somente leitura e com leitura repetível. Validação do cliente e leitura dos produtos usam essa conexão. Acrescentadas leituras sem bloqueio de linha em `PgClientes` e `PgProdutosConstantes`, reutilizando seus respectivos SQLs e adaptadores. O cadastro preserva os bloqueios já usados para proteger suas referências. A simulação não chama o cadastro, não faz INSERT/UPDATE/DELETE e não solicita código.
- **Validação sem banco:** os 24 corpos inválidos existentes agora são exercitados contra os dois POSTs, com a mesma lista de dados, verificando `400` e o caminho do problema antes da conexão. Tipo de conteúdo e indisponibilidade também são conferidos nos dois fluxos. Um novo caso garante a prioridade da rota literal da simulação. A suíte padrão tem 241 casos; repetir os pedidos nos dois caminhos não duplicou a massa de testes.
- **Valores independentes e equivalência:** 12 casos PostgreSQL cobrem ausência de desconto, percentual fracionário, desconto fixo, desconto integral/total zero, opcionais omitidos/nulos, referências ausentes e fixo inicial excedente. Os valores esperados são escritos explicitamente e também comparados com os quatro campos de valores do cadastro do mesmo corpo. O cenário `1.23 × 0.1234` preserva subtotal `0.151782`, desconto `0.018738398592` e total `0.13`. Uma nova simulação observa o preço alterado, mantendo o resultado anterior e a fatura já existente.
- **Prova de ausência de gravação:** os testes usam conexões com somente leitura conferida no PostgreSQL. Antes e depois da simulação, comparam todos os dados de `fatura` e `item_fatura`, além de `last_value` e `is_called` da sequência de códigos. Os cenários já têm faturas gravadas antes da comparação, incluindo pedidos rejeitados. Cada simulação usa uma conexão, uma consulta de produtos e, quando há cliente, uma consulta adicional da referência.
- **Validação real:** passaram 241 casos em 20 suítes sem banco e 351 casos em 33 suítes no perfil completo com PostgreSQL 14 descartável, sem falhas, erros ou ignorados. A nova suíte foi registrada como obrigatória. Relatórios em `precificacao-rica/target/surefire-integracao/20261110-173358622`; resumo em `precificacao-rica/target/validacao-etapa19-20261110.json`. Conferidos zero clientes/cidades, faturas/itens, históricos e objetos temporários ao terminar. Não houve migração nova nem alteração do modelo anêmico.
- **Próxima etapa naquele encerramento:** implementar a listagem resumida com filtros por período e cliente. O usuário autorizou o avanço; a entrega está registrada abaixo.

### Etapa 20 — Listar faturas por período e cliente

**Entrega.** `GET /api/faturas`, com `inicio`, `fim` e `codigoCliente` opcionais e combináveis.

**Implementação.**

1. Evoluir a coleção para a consulta filtrada e criar o adaptador HTTP dos parâmetros.
2. Validar códigos e datas, incluindo intervalo invertido. Combinar as condições com `AND` e ordenar por data e código decrescentes.
3. Devolver o resumo com `codigo`, `dataCriacao`, `quantidadeItens` e os vínculos opcionais. `quantidadeItens` é o número de produtos distintos, não a soma das quantidades fracionárias.
4. Evitar carregar e recalcular cada fatura completa para produzir o resumo. Preservar os limites inclusivos das datas na consulta SQL.

**Aceite.** Conferir filtros isolados e combinados, registros no início e fim do período, resultados vazios e opcionais ausentes. Cliente positivo sem faturas retorna lista vazia; código inválido e intervalo invertido retornam `400`.

**Revisão e parada.** Conferir datas, parâmetros, resumo, ordenação e quantidade de consultas. Pedir ok para a etapa 21.

**Encerramento da implementação — 10 de novembro de 2026. Status: concluída, aguardando aprovação da etapa 21.**

- **HTTP e filtros:** exposto `GET /api/faturas` por `TkListaFaturas`, mantendo POST na mesma rota e `405` com `Allow: GET, POST` para outros métodos. A URL é lida com decodificação UTF-8; filtros vazios são ignorados. `FiltrosFaturas` valida cliente positivo, datas de `0001-01-01` a `9999-12-31` e intervalo não invertido antes de abrir conexão. O primeiro dia é inclusivo e o último é convertido ao início exclusivo do dia seguinte.
- **Resumo e coleção:** `Faturas.listar()` e `listar(FiltrosFaturas)` passam a devolver `ResumoFatura`. O valor compõe identidade, data, contagem e vínculos opcionais; a resposta expõe código, data de criação, quantidade de itens e, quando presentes, cliente e pagamento. Todos os implementadores foram atualizados. A contagem usa os produtos distintos garantidos pela chave primária dos itens, sem somar quantidades fracionárias.
- **SQL e custo:** `ConsultaSqlDeResumosFaturas` faz uma consulta agregada de `fatura` e `item_fatura`, com parâmetros tipados, condições combinadas com `AND` e ordenação por data/código decrescentes. Não busca produtos, preços ou dados de clientes. A consulta antiga de faturas completas foi retirada da listagem; `ConsultaSqlDeFaturas` ficou dedicada à leitura por código, preservada pelos testes anteriores. Datas são comparadas diretamente com a coluna, sem convertê-la por função.
- **Erros e ausência de dados:** filtro inválido retorna `400`; resultado sem correspondências retorna `[]`, inclusive para cliente positivo inexistente ou sem faturas. Falha de conexão ou de execução SQL retorna `503` genérico. A listagem não valida a existência do cliente por consulta separada e não grava dados.
- **Testes sem banco:** 13 entradas inválidas verificam formato, datas inexistentes/fora do limite, período invertido, código não positivo/fracionário/fora de `long` e tentativa de incluir SQL no valor do código, todas antes de I/O. Outro caso confere `503` de conexão. O teste de método da coleção foi ajustado para DELETE, pois GET passou a ser aceito.
- **Testes PostgreSQL:** dois cenários conferem filtros isolados e combinados, limites do período à meia-noite e ao último microssegundo, opcionais ausentes, contagem com quantidades fracionárias e resultados vazios. As faturas são criadas fora da ordem cronológica para distinguir ordenação por data de ordenação só por código; duas têm a mesma data para testar o desempate por código. Também são aceitos parâmetros codificados/vazios e os limites `0001-01-01` e `9999-12-31`, incluindo o fim do último dia.
- **Prova de leitura resumida:** o usuário temporário do teste tem SELECT somente em `fatura` e `item_fatura`, com ausência de acesso a produtos/clientes conferida no PostgreSQL. As conexões são somente de leitura, e cada GET executa uma única consulta. Revogar temporariamente SELECT dos itens provoca `503`; restaurar a permissão permite consultar novamente. Os usuários e concessões são removidos ao terminar.
- **Validação real:** passaram 255 casos em 21 suítes sem banco e 367 casos em 35 suítes no perfil completo com PostgreSQL 14 descartável, sem falhas, erros ou ignorados. As duas suítes novas foram registradas como obrigatórias; o cenário compartilhado passou a limpar também clientes adicionais. Relatórios finais em `precificacao-rica/target/surefire-integracao/20261110-174642326`; resumo em `precificacao-rica/target/validacao-etapa20-20261110.json`. Conferidos zero clientes/cidades, faturas/itens, históricos, objetos e usuários temporários. Nenhuma migração nova ou alteração do código anêmico foi necessária.
- **Próxima etapa:** validar os cenários equivalentes entre os dois módulos e concluir documentação/Postman, conforme a etapa 21, incluindo a pendência de UTF-8 nos filtros de produtos. Aguardar ok antes de iniciar. A mudança do desconto mínimo de R$ 200 continua dependendo de autorização posterior própria.

### Etapa 21 — Validar a equivalência e fechar a documentação

**Entrega.** Registro dos contratos equivalentes, suíte completa executada e documentação e coleção Postman atualizadas para o rico.

**Implementação.**

1. Executar os mesmos cenários de negócio nos dois módulos, usando os códigos gerados em cada banco. Comparar significado, valores, campos e respostas, sem exigir igualdade entre identidades ou horários gerados independentemente.
2. Exercitar categoria, produto, histórico, cidade, cliente, atualização de contatos e endereço, criação de fatura, simulação, consulta com preços alterados e listagens filtradas.
3. Executar as verificações de falha parcial, concorrência controlada, precisão e limite válidos e inválidos. O perfil de integração não pode aprovar casos ignorados ou aproveitar relatórios antigos.
4. Atualizar o README e criar a coleção do rico, preferencialmente com `baseUrl` e variáveis de códigos próprias. Reexecutar o fluxo com novos dados para verificar que ele não depende de IDs fixos.
5. Registrar os cenários e resultados reais e atualizar o quadro de pendências deste plano. A contagem dos testes de um módulo não precisa ser igual à do outro.
6. Verificar acentos e símbolos codificados nos filtros de produtos em uma JVM com codificação padrão diferente de UTF-8. `TkListaProdutos` ainda usa a leitura por `Href`, que apresentou dependência da codificação da JVM na rota de cidades durante a revisão da etapa 9. Corrigir e validar esse ponto antes de encerrar a equivalência.

**Aceite.** Todas as rotas da matriz da seção 2 estão disponíveis e os fluxos comparados apresentam os resultados esperados. Cliente continua vinculado à fatura após mudança de contatos ou endereço; categoria continua vinculada ao produto após renomeação; histórico e dados anteriores são preservados em falhas.

**Revisão e parada.** Revisar os objetos, os limites entre domínio, HTTP e PostgreSQL, a documentação e os testes. Apresentar o resultado da equiparação e parar. A mudança de manutenção do TCC exige uma autorização própria, descrita adiante.

**Encerramento — 10 de novembro de 2026. Status: concluída. Plano de equiparação encerrado.**

- **Correção de código:** leitura UTF-8 explícita dos parâmetros em `TkListaProdutos`, mantendo filtros e regras nos objetos existentes. Sem migração ou mudança de regra de negócio.
- **Teste relevante:** `FiltroProdutosUtf8Test` inicia uma JVM Windows-1252 e verifica acentos, símbolos e parâmetros combinados. Reproduziu a falha anterior e passou com a correção. A suíte foi incluída na conferência obrigatória do perfil de integração.
- **Resultados reais:** anêmico com 84 casos unitários em cinco suítes e 159 completos em nove; rico com 256 unitários em 22 suítes e 368 completos em 36. Nenhuma falha, erro ou caso ignorado. Os completos incluem os unitários. Ambos os JARs foram gerados e executados.
- **Fluxos integrados:** coleção rica com 75 requisições executada por HTTP real, em duas rodadas por modelo, usando bancos separados, códigos gerados diferentes e novos dados por rodada. Foram 300 requisições, 432 verificações `pm.test` e 150 pares de respostas comparados com sucesso. Os cinco cenários com diferenças textuais de erro estão documentados; os status e os campos essenciais foram conferidos.
- **Documentação e Postman:** READMEs atualizados, coleção rica entregue, executor local e [registro final de equivalência](Validacao-Equivalencia-Modelos.md) com instruções de reprodução e evidências. O executor usa somente as APIs `pm` presentes na coleção; não substitui uma validação de importação pelo aplicativo Postman.
- **Revisão:** correção restrita à borda HTTP, sem dependência entre os domínios. Os testes completos exercitaram precisão, limites, falhas parciais e concorrência controlada já cobertos nas etapas anteriores. O cenário integrado conferiu vínculos preservados após mudanças de contatos, endereço e categoria, além de preços atuais e retomada do desconto fixo.
- **Pendências do plano:** nenhuma. O limite de R$ 200 e medições de manutenibilidade ou desempenho não foram implementados ou obtidos nesta etapa. Novo trabalho depende de autorização própria.

## 7. Migrações existentes e organização

### Migrações independentes

O rico já possui as dez migrações abaixo. Todas foram aplicadas com sucesso no banco descartável desta retomada. Antes de acrescentar outra, conferir o diretório e o histórico real do Flyway e usar o próximo número disponível.

| Migração existente | Conteúdo | Relação com o plano |
| --- | --- | --- |
| V1 | Criação de `produto`. | Base anterior ao plano. |
| V2 | Rejeição de `NaN` na precificação. | Base anterior ao plano. |
| V3 | `categoria`, código gerado e unicidade do nome. | 3 |
| V4 | Categoria opcional em `produto` e sua chave estrangeira. | 4 |
| V5 | `fatura` e `item_fatura`, com quantidade, posição e desconto original. | Parte SQL da 17. |
| V6 | `historico_precificacao`, produto, data e valores anteriores e novos. | 6 |
| V7 | `cidade`, UF e unicidade por nome e UF. | 8 |
| V8 | `cliente`, unicidade de CPF/e-mail, endereço e vínculo com cidade. | 11 |
| V9 | Cliente opcional na fatura e índice correspondente. | Parte SQL da 17. |
| V10 | Forma de pagamento opcional na fatura. | Parte SQL da 17. |

Esta lista substitui a previsão de numeração da versão inicial deste plano. V8 do rico não corresponde ao conteúdo da V8 do anêmico. Cada módulo possui sequência, banco e histórico próprios. Não editar migrações já aplicadas, recriar tabelas existentes nem executar mudanças no banco anêmico para acomodar uma etapa do rico. Se uma entrega realmente precisar de alteração de esquema, a próxima versão atualmente disponível é V11; confirmar novamente antes de usá-la.

O objetivo final é conservar as mesmas relações e restrições relevantes. O esquema pode ser criado em outra sequência; os objetos não precisam repetir a divisão de classes do anêmico.

### Organização atual dos pacotes

Os nomes abaixo representam a organização existente. Os adaptadores acrescentados na revisão de modelagem ficam no assunto a que pertencem. Criar novas classes somente para responsabilidades necessárias à etapa autorizada.

```text
br.com.modelagemdominio.rico
├── AplicacaoRica
├── produtos
│   ├── Produto, Produtos, ProdutoConstante e valores existentes
│   ├── precificacao
│   ├── HistoricoDePrecificacao e AlteracaoPrecificacao
│   ├── pg
│   └── http
├── categorias
│   ├── Categoria, Categorias, CodigoCategoria e NomeCategoria
│   ├── pg — PgCategorias, CategoriaDaLinha e FalhaNaRenomeacaoCategoria
│   └── http
├── cidades
│   ├── Cidade, Cidades, CodigoCidade, NomeCidade, UnidadeFederativa e SiglaUf
│   ├── pg — PgCidades e ResultadoSqlDeCidade
│   └── http
├── clientes
│   ├── Cliente, Clientes e valores de identificação/contatos/endereço
│   ├── pg
│   └── http
├── faturas
│   ├── Fatura, Faturas, ItemFatura, Quantidade e valores de cálculo
│   ├── descontos — estratégias, registro original e DescontoReconstituido
│   ├── forma de pagamento e vínculos opcionais
│   ├── pg
│   └── http
├── numeros — InteiroExato compartilhado pelos códigos de categoria e cidade
├── http — CorpoJson e ProblemasHttp compartilhados pelos adaptadores HTTP
└── infraestrutura/banco — preparação do banco entregue na etapa funcional 2
```

`descontos` pertence a `faturas`, porque trata do abatimento no valor da fatura. A infraestrutura de conexão e transação pode ter composição própria, com responsabilidade explícita. A API continua sendo montada em `AplicacaoRica`, sem consultas escondidas na construção das entidades.

## 8. Verificações que não podem faltar

Esta tabela concentra os casos que orientam os testes das etapas. Ela não pede uma repetição idêntica de cada cenário em toda camada.

| Área | Cenários relevantes |
| --- | --- |
| Precificação | Três combinações nas duas modalidades; duas grandezas; custo calculado gravado; zero, limites, precisão e resultado rejeitado sem alterar estado. |
| Histórico | Mudança efetiva, repetição sem novo registro, ordem da consulta, falha após iniciar a escrita e preservação do estado anterior. |
| Categoria | Unicidade sem capitalização, vínculo opcional, troca, renomeação e preservação de produto e histórico. |
| Cidade | UF válida/inválida, normalização, unicidade por nome e UF, trecho literal, filtros combinados e lista vazia. |
| Cliente | CPF com verificadores, e-mail único, campos opcionais, presença no PATCH, atualização de somente as colunas enviadas e preservação em erro. |
| Concorrência | Contatos distintos atualizados sem perda; disputa de preço com histórico consistente; criação simultânea do mesmo banco, quando habilitada. |
| Fatura | Lista vazia, repetição de produto, referência ausente, quantidade mínima/máxima, casas excedentes e limites válidos. |
| Valores | Subtotais e desconto exatos, empates de arredondamento nos dois sentidos, total zero e desconto fixo reduzido e retomado. |
| Persistência da fatura | Cabeçalho e itens juntos; falha de um item desfaz o conjunto; desconto original e vínculos corretos. |
| Leitura e simulação | Preço atual entre requisições, estado coerente dentro de uma resposta, simulação sem gravação e valores esperados independentes. |
| HTTP | Campos e formatos, `201/200/400/404/503`, JSON inválido, códigos não inteiros, campos desconhecidos, enum inválido e detalhes internos protegidos. |
| Execução | Banco exclusivo, migrações corretas, configuração obrigatória, nenhuma suíte omitida, nenhum caso ignorado e relatórios da execução atual. |

Erros podem ser introduzidos em uma cópia temporária para provar uma proteção importante, como foi feito na revisão do anêmico. Esse recurso não autoriza alterar a implementação entregue nem registrar uma falha simulada como resultado do sistema real.

Se a implementação revelar um defeito ou uma regra ambígua no anêmico, registrar a divergência e pedir uma decisão sobre a correção em escopo separado. Preservar a referência do estudo e evitar alterações silenciosas nos dois módulos durante a etapa do rico.

## 9. Mudança posterior para o estudo do TCC

**Proposta descartada pelo usuário após a etapa 21. Não implementar nem propor seu avanço automaticamente. O texto abaixo conserva o planejamento anterior como histórico.**

A proposta anterior previa aplicar o desconto percentual somente quando o subtotal exato dos itens atingisse R$ 200, antes do arredondamento final, conservando o desconto fixo. Essa proposta foi descartada e não é uma funcionalidade a implementar.

O planejamento anterior previa a sequência abaixo, mantida apenas como histórico arquivado:

1. Registrar a base funcional equivalente e os casos abaixo, exatamente no limite e acima de R$ 200. Incluir um subtotal exato abaixo do limite que arredonde para R$ 200 no total, para conferir o momento da comparação.
2. Implementar a mudança somente no primeiro módulo autorizado, registrar os pontos alterados e validar os cenários.
3. Pedir ok antes de aplicar a mesma mudança ao outro módulo, respeitando sua arquitetura.
4. Comparar localização da regra, arquivos e responsabilidades afetados, dependências necessárias para compreendê-la e preservação dos demais comportamentos. Identificar qual política foi adotada para faturas já cadastradas e seguir a mesma nos dois módulos; a consulta atual recalcula os valores.
5. Registrar resultados observados e suas limitações. Quantidade de classes, linhas ou testes pode fornecer contexto, mas não demonstra sozinha manutenibilidade.

Não registrar uma funcionalidade planejada como concluída nem fabricar tempos, índices de cobertura ou resultados para preencher a discussão do TCC.

## 10. Registro de encerramento de cada etapa

Usar este formato durante a implementação:

```text
Etapa N — nome da entrega
Status: concluída / pendente de validação / aguardando decisão
Arquivos alterados:
Migração criada, se houver:
Comandos e cenários executados:
Resultados reais:
Revisão de código e correções realizadas:
Pendências:
Próxima etapa proposta:
Pergunta ao usuário: posso seguir para a etapa N+1?
```

Se houver verificação obrigatória pendente, não marcar a etapa como concluída. Registrar a situação e aguardar orientação. Ao terminar a etapa 21, apresentar o fechamento do plano; o próximo trabalho precisa ser autorizado separadamente.

## 11. Referências para retomar

- [Visão geral do projeto](../README.md).
- [README do modelo anêmico e comandos de testes](../precificacao-anemica/README.md#testes).
- [Registro dos nove passos funcionais do anêmico](Validacao-Plano-Anemico.md).
- [README e decisões já aplicadas no rico](../precificacao-rica/README.md).
- [Contratos atuais de produtos](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/produtos/Produtos.java).
- [Coleção de produtos com leitura constante](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/produtos/pg/PgProdutosConstantes.java).
- [Contratos atuais de faturas](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/Faturas.java).
- [Item de fatura e consulta ao produto atual](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/ItemFatura.java).
- [Migrações existentes do rico](../precificacao-rica/src/main/resources/db/migration).
- [Controllers do anêmico usados como referência de contrato](../precificacao-anemica/src/main/java/br/com/precificacao/anemico/controllers).
- [Coleção Postman do anêmico](postman/precificacao-anemica.postman_collection.json).

O documento de entrada `Documentacao-e-Plano-Precificacao.md`, fornecido pelo usuário, orientou o formato. As convenções do rico foram conferidas em `Documentacao-Precificacao.md`, no README e nos objetos existentes. As instruções de trabalho deste plano só se aplicam quando o usuário autorizar sua implementação.

## 12. Revisão de modelagem — etapas 1 a 34 concluídas

Após a equiparação funcional, o usuário solicitou uma revisão dos conceitos de Elegant Objects e autorizou inicialmente os objetos de valor. Cada etapa exige aprovação explícita; concluir uma não autoriza antecipar a próxima. Os registros abaixo delimitam o que foi autorizado e concluído.

### Etapa 1 — Dinheiro e Percentual

- **Implementação:** os dois valores mantêm um atributo imutável e passam a rejeitar ausência, sinal, precisão ou limite inválidos. A normalização fixa as casas decimais sem arredondar entradas excedentes. Comparações e igualdade deixam de depender dos zeros finais.
- **Comportamento utilizado:** `Precificacao` compara os valores por seus métodos, e as duas estratégias usam a fração exata do percentual. `Precificacoes` delega as validações individuais, mantendo as regras entre valores e o arredondamento dos resultados.
- **Construção:** entradas em `BigDecimal`, texto decimal e inteiro, com delegação ao construtor principal. Foi mantida a decisão local de validar e normalizar na construção; esta etapa não altera essa decisão para o restante do modelo.
- **Erros:** `ValorInvalido` permite adaptar a descrição ao campo recebido. A tradução para `PrecificacaoInvalida` preserva a causa e as mensagens existentes da operação.
- **Escopo:** nenhum ajuste no anêmico, migração ou contrato HTTP. Os demais pontos da revisão aguardam aprovação própria.
- **Testes novos relevantes:** 13 casos verificam diretamente as invariantes e o comportamento dos valores. Quatro casos adicionais no cálculo cobrem as modalidades com percentual fracionário nas duas direções, descrição do campo e causa da falha, além da ausência de valores nas três combinações. Não foram acrescentados testes de contagem de atributos ou construtores.
- **Resultados reais:** 273 casos em 23 suítes sem banco e 385 em 37 suítes no modo completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Comandos: `mvn -o -s configuracao-maven.xml -Dmaven.repo.local=C:\Java\Workspace\tmp\precificacao-maven-repository -pl precificacao-rica test` e o mesmo comando com `-Pintegracao package`.
- **Evidências:** relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-182745211`; logs em `target/revisao-eo/etapa1-valores-unitarios.log` e `etapa1-valores-integracao.log`. JAR gerado. Confirmados zero produtos, faturas e clientes de apoio ao terminar; servidor descartável encerrado e removido.
- **Status:** concluída em 10/11/2026, aguardando ok para a próxima etapa. Não foram aplicados os demais ajustes da revisão.
- **Próxima etapa proposta:** corrigir a composição das dependências, começando por `PgFaturas`, com construtor primário que receba os colaboradores e preservação da mesma conexão nas operações transacionais.

### Etapa 2 — Composição das dependências de PgFaturas

- **Implementação:** `PgFaturas` mantém três atributos finais: banco, `ProdutosNaConexao` e `ClientesNaConexao`. O construtor primário recebe esses colaboradores e os valida sem criar dependências ou executar consultas. O construtor padrão delega a ele, compondo as implementações existentes.
- **Contratos e montagem:** criados os dois contratos nos pacotes `produtos.pg` e `clientes.pg`; `PgProdutosConstantes` e `PgClientes` os implementam com os métodos já existentes. `AplicacaoRica` fornece os objetos já montados de produtos e clientes. As interfaces de domínio permanecem livres de JDBC.
- **Transação:** a conexão continua pertencendo à operação da fatura. Cadastro recebe consultas protegidas; simulação usa leitura sem bloqueios em transação somente de leitura com isolamento repetível. Os contratos não transferem aos colaboradores a abertura, confirmação, reversão ou finalização da conexão.
- **Testes relevantes:** quatro casos PostgreSQL verificam os colaboradores recebidos em cadastro e simulação. Conferem identidade da conexão, estado transacional, modo de consulta e encerramento pelo chamador. Um fake de cliente substitui a consulta real, e a consulta de produtos usa uma fonte que falha se tentar abrir conexão própria. A recusa do fake deve interromper ambos os fluxos sem consultar produtos ou gravar. O teste sem banco existente também verifica o novo construtor; não foi criado um teste separado de contagem de atributos ou construtores.
- **Validação real:** 273 casos em 23 suítes sem banco e 389 em 38 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. A nova suíte foi registrada como obrigatória. Comandos iguais aos da etapa 1, com logs `target/revisao-eo/etapa2-composicao-unitarios.log` e `etapa2-composicao-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-183746403`.
- **Limpeza e escopo:** JAR gerado; confirmados zero produtos, faturas e clientes de apoio ao terminar. PostgreSQL descartável encerrado e removido. Nenhuma migração, regra de negócio ou alteração no anêmico foi necessária.
- **Status:** concluída em 10/11/2026. Aguardar ok antes da etapa 3.
- **Próxima etapa proposta:** reduzir utilitárias estáticas do domínio, começando por `TextoCliente`, preservando as validações e mensagens existentes.

### Etapa 3 — Objetos de texto do cliente

- **Implementação:** `TextoCliente<T>` substitui a utilitária por um contrato interno com um método. `TextoObrigatorio` mantém campo, conteúdo e limite; `TextoOpcional` compõe o comportamento do texto presente e representa sua ausência. Ambos são finais, sem métodos estáticos, com dois construtores e um método de comportamento. Possuem, respectivamente, três e um atributos de instância, todos finais.
- **Construção e comportamento:** os construtores secundários preparam a entrada e delegam ao primário, que conserva valores ou objetos compostos. O método `valor()` produz o resultado validado. Os valores públicos do cadastro continuam solicitando esse resultado na construção, preservando o momento de validação já adotado no projeto.
- **Consumidores:** atualizados `NomeCliente`, `Email`, `Telefone`, `Cpf`, `Cep` e `LocalizacaoEndereco`. Removidas as chamadas estáticas a `TextoCliente`. Limites, mensagens, normalização de CPF/CEP e validações específicas permanecem com os mesmos resultados.
- **Regressões relevantes:** três casos de `TextosClienteTest` conferem espaços Unicode sem alteração do texto interno, obrigatoriedade versus ausência para texto em branco maior que o limite e rejeição de caractere nulo no complemento. Passaram com a implementação anterior e com os novos objetos, demonstrando preservação das regras. Não foram acrescentados testes de contagem de classes, atributos ou construtores.
- **Validação real:** 276 casos em 24 suítes sem banco e 392 em 39 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Os testes HTTP e PostgreSQL existentes de clientes também passaram; a nova suíte foi registrada como obrigatória. Comandos iguais aos das etapas anteriores.
- **Evidências:** logs `target/revisao-eo/etapa3-texto-antes.log`, `etapa3-texto-unitarios.log` e `etapa3-texto-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-190406864`. JAR gerado; confirmados zero produtos, faturas e clientes de apoio ao terminar. PostgreSQL descartável encerrado e removido.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração ou alteração no anêmico. Aguardar ok antes da etapa 4.
- **Próxima etapa proposta:** substituir as fábricas estáticas de `AlteracaoCampo` por objetos que expressem preservação, remoção e substituição, mantendo o comportamento do PATCH de cliente.

### Etapa 4 — Operações de alteração de campo

- **Implementação:** `AlteracaoCampo<T>` passa a ser um contrato. `CampoPreservado`, `CampoRemovido` e `CampoSubstituido` expressam os três comportamentos, sem fábricas estáticas ou combinação de booleano e valor no construtor. O método `aplicar` mantém o resultado anterior ou executa a operação com ausência ou novo valor.
- **Composição utilizada:** o PATCH cria os objetos de acordo com a presença do campo. `Clientes.alterarEmail` compõe a preservação dos demais campos. `PgClientes` usa a operação de telefone para incluir a coluna somente quando solicitada. `informada()` e `valor()` são consultas derivadas do comportamento, preservando os consumidores de endereço e a validação da alteração conjunta.
- **Estrutura:** os três tipos são finais. Preservação e remoção são políticas sem estado, com construtores padrão; substituição conserva um único atributo final e rejeita valor nulo. Não foram criados atributos ou sobrecargas de construtor apenas para cumprir contagens.
- **Testes relevantes:** quatro casos de `AlteracoesCampoTest` verificam preservação sem escrita, remoção com ausência, substituição com o novo valor, execução da escrita uma única vez, consultas derivadas e rejeição de substituição nula. Os testes de atualização parcial existentes continuam verificando campos omitidos, remoção explícita, validação conjunta, colunas escritas, rollback e concorrência. Não foram acrescentados testes de contagem de classes, atributos ou construtores.
- **Validação real:** 280 casos em 25 suítes sem banco e 396 em 40 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. A nova suíte foi registrada como obrigatória. Comandos iguais aos das etapas anteriores.
- **Evidências:** logs `target/revisao-eo/etapa4-campos-unitarios.log` e `etapa4-campos-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-191453853`. JAR gerado; confirmados zero produtos, faturas e clientes de apoio ao terminar. PostgreSQL descartável encerrado e removido.
- **Escopo e status:** concluída em 10/11/2026. Contrato HTTP, regras e mensagens do PATCH preservados. Nenhuma migração ou alteração no anêmico. Aguardar ok antes da etapa 5.
- **Próxima etapa proposta:** substituir `Telefone.opcional` por um objeto composto para telefone opcional, preservando ausência, texto em branco, validação e normalização do cadastro e da leitura do cliente.

### Etapa 5 — Telefone opcional por composição

- **Implementação:** `TelefoneOpcional` substitui `Telefone.opcional`, removida de `Telefone`. O novo objeto é final, implementa `TextoCliente<Optional<Telefone>>`, mantém um atributo final e possui dois construtores e um método de comportamento. O construtor que recebe texto delega ao primário com a representação da presença ou ausência.
- **Composição utilizada:** `valor()` conserva a ausência para conteúdo nulo ou em branco e compõe `Telefone` para conteúdo presente. Formato brasileiro, limite de tamanho, remoção de espaços nas extremidades e mensagens permanecem sob responsabilidade de `Telefone`, sem duplicar essas regras. O cadastro HTTP e a materialização em `PgClientes` usam o novo objeto.
- **Atualização parcial:** o PATCH continua usando suas operações de preservação, remoção e substituição. Campo omitido preserva o telefone, `null` solicita remoção e texto em branco é rejeitado. O objeto opcional não foi aplicado à substituição explícita.
- **Testes relevantes:** três casos de `TelefoneOpcionalTest` verificam ausência para nulo e texto Unicode em branco acima do limite, preservação da formatação com remoção de espaços nas extremidades e delegação da rejeição de formato inválido ao solicitar o resultado. Os testes HTTP e PostgreSQL existentes também verificam cadastro, leitura, validação do PATCH, escrita parcial, rollback e concorrência. Não foram acrescentados testes de contagem de atributos ou construtores.
- **Validação real:** 283 casos em 26 suítes sem banco e 399 em 41 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. A nova suíte foi registrada como obrigatória. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9.
- **Evidências:** logs `target/revisao-eo/etapa5-telefone-unitarios.log` e `etapa5-telefone-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-192435575`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa5-20261110.json`. JAR gerado; confirmados zero produtos, faturas e clientes de apoio ao terminar. PostgreSQL descartável encerrado e removido; banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de contrato HTTP. Aguardar ok antes da etapa 6.
- **Próxima etapa proposta:** substituir os auxiliares estáticos de validação dos dígitos do CPF por comportamento de objetos, preservando normalização, formatos aceitos e mensagens. Não iniciada nesta etapa.

### Etapa 6 — Conferência dos dígitos do CPF por objetos

- **Implementação:** removidos os métodos estáticos `digitosVerificadoresValidos` e `digitoVerificador` de `Cpf`. A conferência passa a ser composta por `DigitosVerificadoresCpf` e `DigitoVerificadorCpf`, internos ao pacote de clientes, sem dependência de HTTP ou PostgreSQL.
- **Responsabilidades:** `Cpf` mantém presença, limite, formato aceito, remoção da máscara e rejeição de sequências repetidas. `DigitosVerificadoresCpf` exige a aprovação dos dois dígitos. `DigitoVerificadorCpf` calcula o resultado sobre o prefixo recebido e o compara com o dígito informado. O segundo usa os dez primeiros algarismos, incluindo o primeiro dígito recebido, como na implementação anterior.
- **Estrutura e construção:** os dois objetos são finais, com dois atributos finais, dois construtores e um método de comportamento cada. O primário conserva seus valores ou colaboradores. O secundário compõe as conferências ou separa prefixo e dígito e delega ao primário. Não foram introduzidos getters, setters, novos auxiliares estáticos ou construtores apenas para cumprir contagens.
- **Compatibilidade:** a validação de `Cpf` continua ocorrendo na construção. O valor normalizado, a igualdade do record e as mensagens existentes são preservados. Cadastro, leitura e tratamento HTTP continuam usando o mesmo objeto público `Cpf`.
- **Testes relevantes:** os 12 casos de `CpfTest` passaram com a implementação anterior e com os objetos novos. Conferem máscara e espaços Unicode, restos zero e um no cálculo dos dois dígitos, zeros iniciais, falha em cada dígito, sequência repetida matematicamente compatível e formatos inválidos. No caso de primeiro dígito errado, o segundo confere com o prefixo recebido, impedindo que omitir a primeira conferência passe despercebido. Não foram criados testes de contagem de atributos, construtores ou classes.
- **Validação real:** 295 casos em 27 suítes sem banco, antes e depois da refatoração, e 411 em 42 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. A nova suíte foi registrada como obrigatória. Os fluxos HTTP e PostgreSQL existentes também passaram. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9.
- **Evidências:** logs `target/revisao-eo/etapa6-cpf-antes.log`, `etapa6-cpf-unitarios.log` e `etapa6-cpf-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-193047286`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa6-20261110.json`. JAR gerado; confirmados zero produtos, faturas e clientes de apoio ao terminar. PostgreSQL descartável encerrado e removido; banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de contrato HTTP. Aguardar ok antes da etapa 7.
- **Próxima etapa proposta:** substituir `Desconto.reconstituir` por composição de objetos, preservando as estratégias, o valor original do desconto e a diferença entre desconto zero e ausência. Não iniciada nesta etapa.

### Etapa 7 — Reconstituição do desconto por composição

- **Implementação:** removida a fábrica estática `Desconto.reconstituir`. `DescontoReconstituido` implementa `Desconto` e atua como decorator da estratégia composta a partir do registro original. O contrato `Desconto` conserva cálculo, validação do subtotal inicial e representação para gravação.
- **Comportamento:** os três métodos delegam à estratégia recebida. Ausência compõe `SemDesconto`; registro presente compõe o comportamento de `DescontoContratado`. Um valor zero continua registrado com seu tipo. As fórmulas, os limites e as mensagens permanecem nas estratégias existentes.
- **Estrutura e construção:** o objeto é final e possui um atributo final, quatro construtores e três métodos de comportamento. Há entradas para ausência, registro presente, registro opcional e estratégia já composta. Os secundários delegam ao primário, que conserva a estratégia. Não foram introduzidos getters ou setters.
- **Consumidor PostgreSQL:** `CabecalhoFatura` compõe o novo objeto com ou sem registro a partir das colunas já lidas. A materialização continua usando a conexão e a consulta existentes. O valor contratado permanece independente do abatimento limitado pelo subtotal atual.
- **Testes relevantes:** reutilizados os cinco casos de reconstituição de `DescontosTest`, adaptados ao novo objeto. Acrescentada uma regressão para impedir que o decorator herde a validação inicial vazia do contrato e aceite desconto fixo acima do subtotal; o limite igual ao valor contratado continua aceito. Os 21 casos da suíte de descontos passaram antes e depois da refatoração. Não foram criados testes de contagem de atributos ou construtores nem uma nova suíte para repetir os casos existentes.
- **Validação real:** 296 casos em 27 suítes sem banco, antes e depois da troca, e 412 em 42 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Os testes PostgreSQL existentes de cadastro e consulta também passaram, inclusive desconto zero, percentual fracionário, preço atual, redução e retomada do desconto fixo e preservação dos valores gravados. A suíte de descontos já era obrigatória no perfil completo. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9.
- **Evidências:** logs `target/revisao-eo/etapa7-desconto-antes.log`, `etapa7-desconto-unitarios.log` e `etapa7-desconto-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-193706490`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa7-20261110.json`. JAR gerado; confirmados zero produtos, faturas e clientes de apoio ao terminar. PostgreSQL descartável encerrado e removido; banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de contrato HTTP. Aguardar ok antes da etapa 8.
- **Próxima etapa proposta:** substituir `CabecalhoFatura.daLinha` por construção de objetos na materialização PostgreSQL, preservando a leitura conjunta de cabeçalho e itens e a ausência de consultas adicionais. Não iniciada nesta etapa.

### Etapa 8 — Construção do cabeçalho da fatura

- **Implementação:** removida a fábrica estática `CabecalhoFatura.daLinha`. O construtor que recebe a linha SQL delega ao construtor canônico com código, data, desconto e vínculos materializados. `ConsultaSqlDeFaturas` utiliza `new CabecalhoFatura(resultado)`.
- **Estrutura e comportamento:** o cabeçalho conserva seus quatro componentes imutáveis e o comportamento `fatura(itens)`, que compõe `FaturaConstante` e `CalculoFatura`. Não conserva o `ResultSet` nem acrescenta acesso posterior ao banco. O desconto continua composto por `DescontoReconstituido`; as regras permanecem nos objetos de domínio existentes.
- **Persistência:** cabeçalho, itens e preços continuam vindo da mesma consulta e conexão. A leitura das colunas ocorre com o cursor aberto e posicionado, antes de percorrer os demais itens. Cadastro transacional, tratamento de falhas e contratos HTTP foram preservados.
- **Testes relevantes:** o teste integrado dos vínculos foi ampliado de duas para quatro combinações: ambos ausentes, somente pagamento, somente cliente e ambos presentes. Além de comparar cadastro e consulta, cada caso verifica explicitamente a presença de cada vínculo, a consulta única e a ausência de escrita. Foram reutilizados os testes de descontos, precisão, preços atuais, preservação da leitura anterior, erro de materialização e rollback. Não foram criados testes de estrutura ou construtores.
- **Validação real:** 296 casos em 27 suítes sem banco e 414 em 42 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa8-cabecalho-unitarios.log` e `etapa8-cabecalho-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-194357392`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa8-20261110.json`. Confirmados zero produtos, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de regras de negócio. Aguardar ok antes da etapa 9.
- **Próxima etapa proposta:** substituir `ResultadoSqlDeProduto.codigoCategoria` por comportamento de objeto, preservando o vínculo opcional e os consumidores da leitura PostgreSQL do produto. Não iniciada nesta etapa.

### Etapa 9 — Leitura do vínculo de categoria por objeto

- **Implementação:** removido o auxiliar estático `ResultadoSqlDeProduto.codigoCategoria`. `ResultadoSqlDeCategoria` interpreta o vínculo opcional e é composto tanto por `ResultadoSqlDeProduto` quanto por `PgProduto.codigoCategoria()`.
- **Estrutura e construção:** o novo objeto é final, com um atributo final, dois construtores e um método de comportamento. O construtor que recebe a linha SQL lê a coluna existente e delega ao primário com `Optional<Long>`. O primário conserva essa referência opcional. Não guarda o cursor, não abre conexão e não acrescenta consultas.
- **Comportamento:** `codigo()` conserva a ausência para `NULL` e compõe `CodigoCategoria` quando houver referência. A validação do código positivo continua no domínio. O produto materializado conserva o vínculo lido; a leitura individual continua consultando o registro atual. SQL, tratamento de falhas e contrato HTTP foram preservados. Os dois arquivos de produção existentes tocados na etapa foram ajustados para indentação de dois espaços.
- **Testes relevantes:** acrescentado um caso integrado a `PgProdutosTest` para ausência inicial, vínculo presente e retorno à ausência. Confere os dois consumidores, a atualização percebida pelo mesmo produto individual e a conservação da categoria nas leituras já materializadas, inclusive depois do fechamento de seus cursores. Foram reutilizados os testes existentes de categoria opcional no HTTP, listagem, troca de vínculo, faturas e falhas de chave estrangeira. Nenhuma nova suíte ou teste de contagem estrutural foi criado.
- **Validação real:** 296 casos em 27 suítes sem banco e 415 em 42 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Os 13 casos de `PgProdutosTest` passaram. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa9-categoria-unitarios.log` e `etapa9-categoria-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-194912111`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa9-20261110.json`. Confirmados zero produtos, categorias, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de regras de negócio. Aguardar ok antes da etapa 10.
- **Próxima etapa proposta:** substituir o auxiliar estático `CodigoCategoria.inteiroExato` por comportamento de objeto, preservando a rejeição de ausência, frações, valores fora do limite e códigos não positivos. Não iniciada nesta etapa.

### Etapa 10 — Conversão exata do código de categoria por objeto

- **Implementação:** removido o método estático `CodigoCategoria.inteiroExato`. O construtor decimal de `CodigoCategoria` compõe `InteiroExato`, solicita o inteiro e delega ao construtor canônico. A exigência de código positivo continua em `CodigoCategoria` e vale para as duas formas de entrada.
- **Estrutura e comportamento:** `InteiroExato` é interno ao pacote de categorias, final, com dois atributos finais, dois construtores e um método de comportamento. O secundário representa presença ou ausência do decimal e delega ao primário, que conserva o conteúdo e a mensagem recebida. `valor()` exige presença e executa a conversão exata para `long`. Não foram introduzidos getters, setters ou novos auxiliares estáticos.
- **Compatibilidade:** mantidos a mensagem original, o tipo `IllegalArgumentException` e a causa `ArithmeticException` nas falhas de conversão. Inteiros com zeros decimais excedentes continuam aceitos. Frações e valores fora dos dois limites de `long` são rejeitados sem truncamento ou arredondamento. A validação continua ocorrendo durante a construção do código, antes do acesso ao banco. A igualdade do record e seus consumidores permanecem iguais.
- **Testes relevantes:** os nove casos de `CodigoCategoriaTest` passaram antes e depois da refatoração. Conferem inteiro com zeros decimais, valor acima da precisão inteira exata de `double`, maior `long` positivo, ausência, fração pequena, estouro dos dois limites, zero e menor `long`. Os casos não positivos verificam os dois construtores; as falhas conferem mensagem e presença ou ausência da causa. A suíte foi registrada como obrigatória no perfil completo. Não foram criados testes de contagem estrutural nem testes que dependam da implementação interna do novo objeto.
- **Validação real:** 305 casos em 28 suítes sem banco, antes e depois da refatoração, e 424 em 43 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Os testes HTTP e PostgreSQL existentes também passaram. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa10-inteiro-antes.log`, `etapa10-inteiro-unitarios.log` e `etapa10-inteiro-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-195417845`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa10-20261110.json`. Confirmados zero produtos, categorias, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico, mudança de contrato HTTP ou de regras de negócio. `CodigoCidade` permanece como estava. Aguardar ok antes da etapa 11.
- **Próxima etapa proposta:** substituir `CodigoCidade.inteiroExato` por comportamento de objeto, preservando a conversão exata, a exigência de código positivo e as mensagens próprias de cidade. Não iniciada nesta etapa.

### Etapa 11 — Conversão exata do código de cidade por composição

- **Implementação:** removido o auxiliar estático `CodigoCidade.inteiroExato`. O construtor decimal compõe `InteiroExato`, solicita o inteiro e delega ao construtor canônico de `CodigoCidade`, que continua exigindo código positivo.
- **Reutilização:** o objeto `InteiroExato`, criado na etapa 10, foi movido de `categorias` para `numeros` e tornou-se público para uso pelos dois códigos. A implementação da conversão foi preservada; mudaram apenas pacote e visibilidade. `CodigoCategoria` recebeu o import do objeto compartilhado. Há uma única implementação da conversão, com dois atributos finais, dois construtores e um método de comportamento.
- **Compatibilidade:** cada código fornece sua própria mensagem ao objeto composto. Ausência e códigos não positivos mantêm `IllegalArgumentException` sem causa numérica; frações e estouro mantêm a causa `ArithmeticException`. A validação continua ocorrendo na construção, antes da persistência. Valores inteiros com zeros decimais e códigos grandes conservam precisão e igualdade. HTTP, SQL e regras de endereço foram preservados.
- **Testes relevantes:** os seis casos de `CodigoCidadeTest` passaram antes e depois da alteração. Conferem maior código com zeros decimais, ausência, fração pequena, estouro superior, zero e código negativo; verificam mensagens próprias de cidade e causas, além dos dois construtores para códigos não positivos. Os nove casos existentes de `CodigoCategoriaTest` continuam cobrindo os limites e a precisão da conversão compartilhada. A nova suíte foi registrada como obrigatória no perfil completo. Não foram criados testes de contagem estrutural nem testes dos detalhes internos de `InteiroExato`.
- **Validação real:** 311 casos em 29 suítes sem banco, antes e depois da refatoração, e 430 em 44 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Os fluxos existentes de cidades, cadastro e alteração de endereço, categorias e faturas também passaram. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa11-cidade-antes.log`, `etapa11-cidade-unitarios.log` e `etapa11-cidade-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-195957212`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa11-20261110.json`. Confirmados zero produtos, categorias, cidades, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de contrato HTTP. Aguardar ok antes da etapa 12.
- **Próxima etapa proposta:** substituir `UnidadeFederativa.daSigla` por um objeto que interprete e valide a sigla da UF, preservando os valores aceitos, a normalização e as mensagens. Não iniciada nesta etapa.

### Etapa 12 — Interpretação da UF por composição

- **Implementação:** removida a fábrica estática `UnidadeFederativa.daSigla`. `SiglaUf` recebe o texto e produz a unidade federativa em `unidade()`. O enum conserva o conjunto de UFs aceitas. `TkCadastroCidade` e `TkListaCidades` compõem o novo objeto.
- **Estrutura e construção:** `SiglaUf` é público e final, com um atributo final, dois construtores e um método de comportamento. O construtor de texto representa a presença ou ausência e delega ao primário com `Optional<String>`. A interpretação exige conteúdo, remove espaços nas extremidades e normaliza as letras com `Locale.ROOT`. Não foram introduzidos getters, setters ou novos auxiliares estáticos.
- **Compatibilidade:** ausência e texto em branco mantêm a mensagem de UF obrigatória, sem causa de conversão. Sigla inexistente ou com espaço interno mantém a mensagem de UF inválida e a causa `IllegalArgumentException`. No cadastro, a interpretação acontece antes da persistência. Na listagem, o parâmetro vazio ou em branco continua equivalendo à ausência de filtro, pois é tratado antes da composição. SQL, enum armazenado e contrato HTTP foram preservados.
- **Testes relevantes:** os seis casos de `SiglaUfTest` passaram com a fábrica anterior e com o novo objeto. Conferem espaços Unicode e minúsculas sob idioma turco, ausência, vazio, espaços em branco, sigla inexistente e espaço interno, com mensagens e causas. A alteração temporária do idioma usa o controle de recurso do JUnit e restaura o idioma ao terminar. Foram reutilizados os testes HTTP e PostgreSQL de normalização, validação antes da conexão, UF em branco no filtro e filtros combinados. A suíte foi registrada como obrigatória. Não foram criados testes de contagem estrutural.
- **Validação real:** 317 casos em 30 suítes sem banco, antes e depois da refatoração, e 436 em 45 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa12-uf-antes.log`, `etapa12-uf-unitarios.log` e `etapa12-uf-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-201128398`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa12-20261110.json`. Confirmados zero produtos, categorias, cidades, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de regras de negócio. Aguardar ok antes da etapa 13.
- **Próxima etapa proposta:** substituir `PgCidades.cidadeDoResultado` por comportamento de objeto na materialização da cidade, preservando a leitura na mesma consulta e o tratamento das falhas. Não iniciada nesta etapa.

### Etapa 13 — Materialização da cidade por composição

- **Implementação:** removida a fábrica estática `PgCidades.cidadeDoResultado`. `ResultadoSqlDeCidade` lê os valores da linha e compõe `CidadeConstante` pelo método `cidade()`. O retorno do cadastro e a listagem utilizam o mesmo objeto.
- **Estrutura e construção:** o novo objeto é final e interno a `cidades.pg`, com três atributos finais, dois construtores e um método de comportamento. O construtor que recebe o resultado SQL lê código, nome e UF e delega ao primário com os valores materializados. O primário conserva esses valores; o cursor SQL não é mantido. Não foram introduzidos getters, setters ou novos auxiliares estáticos.
- **Compatibilidade:** `NomeCidade` continua validando o nome, e a UF armazenada continua sendo interpretada pelo enum como antes. O cadastro usa as colunas de `RETURNING`; a listagem usa a linha da consulta existente. SQL, filtros, ordenação, fechamento dos recursos, logs e tratamento de `SQLException` foram preservados. O auxiliar de execução da consulta permanece fora do escopo desta etapa.
- **Teste relevante:** acrescentado um caso integrado a `ListaCidadesTest`. Ele cadastra e lista uma cidade, altera nome e UF diretamente no banco e confirma que os dois objetos anteriores conservam código, nome e UF. Uma nova listagem recebe os valores alterados. O teste também exige uma conexão e um comando por operação, incluindo após consultar os valores dos objetos retornados. Foram reutilizados os testes existentes de campos HTTP, filtros, duplicidade e falhas de banco. A suíte já era obrigatória; nenhuma nova suíte ou teste de contagem estrutural foi criado.
- **Validação real:** 317 casos em 30 suítes sem banco e 437 em 45 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários e a regressão integrada nova, cuja execução foi conferida no relatório. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa13-cidade-unitarios.log` e `etapa13-cidade-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-201801210`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa13-20261110.json`. Confirmados zero produtos, categorias, cidades, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de contrato HTTP. Aguardar ok antes da etapa 14.
- **Próxima etapa proposta:** substituir `PgCategorias.categoriaDoResultado` por comportamento de objeto na materialização da categoria, preservando as consultas e os fluxos de cadastro, listagem e renomeação. Não iniciada nesta etapa.

### Etapa 14 — Materialização da categoria por composição

- **Implementação:** removida a fábrica estática `PgCategorias.categoriaDoResultado`. `CategoriaDaLinha`, interno a `categorias.pg`, lê código e nome e compõe `CategoriaConstante` pelo método `categoria()`. Cadastro, renomeação e listagem utilizam esse comportamento.
- **Estrutura e construção:** o novo objeto é final, com dois atributos finais, dois construtores e um método de comportamento. O construtor da linha SQL delega ao primário com código e `NomeCategoria` já materializados. O primário conserva os valores, sem manter o cursor ou abrir conexão. Não foram introduzidos getters, setters ou novos auxiliares estáticos.
- **Compatibilidade:** `NomeCategoria` continua validando o nome. Cadastro e renomeação usam suas colunas de `RETURNING`, e a listagem usa a consulta existente. SQL, ordenação, fechamento dos recursos, tratamento de duplicidade, logs e tradução das exceções foram preservados. O auxiliar de classificação das falhas de renomeação permanece fora do escopo desta etapa.
- **Teste relevante:** acrescentado um caso a `RenomeacaoCategoriaPostgresTest`. Ele guarda os objetos devolvidos pelo cadastro, pela listagem e pela primeira renomeação; depois renomeia novamente e confere o código e o nome de cada leitura anterior. Uma nova listagem recebe o nome mais recente. O teste exige uma conexão e um comando por operação, incluindo após consultar os valores dos objetos retornados. Os testes existentes de campos HTTP, ordenação, duplicidade e falhas também passaram. A suíte já era obrigatória; nenhuma nova suíte ou teste de contagem estrutural foi criado.
- **Validação real:** 317 casos em 30 suítes sem banco e 438 em 45 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários e a regressão integrada nova, cuja execução foi conferida no relatório. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa14-categoria-unitarios.log` e `etapa14-categoria-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-202251677`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa14-20261110.json`. Confirmados zero produtos, categorias, cidades, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. Nenhuma migração, alteração no anêmico ou mudança de contrato HTTP. Aguardar ok antes da etapa 15.
- **Próxima etapa proposta:** substituir `PgCategorias.falhaAoRenomear` por comportamento de objeto, preservando a identificação da duplicidade de nome, as demais falhas de persistência, as mensagens, os logs e a causa original. Não iniciada nesta etapa.

### Etapa 15 — Classificação da falha de renomeação por objeto

- **Implementação:** removido o auxiliar estático `PgCategorias.falhaAoRenomear`. O bloco de tratamento SQL compõe `FalhaNaRenomeacaoCategoria` e lança a `FalhaCategoria` produzida por `traduzida()`.
- **Estrutura e construção:** o novo objeto é final e interno a `categorias.pg`, com dois atributos finais, dois construtores e um método de comportamento. O primário recebe a exceção SQL e o logger. O secundário delega ao primário com o mesmo logger nomeado de `PgCategorias`, preservando o destino dos registros. Não foram introduzidos getters, setters ou novos auxiliares estáticos.
- **Classificação preservada:** somente uma `PSQLException` com estado `23505`, metadados presentes e restrição `ux_categoria_nome` é traduzida como `NOME_DUPLICADO`. Os demais erros continuam como `PERSISTENCIA_INDISPONIVEL`. Em ambos os casos, a causa original é mantida por identidade. Duplicidade não gera registro de indisponibilidade; as demais falhas mantêm o texto e o nível `SEVERE`, incluindo somente o estado SQL. As mensagens HTTP continuam sendo produzidas pelos adaptadores existentes.
- **Testes relevantes:** seis casos de `FalhaNaRenomeacaoCategoriaTest` conferem duplicidade da restrição correta, outra restrição, outro estado SQL, ausência de metadados do PostgreSQL, exceção SQL genérica com estado de duplicidade e exceção sem estado SQL. Verificam motivo, mensagem da exceção de domínio, identidade da causa, ausência de log na duplicidade e texto e severidade do log nas demais falhas, sem anexar a exceção ao registro. Os testes usam um logger local e não alteram a configuração global. A suíte foi registrada como obrigatória. Não foram criados testes de contagem estrutural ou apenas dos construtores.
- **Validação real:** 323 casos em 31 suítes sem banco e 444 em 46 suítes no perfil completo, sem falhas, erros ou ignorados. O completo inclui os unitários e os seis casos novos. Também passaram os testes existentes de duplicidade real, falha de outra restrição, preservação dos dados e respostas HTTP. Comandos iguais aos das etapas anteriores, com Java 17 e Maven 3.9.9. JAR gerado com sucesso.
- **Evidências:** logs `target/revisao-eo/etapa15-falha-categoria-unitarios.log` e `etapa15-falha-categoria-integracao.log`. Relatórios completos em `precificacao-rica/target/surefire-integracao/20261110-202916354`. Registro local em `precificacao-rica/target/validacao-modelagem-etapa15-20261110.json`. Confirmados zero produtos, categorias, cidades, faturas e clientes de apoio ao terminar; PostgreSQL descartável encerrado e removido. Banco de desenvolvimento não utilizado.
- **Escopo e status:** concluída em 10/11/2026. SQL, transações, contratos HTTP e regras de negócio preservados. Nenhuma migração ou alteração no anêmico. Aguardar ok antes da etapa 16.
- **Próxima etapa proposta:** substituir `PgCidades.cidadesDaConsulta` por comportamento de objeto, preservando materialização, ordem, fechamento do resultado SQL e ausência de consultas adicionais. Não iniciada nesta etapa.

### Etapa 16 — Leitura da coleção de cidades

`CidadesDaConsulta` recebe o comando JDBC preparado por `PgCidades`. Seu construtor guarda a dependência. O método `cidades()` executa a consulta, materializa cada linha com `ResultadoSqlDeCidade`, fecha o resultado e devolve uma lista imutável em ordem de código. `PgCidades` continua responsável pelos parâmetros, pelo comando e pela conexão.

Os dois casos de `CidadesDaConsultaTest` conferem execução sob demanda, resultado imutável e fechamento do cursor tanto na leitura bem-sucedida quanto na falha de coluna. Os testes de listagem conferem os filtros e uma consulta por operação.

A validação em 10 de novembro de 2026 passou com **325 testes sem banco em 32 suítes** e **446 na suíte completa em 47 suítes**, sem falhas, erros ou casos ignorados. A suíte completa inclui os unitários. O perfil de integração gerou o JAR e usou PostgreSQL 15 descartável. A execução usou Java 24 com saída e logs em UTF-8 no Windows. Relatórios em `precificacao-rica/target/surefire-integracao/20261110-235528107`.

A etapa 16 está concluída. A etapa 17 propõe revisar as invariantes de `Precificacao` e `ClienteConstante` e aguarda aprovação.

### Etapa 17 — Invariantes e comportamento dos estados de domínio

`Precificacao` protege o valor composto desde a construção. Custo, venda, percentual e modalidade são obrigatórios. A venda é pelo menos igual ao custo e precisa superá-lo quando o percentual é positivo. Na modalidade de margem sobre venda, o percentual é inferior a 100%. As estratégias continuam responsáveis pelas fórmulas e `Precificacoes` conserva a verificação da ordem antes de calcular a taxa a partir de custo e venda. Uma violação no estado final é traduzida em `PrecificacaoInvalida` com a mensagem da regra e sua causa.

`ClienteConstante`, `CategoriaConstante` e `CidadeConstante` representam estados materializados. Exigem identidade positiva e componentes obrigatórios presentes. Categoria e cidade compõem os objetos de código existentes e mantêm `codigo()` com retorno `long`. O endereço ausente do cliente é representado por `Optional.empty()`. `IdentificacaoCliente`, `ContatoCliente` e `Endereco` já protegem suas referências obrigatórias. Os componentes revisados são imutáveis e cada objeto tem até quatro atributos.

Os adaptadores de leitura traduzem as violações das novas invariantes em falha de persistência. A suíte existente conferiu os contratos HTTP e os fluxos com registros válidos. `PrecificacaoTest` e `EntidadesMaterializadasTest` acrescentaram sete casos para as invariantes da construção direta, incluindo limites válidos e ausência explícita de endereço.

Passaram **332 testes sem banco em 34 suítes** e **453 no perfil completo em 49 suítes**, sem falhas, erros ou casos ignorados. O perfil completo inclui os unitários. A execução usou Java 24, Maven 3.9.9 e PostgreSQL 15 descartável. O JAR foi gerado e o servidor temporário removido. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-000157140`.

A etapa 17 está concluída. A etapa 18 trata da composição das condições de validade com Specification e aguarda aprovação.

### Etapa 18 — Specification para validade da precificação

`VendaNaoInferiorAoCusto` e `VendaSuperiorComPercentualPositivo` expressam as condições que relacionam os preços finais. `EspecificacaoDePrecificacao.e(...)` combina as duas condições e informa a primeira violação. `Precificacao` exige a composição na construção. `Precificacoes` reutiliza a ordem dos preços antes de calcular o percentual quando recebe custo e venda.

A composição cobre as duas regras de preço previstas na etapa. `AcrescimoSobreCusto` e `MargemSobreVenda` calculam os valores. O limite de margem sobre venda é aplicado na estratégia e na construção de `Precificacao`.

Os três novos casos verificam a prioridade entre regras violadas, a rejeição antes de calcular o percentual e o limite em que o arredondamento empata a venda com o custo. Os testes também conferem a mensagem e a causa da exceção em cada caminho.

Passaram **335 testes sem banco** e **456 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-000614443`.

A etapa 19 trata da composição dos dados do histórico e aguarda aprovação.

### Etapa 19 — Histórico com composição e até quatro atributos

`AlteracaoPrecificacao` agrupa o código do registro e o código do produto em `Identidade`. A alteração tem quatro componentes. Os objetos de valores anteriores e novos exigem custo, preço de venda e percentual e conservam os decimais lidos do histórico.

`AlteracoesDaConsulta` recebe o comando JDBC preparado e o código do produto. Executa a consulta, materializa a lista imutável e fecha o cursor. `PgHistoricoDePrecificacao` mantém os parâmetros e a ordenação por código decrescente. A representação HTTP produz `codigo`, `codigoProduto`, `dataAlteracao`, `anteriores` e `novos`.

O teste HTTP existente passou a comparar a identidade e a data de cada alteração com o registro no PostgreSQL. Ele também confere os preços anteriores e novos após duas alterações, o histórico vazio e o produto ausente.

Passaram **335 testes sem banco** e **456 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-001051404`.

A etapa 20 trata da leitura da coleção de produtos e aguarda aprovação.

### Etapa 20 — Coleção de produtos por objetos de leitura

`ConsultaSqlDeProdutos` prepara o SQL, aplica os filtros e fornece o comando e o mapeamento de linha a `ProdutosDaConsulta`. O novo objeto executa a consulta, fecha o cursor e entrega uma lista imutável em ordem de código.

A listagem da API usa `PgProdutosConstantes` e reaproveita `ResultadoSqlDeProduto` com os adaptadores de precificação e categoria. A coleção `PgProdutos` também usa o novo objeto de consulta e produz os produtos que leem seus atributos sob demanda.

O teste integrado confere uma conexão e um comando para a resposta da API. Depois de gravar um novo preço no PostgreSQL, o produto materializado conserva o valor anterior e o produto sob demanda obtém o valor atualizado. Uma nova listagem pela API devolve o preço atualizado.

Passaram **335 testes sem banco** e **456 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-001429459`.

A etapa 21 trata dos comandos de alteração de precificação e aguarda aprovação.

### Etapa 21 — Comandos da alteração de precificação

`AtualizacaoDaPrecificacao` grava custo, preço de venda e percentual e materializa a linha devolvida pelo PostgreSQL. `RegistroDoHistoricoDePrecificacao` grava os valores anteriores e os que o banco devolveu. Os dois comandos reutilizam `ParametrosSqlDaPrecificacao` para preencher custo, venda e percentual.

`AlteracaoSqlDePrecificacao` abre a conexão, bloqueia a linha do produto e compara os valores. Quando há mudança, fornece a mesma conexão aos dois comandos e coordena a confirmação ou o desfazimento. Valores iguais encerram a operação após a leitura.

Os testes de reprecificação conferem uma única conexão, as duas gravações, a repetição de valores, o desfazimento após uma falha que alcança o histórico e a atualização concorrente da mesma linha.

Passaram **335 testes sem banco** e **456 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-001938242`.

A etapa 22 trata da validação e das falhas do adaptador de produtos e aguarda aprovação.

### Etapa 22 — Validação e falhas do adaptador de produtos

`CodigoProduto` exige um código positivo antes da abertura da conexão. A consulta individual, a consulta materializada, o vínculo de categoria, a alteração de preço e a leitura do histórico usam o mesmo objeto de valor.

`FalhaNaGravacaoProduto` recebe a exceção SQL original, a indicação de que a gravação inclui categoria e o logger de `PgProdutos`. O motivo `CATEGORIA_AUSENTE` exige uma gravação com categoria e a violação PostgreSQL `23503` da restrição `produto_categoria_codigo_fkey`. As demais falhas são traduzidas para `PERSISTENCIA_INDISPONIVEL`, com a causa original e log no nível `SEVERE` contendo o SQLState.

Os seis novos casos cobrem a restrição exata, a gravação sem categoria, outra restrição, outro estado SQL e a falta de metadados. Os testes com PostgreSQL verificam a remoção da categoria entre validação e gravação e a falha de outra chave estrangeira.

Passaram **341 testes sem banco** e **462 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-002352110`.

A etapa 23 trata da materialização e das consultas de clientes e aguarda aprovação.

### Etapa 23 — Materialização e consultas de clientes

`ResultadoSqlDeCliente` compõe identificação, contato e endereço a partir da mesma linha, usando os valores de domínio existentes. Ele distingue a ausência de uma linha na consulta individual ou no retorno do `UPDATE`. A listagem usa o mesmo mapeamento de cada linha.

`ResultadoSqlDeEndereco` materializa o endereço opcional. O código de cidade ausente representa um cliente sem endereço. O cliente e seus valores ficam disponíveis após o fechamento do cursor.

O teste integrado lê os dados de contato e endereço do cliente materializado e confirma uma conexão e um comando. Também confere consulta e listagem HTTP, leitura atualizada, cliente ausente e falha de materialização. Os testes de atualização conferem o retorno do `UPDATE` e o desfazimento quando a linha atualizada é inválida.

Passaram **341 testes sem banco** e **462 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-002739268`.

A etapa 24 trata dos comandos da atualização parcial de clientes e aguarda aprovação.

### Etapa 24 — Comandos da atualização parcial de clientes

`AtualizacaoDoCliente` recebe a conexão da transação e monta o `UPDATE` a partir de `AlteracaoCliente`. O comando escreve os campos informados. Um endereço presente substitui o conjunto de seis colunas e um endereço explicitamente nulo limpa o conjunto. `ParametrosSqlDoEndereco` preenche os parâmetros do endereço no cadastro e na atualização.

`ExistenciaDeCidade` verifica a referência na conexão da atualização. `ExistenciaDeCliente` também recebe a conexão do chamador e aplica `FOR KEY SHARE` quando o chamador solicita a consulta protegida. `PgClientes` confirma ou desfaz a transação após materializar a linha devolvida pelo `UPDATE`.

Os testes integrados cobrem omissão, remoção, substituição, validação da cidade, escritas conjuntas, reversão após falha e concorrência. O cenário de endereço confere que a validação da cidade e o `UPDATE` compartilham uma conexão.

Passaram **341 testes sem banco** e **462 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-003201884`.

A etapa 25 trata das falhas e da reversão das operações de clientes e aguarda aprovação.

### Etapa 25 — Falhas e reversão das operações de clientes

`CodigoCliente` valida a referência antes de abrir a conexão para consultar ou alterar um cliente. `FalhaNaGravacaoCliente` usa o estado SQL, a restrição e o tipo de gravação para traduzir CPF ou e-mail duplicado e referência de cidade ausente. As demais falhas de gravação retornam indisponibilidade e registram o estado SQL, preservando a exceção original.

`ReversaoDaAlteracaoCliente` recebe a conexão da transação e registra uma eventual falha de rollback. Quando o rollback lança outra exceção, ela fica suprimida na causa original. Se o driver relançar a mesma instância, a exceção original segue intacta.

Os quatro novos casos verificam a classificação conforme a operação, a falta de metadados, o conteúdo do log e a preservação da causa quando o rollback falha. Os testes com PostgreSQL conferem duplicidade, chaves estrangeiras, atomicidade e reversão de alterações.

Passaram **345 testes sem banco** e **466 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-003613620`.

A etapa 26 trata dos comandos de gravação de fatura e itens e aguarda aprovação.

### Etapa 26 — Gravação de fatura e itens por comandos

`CadastroDoCabecalhoDaFatura` grava o desconto contratado e os vínculos opcionais e devolve o código gerado pelo banco. `CadastroDosItensDaFatura` grava em lote, com a quantidade exata e a posição original de cada item.

`PgFaturas` prepara o cálculo, fornece a mesma conexão aos dois comandos e materializa a fatura antes de confirmar a transação. A leitura recupera a data gerada pelo PostgreSQL.

O teste integrado de cadastro confere uma conexão para referências, gravação e retorno. Os testes de falha no segundo item e na materialização verificam o desfazimento do cabeçalho e dos itens. Os testes de simulação verificam a operação de leitura.

Passaram **345 testes sem banco** e **466 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-003942695`.

### Etapa 27 — Classificação de falhas e rollback de faturas

`FalhaNaLeituraDeFatura` traduz falhas técnicas de consulta e materialização. `FalhaNaGravacaoDeFatura` percorre a cadeia JDBC, inclusive em gravações em lote. O SQLState `23503` só indica cliente ou produto ausente quando o nome da restrição corresponde ao vínculo. A exceção recebida continua como causa da falha traduzida.

`ReversaoDaOperacaoDeFatura` usa a conexão da operação de cadastro ou simulação. Se o rollback também falhar, a exceção adicional é registrada como suprimida na falha original. O registro técnico contém o SQLState e, na leitura, o tipo da exceção. A resposta HTTP de indisponibilidade usa a mensagem genérica.

Os oito novos testes conferem a cadeia de erros de lote, os metadados ausentes ou incompatíveis, o diagnóstico de leitura e a preservação da falha original após erro de rollback. Os testes PostgreSQL existentes confirmam 404 para chaves estrangeiras de cliente ou produto violadas após a validação inicial, 503 para falhas técnicas e ausência de gravação parcial.

Passaram **353 testes sem banco** e **474 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-004612280`.

### Etapa 28 — Parâmetros HTTP por objetos de interpretação

`ParametrosDaConsulta` lê a primeira ocorrência de cada parâmetro e decodifica nome e valor em UTF-8. `FiltrosDeCidadesNaUrl`, `FiltrosDeProdutosNaUrl` e `FiltrosDeFaturasNaUrl` compõem os filtros dos seus respectivos domínios. A interpretação mantém o tratamento de espaços, vazios, datas e códigos de cada rota. Os endpoints validam os filtros antes de consultar o banco.

Os testes existentes passaram a conferir que uma ocorrência repetida não substitui a primeira, mesmo quando esta está vazia, em branco ou é inválida conforme a rota. A execução também verificou a decodificação com uma JVM configurada para Windows-1252, a busca literal de caracteres especiais e os resultados dos filtros combinados no PostgreSQL.

Passaram **353 testes sem banco** e **474 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-005803964`.

### Etapa 29 — Entradas de categoria e atualização de cliente

Cadastro e renomeação usam `NomeCategoriaJson` para interpretar o corpo e construir o nome de domínio. A configuração do JSON continua rejeitando campos desconhecidos. No PATCH de cliente, `CampoDeTextoJson` verifica o formato dos campos textuais e compõe os valores e as operações de alteração existentes.

Omissão preserva o valor. E-mail nulo ou em branco continua inválido. Telefone nulo solicita remoção e telefone em branco continua inválido. A validação do formato precede a interpretação dos valores. Os testes acrescentados conferem e-mail em branco e a precedência do erro de formato quando o telefone tem tipo inválido.

Passaram **355 testes sem banco** e **476 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-010219680`.

### Etapa 30 — Composição do pedido e dos filtros de fatura

`JsonDeFatura` reúne a configuração JSON usada no cadastro, na simulação e na listagem. Cadastro e simulação interpretam o mesmo `PedidoFatura`. Cada item compõe a referência ao produto e a quantidade. Os códigos do produto e do cliente reutilizam `InteiroExato` e preservam as mensagens específicas de fatura.

`DataDaConsultaDeFaturas` valida cada limite informado. O filtro mantém o intervalo inclusivo para o usuário e traduz a data final para o início do dia seguinte na consulta SQL.

Os testes do cadastro e da simulação conferem as mensagens de códigos fracionários, fora do limite e iguais a zero antes de qualquer acesso ao banco. A integração verifica itens, descontos, vínculos opcionais, datas extremas e a inclusão de todo o último dia.

Passaram **355 testes sem banco** e **476 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-010709187`.

### Etapa 31 — Representações HTTP e estruturas acima de quatro atributos

As representações de saída compõem dados com responsabilidades próprias. `@JsonUnwrapped` publica os campos desses grupos no mesmo nível do JSON.

| Representação | Solução |
| --- | --- |
| `ProdutoResposta` | Compõe a precificação e mantém a categoria opcional. O construtor consulta a precificação uma vez e substitui `dadosDoProduto`. |
| `AlteracaoResposta` | Compõe a identidade do histórico e preserva os objetos `anteriores` e `novos`. |
| `ClienteResposta` | Compõe identificação, contato e endereço. Telefone e endereço ausentes continuam explícitos como `null`. |
| `EnderecoResposta` | Compõe a localização dentro do endereço, incluindo o complemento opcional. |
| `FaturaResposta` | Compõe valores e vínculos. A simulação reaproveita a representação de valores. |
| `ResumoResposta` | Reaproveita os vínculos da fatura. Os vínculos ausentes são omitidos nas duas respostas. |
| `TkCadastroProduto.Cadastro` | Mantém seis campos do JSON plano de entrada e compõe o nome, a categoria e a precificação pelos objetos existentes. |
| `TkCadastroCliente.Cadastro` | Mantém cinco campos do JSON plano de entrada e compõe identificação, contato e endereço opcional. |
| `EnderecoJson` | Mantém seis campos do objeto de entrada e reaproveita a composição existente de CEP, localização e código de cidade no cadastro e no PATCH. |

Os três DTOs de entrada preservam os campos usados pelo Jackson para receber o JSON. A interpretação produz objetos do domínio e as validações permanecem nos tipos responsáveis por elas.

Passaram **355 testes sem banco** e **476 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-011209638`. O executor da coleção Postman comparou **150 pares de respostas HTTP** dos dois módulos em dois bancos temporários. A execução usou Node.js 20 com o reaproveitamento de conexões HTTP desativado no executor após um reset de conexão. O relatório está em `precificacao-rica/target/comparacao-modelagem-etapa-31.json`.

### Etapa 32 — Configuração e preparação do banco por composição

`ConfiguracaoPostgres` interpreta as variáveis de ambiente e compõe credenciais, endereço e opção de criação. `OpcaoCriacaoBanco` aceita `true` ou `false`, com espaços nas extremidades e variação de maiúsculas. `UrlPostgres` compõe a URL de manutenção a partir do endereço JDBC. `NomeBancoPostgres` valida o limite em bytes e aplica o escape necessário no identificador SQL.

`BancoPostgres` usa a mesma operação para verificar a conexão com criação automática ligada ou desligada. O acesso de manutenção fica condicionado ao erro de banco ausente e à opção de criação. As migrações ocorrem antes de a API aceitar requisições.

Os testes cobrem interpretação da opção, preservação das URLs, autenticação, permissões, criação concorrente e falha de migração. Um cenário com um banco existente que recusa conexões confirma a preservação da causa e o diagnóstico de conexão. Os diagnósticos da preparação registram o SQLState e a mensagem pública.

Passaram **357 testes sem banco** e **479 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-011907042`.

### Etapa 33 — Montagem da aplicação e revisão dos construtores

`RotasDaAplicacaoRica` compõe as coleções e os adaptadores HTTP. O caminho principal fornece as categorias à coleção de produtos, as cidades à coleção de clientes e as consultas de clientes e produtos à coleção de faturas. `RotaPorMetodo` devolve 405 com `Allow` quando a operação solicitada é incompatível com o caminho. O `main` prepara o banco, abre o servidor e usa a mesma montagem de rotas exercitada pelos testes.

| Elemento revisado | Decisão |
| --- | --- |
| `Precificacoes` e estratégias | As três combinações de entrada compartilham a mesma validação e a estratégia recebida pelo construtor. As estratégias de acréscimo e margem têm fórmulas e contratos próprios. |
| `CalculoFatura` | O construtor valida a composição e copia os itens. A leitura dos preços acontece ao calcular ou preparar a fatura. |
| Coleções PostgreSQL | A montagem principal fornece os colaboradores explícitos. Os construtores secundários preservam as formas de construção usadas por testes e outros chamadores. A consulta ocorre nas operações das coleções. |
| `ProdutoConstante` e `DescontoReconstituido` | O produto mantém os valores materializados e delega a identidade. O desconto delega cálculo, validação e registro à estratégia composta. |
| Adaptadores HTTP | Os construtores configuram a serialização e recebem as coleções. Os DTOs planos de entrada descritos na etapa 31 preservam o mapeamento do Jackson. |

Os 13 casos de `RotasDaAplicacaoRicaTest` usam uma fonte de dados que rejeita qualquer conexão. Os casos verificam 405 e o cabeçalho `Allow` para cada rota, incluindo a precedência do caminho literal de simulação. O inventário de métodos estáticos de produção aponta o `main` como único método declarado dessa forma.

Passaram **370 testes sem banco** e **492 na suíte completa com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. O JAR foi gerado. Relatórios em `precificacao-rica/target/surefire-integracao/20261111-012535762`.

### Etapa 34 — Validação final e atualização das evidências

O inventário do JAR confirmou o `main` como único método estático próprio e três DTOs de entrada acima de quatro atributos. A revisão consolidou as verificações repetidas de 405 e `Allow` em `RotasDaAplicacaoRicaTest`. Os testes de validação dos pedidos, cálculo, persistência e falhas mantêm suas responsabilidades.

Passaram **369 testes sem banco** e **491 no perfil completo com PostgreSQL 15 descartável**, sem falhas, erros ou casos ignorados. A coleção foi executada por HTTP em duas rodadas por módulo, com **150 pares de respostas comparados**. Os relatórios, exemplos para o estudo e o alcance da validação estão no [registro final](Validacao-Equivalencia-Modelos.md#validacao-final-modelagem).

<a id="proximas-etapas-modelagem"></a>

## 13. Critérios usados na revisão de modelagem

As 34 etapas da rodada anterior estão concluídas. As evidências desse fechamento estão no [registro final](Validacao-Equivalencia-Modelos.md#validacao-final-modelagem). A seção 14 registra a execução posterior à limpeza dos testes e as decisões complementares de modelagem.

- A entrega preserva as regras da seção 3 e a separação entre domínio, HTTP e PostgreSQL.
- Os novos objetos têm uma responsabilidade explicável, estado final e até quatro atributos; os construtores recebem valores ou colaboradores e delegam a inicialização.
- A transformação de uma estrutura em objeto rico protege uma regra ou executa uma operação necessária. Não basta renomear um getter, encapsular uma função sem contexto ou criar interfaces sem uso.
- Os registros imutáveis de leitura conservam seus valores, e novas operações recebem dados atuais quando esse é o contrato. Recursos JDBC não escapam para o domínio.
- Os testes verificam comportamento e riscos reais, com causa e efeito observáveis. A quantidade de testes pode permanecer igual após uma refatoração bem coberta.
- O registro distingue implementação, verificação e planejamento. Uma etapa pendente só muda de status depois de autorizada, executada e validada.

## 14. Revisões e decisões complementares de 11 de novembro de 2026

### 14.1. Revisão dos testes dos dois módulos

A revisão abrangeu as 62 suítes JUnit dos módulos anêmico e rico. Foram removidos 11 casos redundantes, sendo dois do anêmico e nove do rico. As verificações de regras de negócio, precisão, rollback, concorrência e contratos HTTP foram preservadas.

| Conjunto | Ajuste realizado |
| --- | --- |
| Roteamento HTTP | A verificação de PATCH incompatível na consulta de fatura foi concentrada em RotasDaAplicacaoRicaTest. |
| Construção PostgreSQL | Foi removida uma verificação de construção sem acesso ao banco já exercitada por outro cenário com DataSourceSemConexao. |
| Leitura de produtos | A consulta pela interface Produtos, a leitura de acentos e apóstrofo e a atualização observada pelo mesmo objeto foram reunidas em um cenário. |
| Itens e cálculo da fatura | Foram removidas verificações repetidas de construção sem consulta e precisão fracionária já cobertas por CalculoFaturaTest. |
| Precificação rica | Foram retiradas entradas repetidas de valores inválidos, arredondamento e venda inferior ao custo. Os cenários mais específicos conservam a verificação das mensagens e da ordem de validação. |
| Precificação anêmica | Foram retirados dois parâmetros redundantes. Os testes restantes verificam o limite da margem e a preservação dos dados originais quando o cálculo é rejeitado. |

A execução após a revisão apresentou os resultados abaixo.

| Módulo | Testes sem banco | Suíte completa com PostgreSQL | Suítes na execução completa |
| --- | --- | --- | --- |
| precificacao-anemica | 82 | 157 | 9 |
| precificacao-rica | 362 | 482 | 53 |

Todos passaram, com zero falhas, erros ou casos ignorados. A suíte completa inclui os testes sem banco. A execução usou PostgreSQL 15.1 descartável e gerou os relatórios em target/surefire-integracao/20261111-013754183 dentro de cada módulo.

Os números registrados nas etapas anteriores correspondem às respectivas execuções históricas. A execução descrita aqui é posterior à remoção dos casos redundantes.

### 14.2. Análise complementar de Elegant Objects

A revisão estática conferiu os métodos estáticos próprios, atributos, construtores, contratos e objetos usados na modelagem. O inventário do código de produção encontrou o main como único método estático próprio declarado. Os atributos de instância analisados são finais. A aplicação usa Strategy, Specification, composição e delegação.

| Aspecto | Situação observada | Decisão ou encaminhamento |
| --- | --- | --- |
| Validação nos construtores | CalculoFatura verifica a composição e os produtos repetidos. Dinheiro, Percentual e outros valores protegem suas invariantes na construção. | O autor decidiu conservar essa abordagem. Essa decisão deve ser considerada ao descrever a relação com o capítulo sobre construtores sem processamento. |
| Leitura JDBC na construção | ResultadoSqlDeCategoria, CategoriaDaLinha e ResultadoSqlDeCidade leem colunas e interpretam valores em construtores secundários. | Achado registrado para avaliação futura, considerando o tempo de vida do ResultSet e a tradução das falhas. |
| Tamanho dos contratos | Fatura declara nove métodos e Produtos declara sete, incluindo conveniências e sobrecargas. | Avaliar responsabilidades e necessidade das conveniências em uma revisão futura. |
| Contratos de comportamento | RegraDePrecificacao e Desconto permitem alternativas polimórficas. Precificacoes e CalculoFatura expõem operações por tipos concretos. | Avaliar interfaces quando houver uma necessidade concreta de substituição ou decoração do comportamento. |
| Construtores primários e secundários | PgProdutos e DescontoReconstituido oferecem formas de construção que delegam ao primário. A relação entre quantidade de construtores e métodos varia. | Avaliar a utilidade de cada forma de construção e a centralização da inicialização. |
| Representação e encapsulamento | ProdutoConstante e os records oferecem acesso aos valores materializados. | Avaliar a responsabilidade de cada tipo e o comportamento exigido pelos consumidores. |

A conclusão das 34 etapas documenta as entregas e as verificações daquela rodada. A análise complementar explicita as escolhas que ainda diferem de uma aplicação estrita das recomendações do livro.

As orientações do autor consultadas foram [construtor primário](https://www.yegor256.com/2015/05/28/one-primary-constructor.html), [construtores sem processamento](https://www.yegor256.com/2015/05/07/ctors-must-be-code-free.html), [interfaces e composição](https://www.yegor256.com/2014/11/20/seven-virtues-of-good-object.html) e [critérios de tamanho dos objetos](https://www.yegor256.com/2017/02/28/too-many-classes.html).

### 14.3. Records com corpo vazio

AlteracaoPrecificacao.Identidade agrupa codigo e codigoProduto. O compilador gera o construtor canônico, os campos finais, os acessores, equals, hashCode e toString. Esse record possui corpo vazio e aceita os códigos recebidos sem validação própria.

Os records de entrada e saída HTTP representam o formato dos dados trocados com o cliente. Os records do domínio devem ser avaliados pelo significado dos valores, pelas invariantes e pelo comportamento necessário. O agrupamento de campos precisa ter uma responsabilidade justificável.

A conversa registrou essa avaliação como um ponto de revisão futura. A decisão vigente conserva os construtores e suas validações atuais.

### 14.4. Coleções Postman e comparação de equivalência

As coleções disponíveis estão em documentacao/postman.

| Coleção | Pastas | Requisições | baseUrl padrão |
| --- | --- | --- | --- |
| precificacao-anemica.postman_collection.json | 5 | 59 | http://localhost:8080 |
| precificacao-rica.postman_collection.json | 6 | 75 | http://localhost:8081 |

Os arquivos já contêm corpos JSON, verificações e captura dos códigos retornados. Cada coleção mantém suas próprias variáveis. A execução completa começa pela primeira requisição de preparação e segue a ordem das pastas. As requisições criam e alteram registros de exemplo.

A sexta pasta da coleção rica acrescenta cenários de precisão, preços atuais e vínculos integrados. A equiparação das duas coleções foi sugerida e permanece pendente de implementação. As orientações de importação foram fornecidas ao autor, que apresentou as duas coleções carregadas no Postman.

O executor verificacao/equivalencia.js usa a coleção rica nas duas APIs. Ele envia as requisições por HTTP, executa as verificações suportadas e compara status, campos, valores e ordenação. A comparação associa os códigos gerados aos mesmos cadastros e valida o formato das datas geradas antes de normalizá-las. Diferenças textuais das respostas de erro ficam registradas no relatório.

O comparador depende de duas APIs em execução e de bancos vazios destinados à comparação. Seu alcance é o conjunto de cenários executados. As instruções de uso estão no [guia da comparação](../verificacao/README.md). A última comparação HTTP registrada continua sendo a execução da etapa 34, com 150 pares em duas rodadas por módulo.

### 14.5. Próximos passos registrados

| Ordem | Trabalho | Situação |
| --- | --- | --- |
| 1 | Conservar as validações atuais nos construtores, conforme decisão do autor. | Decisão definida. |
| 2 | Avaliar records do domínio e a utilidade dos agrupamentos de valores. | Revisão sugerida. |
| 3 | Avaliar tamanho das interfaces, abstrações e leitores JDBC apontados na análise complementar. | Revisão sugerida. |
| 4 | Equiparar os cenários das coleções Postman, incluindo os casos adicionais no arquivo do anêmico. | Melhoria sugerida. |

Cada implementação deve manter as regras dos dois modelos e terminar com revisão do código e verificação dos comportamentos afetados. Os testes devem demonstrar resultados observáveis e riscos concretos.
