# Implementação e validação do plano do modelo anêmico

Registro de 9 de novembro de 2026. Referência: `Documentacao-e-Plano-Precificacao.md`, enviado pelo autor do projeto.

Foram concluídas as nove etapas detalhadas para o modelo anêmico. As etapas de código foram implementadas em sequência, com compilação e revisão ao final de cada uma. Os cenários HTTP e de persistência foram executados em conjunto na etapa 8.

## Entregas e validação inicial

A tabela registra a primeira entrega. As revisões detalhadas e as execuções posteriores estão descritas nas seções seguintes.

| Passo | Entrega | Compilação e revisão | Validação em execução |
|---|---|---|---|
| 1 | `GET /api/clientes/{codigo}`. | Compilação aprovada. SQL parametrizado e mapeamento do cadastro reutilizado. Controller delega ao service. | Clientes com e sem endereço, listagem preservada, códigos inválidos com `400` e cliente ausente com `404`. |
| 2 | `PATCH /api/clientes/{codigo}` com e-mail. | Compilação aprovada. DTO registra a presença do campo; cadastro e atualização compartilham a validação do e-mail. | E-mail atualizado e consultado; e-mail nulo, inválido ou duplicado recusado; unicidade sem distinguir capitalização; dados preservados nas rejeições. |
| 3 | Alteração ou remoção do telefone pelo mesmo PATCH. | Compilação aprovada. Regra do cadastro reutilizada; leitura com `FOR UPDATE` e gravação na mesma transação. | Telefone omitido, preenchido e nulo; atualização conjunta; telefone inválido impede também a alteração do e-mail; dois PATCHs simultâneos preservam os campos omitidos. |
| 4 | Substituição ou remoção do endereço. | Compilação aprovada. Mesmo DTO de endereço e mesma preparação do cadastro; cidade validada antes de gravar. | Endereço completo substituído, cidade trocada, dados removidos em todas as colunas; campos incompletos e cidade ausente recusados; contatos e vínculo da fatura preservados. |
| 5 | Filtro opcional de cidade por nome. | Compilação aprovada. `STRPOS(LOWER(nome), LOWER(?))` com parâmetro SQL e ordenação por código. | Trechos em maiúsculas e minúsculas, espaços nas pontas, filtro em branco, lista vazia e busca literal de `%`, `_` e conteúdo semelhante a SQL. |
| 6 | Filtro opcional de UF, combinado com nome. | Compilação aprovada. Mesma validação de UF do cadastro; condições combinadas com `AND`. | UF em minúsculas ou com espaços normalizada, UF inválida com `400`, filtros combinados e código encontrado usado no endereço de cliente. |
| 7 | `PATCH /api/categorias/{codigo}` para renomear. | Compilação aprovada. Cadastro e renomeação compartilham a validação; SQL atualiza somente o nome. | Código preservado, conflito de nome com `400`, ausente com `404`, produto continua vinculado e histórico permanece íntegro. |
| 8 | Integração em PostgreSQL separado. | Testes compilados; Flyway aplicou V1 a V8 no servidor temporário. | 121 testes aprovados, sem falhas, erros ou testes ignorados. |
| 9 | README e coleção Postman atualizados. | Exemplos comparados com os contratos executados. Coleção v2.1 lida pelo Newman. | 57 requisições e 77 verificações aprovadas, sem falhas. |

Nas etapas 1 a 7, a revisão foi feita no código e na compilação; os resultados HTTP da última coluna foram confirmados na etapa 8. A revisão conferiu a separação de responsabilidades, o uso de parâmetros SQL, a reutilização das validações e a preservação das restrições de unicidade e dos vínculos existentes.

A atualização de cliente valida todos os campos enviados antes da gravação e mantém o bloqueio da linha até o término da transação. Nome, CPF e código permanecem os mesmos. Os conflitos de unicidade das atualizações são traduzidos em erro de validação; outras falhas de acesso a dados continuam sob o tratamento de persistência existente.

## Correção após a revisão da etapa 2

A revisão identificou que um PATCH contendo somente e-mail também incluía as colunas de telefone e endereço no SQL, regravando os valores atuais. O repositório passou a receber os campos selecionados pelo serviço e a montar o `UPDATE` somente com as colunas correspondentes.

Foram aplicados os critérios gerais da auditoria de referência do SGF, considerando as bibliotecas e a arquitetura deste projeto:

- Seleção de campos representada por `Set<CampoCliente>`, mantendo o DTO HTTP fora do repositório.
- Nomes de colunas fixos no código e valores passados como parâmetros SQL.
- Validação e transação mantidas no serviço; construção do comando e mapeamento no repositório.
- Cláusulas de guarda, condições sem aninhamento e preparação dos valores do endereço reutilizada entre cadastro e atualização.
- Tratamento de duplicidade preservado, com outras falhas de persistência seguindo para a resposta genérica `503`.

O teste `atualizacaoSomenteDoEmailNaoIncluiOutrasColunasNoComando` confirmou o defeito antes da correção e passou depois dela. Ele utiliza um gatilho de teste por coluna, que detecta a presença de colunas omitidas no comando mesmo quando o valor regravado seria igual ao anterior. Esse comportamento de `UPDATE OF` está descrito na [documentação do PostgreSQL](https://www.postgresql.org/docs/16/sql-createtrigger.html). A função e o gatilho são removidos ao final do teste.

Também foi acrescentado `ClienteControllerTest` para verificar o retorno `503` em uma falha de persistência. Após a correção, a execução completa do módulo aprovou **123 testes, sem falhas, erros ou casos ignorados**, em PostgreSQL 16.4 temporário e exclusivo. Desses, 58 estão em `CadastrosPostgresTest` e um em `ClienteControllerTest`. Essa execução verifica regressões do ajuste compartilhado do PATCH; a revisão detalhada das demais etapas segue separadamente.

## Revisão detalhada da etapa 3

A atualização e a remoção de telefone foram revisadas quanto ao contrato HTTP, tratamento de nulos, tipagem, separação de responsabilidades, legibilidade, SQL e transações. Não foram encontrados novos problemas que exigissem correção nessa etapa.

O DTO distingue telefone omitido de telefone nulo. O serviço reutiliza a validação do cadastro, limita o texto ao tamanho da coluna e confere todos os campos antes de gravar. O repositório inclui `telefone` no comando somente quando enviado e usa parâmetro SQL para seu valor. A leitura com bloqueio e a atualização permanecem na mesma transação.

Foram conferidos os seis casos relacionados no relatório da execução anterior de 123 testes: troca e remoção do telefone com preservação de campos omitidos, quatro entradas inválidas que impedem também a alteração do e-mail e atualização concorrente de contatos. Todos estavam aprovados. Texto em branco mantém o tratamento opcional do cadastro, conforme já documentado no README.

## Revisão detalhada da etapa 4

A substituição e a remoção do endereço foram revisadas quanto ao contrato, validação compartilhada, nulabilidade, conversão do código da cidade, limites dos campos, organização do código, SQL e transações. Não foram encontrados novos problemas que exigissem correção nessa etapa.

O campo omitido preserva o endereço atual; um objeto substitui o endereço inteiro; `null` limpa as seis colunas, inclusive complemento e cidade. Cadastro e atualização usam a mesma preparação. Os cinco campos obrigatórios e a existência da cidade são conferidos antes da gravação; o complemento permanece opcional. O SQL usa parâmetros e trata as colunas do endereço em conjunto. Código do cliente e vínculos de faturas são preservados.

Foram conferidos oito casos relacionados no relatório da execução anterior de 123 testes: troca e remoção com manutenção do vínculo da fatura, cinco casos de endereço incompleto, cidade ausente ou código inválido e preservação do endereço quando apenas e-mail é enviado. Todos estavam aprovados. A revisão de código confirmou funções com responsabilidades definidas, cláusulas de guarda e tratamento explícito de valores nulos, sem necessidade de bibliotecas do SGF.

## Revisão detalhada da etapa 5

O filtro de cidades por nome foi revisado quanto ao contrato HTTP, tratamento de filtro ausente ou em branco, nulabilidade, limites da entrada, separação de responsabilidades, legibilidade e segurança do SQL. Não foram encontrados problemas que exigissem correção nessa etapa.

O controller recebe o parâmetro opcional e delega ao serviço. O serviço remove espaços nas pontas, trata branco como ausência e rejeita texto acima do limite ou com caractere nulo. O repositório usa uma única consulta parametrizada com `STRPOS(LOWER(nome), LOWER(?))`, no mesmo padrão da listagem de produtos. A busca ignora capitalização, interpreta `%` e `_` como texto literal e mantém a ordenação por código. A consulta sem filtro preserva a listagem anterior.

Foram conferidos os dois testes relacionados no relatório da execução anterior de 123 testes. Eles cobrem busca por trecho com capitalização diferente e espaços nas pontas, ausência de correspondências, filtro em branco, listagem completa ordenada, caracteres `%` e `_` e conteúdo semelhante a SQL. Ambos estavam aprovados; buscas sem correspondências retornaram `200` com `[]`.

## Revisão detalhada da etapa 6

O filtro por UF foi revisado quanto ao contrato, reutilização das regras do cadastro, nulabilidade, normalização, tipagem, combinação dos parâmetros e segurança do SQL. Não foram encontrados problemas que exigissem correção nessa etapa.

Cadastro e filtro reutilizam `validarUf`, com remoção de espaços e conversão para maiúsculas usando `Locale.ROOT`. Na consulta, UF ausente ou em branco é tratada como filtro ausente. Valores inválidos são rejeitados antes do acesso ao repositório. O repositório recebe `UnidadeFederativa`, usa parâmetro SQL e combina nome e UF com `AND`, mantendo uma única consulta e ordenação por código. Os 27 valores do enum coincidem com os da restrição existente no banco.

Foram conferidos cinco casos relacionados no relatório da execução anterior de 123 testes: combinação de nome e UF com uso do código retornado no endereço de cliente e quatro filtros de UF inválidos. O cenário combinado também verifica UF em minúsculas, espaços nas pontas e filtro em branco. Todos estavam aprovados. A revisão de código confirmou responsabilidades separadas e normalização compartilhada, sem validações duplicadas ou consultas repetidas.

## Revisão detalhada da etapa 7

A renomeação de categoria foi revisada quanto ao contrato HTTP, validações compartilhadas, nulabilidade, tamanho do nome, tratamento de duplicidade, integridade dos vínculos, organização do código e SQL. Não foram encontrados desvios funcionais. A duplicação do mapeamento do resultado SQL em cadastro, listagem e renomeação foi removida, centralizando-o no método privado `mapear` de `JdbcCategoriaRepository`, conforme o padrão dos outros repositórios JDBC.

Cadastro e renomeação reutilizam `validarNome`. O comando parametrizado atualiza somente `nome`, preservando o código da categoria e os vínculos dos produtos. A atualização é feita em uma única instrução, sem consulta prévia de existência. O resultado vazio gera `404`; a violação do índice único de nome, que ignora capitalização, gera `400`. Outras falhas de persistência continuam sob o tratamento existente. O histórico de precificação permanece independente dessa alteração.

Após o ajuste, foram executados sete casos por HTTP em um PostgreSQL 16.4 temporário e separado: renomeação com produto vinculado e histórico, cinco corpos inválidos e distinção entre categoria ausente e código inválido. O cenário principal também passou a conferir o nome atualizado em `GET /api/categorias`, exercitando os três usos do mapeamento compartilhado. Todos os sete casos passaram, sem falhas, erros ou casos ignorados. O relatório específico está em `precificacao-anemica/target/surefire-reports/TEST-br.com.precificacao.anemico.CadastrosPostgresTest-revisao-etapa7.xml`.

## Revisão detalhada da etapa 8

A suíte completa do modelo anêmico foi executada novamente após os ajustes das etapas 2 e 7, usando JDK 17.0.12, Maven 3.9.9 e PostgreSQL 16.4 em container temporário, com banco exclusivo `precificacao_etapa8_teste`. Todas as suítes de integração foram habilitadas. A revisão de código conferiu a configuração das conexões de teste, o fechamento dos contextos, os limites de espera e as verificações do estado persistido depois de requisições rejeitadas.

O cenário de fatura sem campos opcionais foi ampliado para comparar os valores da simulação com os do cadastro, confirmar que a simulação não insere fatura e conferir a ausência de cliente e pagamento na listagem. Os cenários anteriores de consulta, alteração de contatos e endereço, filtros, renomeação de categoria, manutenção dos vínculos, histórico, concorrência e descontos também foram executados. Não foram encontradas novas falhas de produção nessa validação.

| Suíte | Casos | Falhas / erros / ignorados |
|---|---:|---:|
| `AplicacaoPostgresTest` | 1 | 0 / 0 / 0 |
| `CadastrosPostgresTest` | 58 | 0 / 0 / 0 |
| `ClienteControllerTest` | 1 | 0 / 0 / 0 |
| `ProdutoControllerTest` | 7 | 0 / 0 / 0 |
| `CriacaoBancoPostgresTest` | 5 | 0 / 0 / 0 |
| `EnderecoBancoPostgresTest` | 16 | 0 / 0 / 0 |
| `PostgresProdutoRepositoryTest` | 1 | 0 / 0 / 0 |
| `PrecificacaoServiceTest` | 34 | 0 / 0 / 0 |
| **Total** | **123** | **0 / 0 / 0** |

Uma consulta adicional, em transação somente de leitura no servidor temporário, confirmou as oito tabelas de `public`, incluindo `flyway_schema_history`, as migrações V1 a V8 com sucesso, as seis chaves estrangeiras validadas e a remoção do gatilho temporário usado no teste de atualização parcial. O servidor temporário foi encerrado ao final da revisão.

Os resultados desta execução estão em `precificacao-anemica/target/surefire-reports/TEST-*-revisao-etapa8.xml` e o log em `precificacao-anemica/target/revisao-etapa8.log`. Os resultados das execuções anteriores permanecem separados. A contagem da tabela inclui testes unitários e testes de integração com PostgreSQL; todos foram executados nessa rodada.

## Revisão detalhada da etapa 9

O README e a coleção foram comparados com os controllers, DTOs e fluxos validados na etapa 8. Foram conferidos métodos HTTP, rotas, parâmetros, nomes dos campos, tipos dos valores, códigos de resposta e dependências entre as requisições. Os nove links locais da documentação foram verificados e nenhum estava quebrado. Os exemplos usam dados de demonstração e referências a variáveis; o arquivo local de credenciais permanece ignorado pelo Git.

O README passou a distinguir o repositório em memória dos mocks dos testes, a explicar a ordem entre simulação e cadastro da fatura e a documentar as variáveis usadas no fluxo. A coleção passou a declarar e limpar `totalSimulado`, a guardar um código próprio para a fatura sem campos opcionais e a incluir sua consulta e identificação na listagem. O exemplo de cidade inexistente também foi padronizado com um código inteiro literal. A coleção final possui cinco pastas, 59 requisições e 14 variáveis. A sintaxe dos 60 scripts foi conferida e não foram encontradas referências de template a variáveis não declaradas.

A importação e a execução da coleção v2.1 foram verificadas com Newman 6.2.1 e Node.js 22.22.3. Foram realizadas duas iterações completas contra uma instância temporária da API conectada ao banco exclusivo `precificacao_etapa9_teste`, em PostgreSQL 16.4. Passaram as 118 requisições e as 162 verificações, sem falhas. A segunda iteração reutilizou os códigos retornados pelos novos cadastros, com dados de preparação renovados.

O relatório desta execução está em `precificacao-anemica/target/newman-revisao-etapa9.json` e o log em `precificacao-anemica/target/newman-revisao-etapa9.log`. A API e o PostgreSQL temporários foram encerrados ao final da revisão.

## Ambiente e testes da execução inicial

- JDK 17.0.12 e Maven 3.9.9.
- PostgreSQL 16.4 em container temporário, publicado somente em `127.0.0.1`, em porta aleatória.
- Banco exclusivo `precificacao_plano_teste`, com usuário e senha de teste.
- Servidores HTTP iniciados em portas aleatórias, separados da instância local de desenvolvimento.
- Migrações existentes V1 a V8, sem alteração dos scripts já aplicados.

| Classe de teste | Casos executados | Falhas / erros / ignorados |
|---|---:|---:|
| `AplicacaoPostgresTest` | 1 | 0 / 0 / 0 |
| `CadastrosPostgresTest` | 57 | 0 / 0 / 0 |
| `ProdutoControllerTest` | 7 | 0 / 0 / 0 |
| `CriacaoBancoPostgresTest` | 5 | 0 / 0 / 0 |
| `EnderecoBancoPostgresTest` | 16 | 0 / 0 / 0 |
| `PostgresProdutoRepositoryTest` | 1 | 0 / 0 / 0 |
| `PrecificacaoServiceTest` | 34 | 0 / 0 / 0 |
| **Total** | **121** | **0 / 0 / 0** |

Além dos critérios dos passos 1 a 7, a integração confirmou:

- Cadastro de cidade e cliente, consulta, alteração e manutenção do vínculo de faturas com o mesmo cliente.
- Faturas com cliente e cada uma das cinco formas de pagamento; consulta e filtro por cliente.
- Simulação com o mesmo total do cadastro, sem inserir fatura; cadastro e consulta com campos opcionais omitidos.
- Produto vinculado a categoria renomeada; alteração da precificação preservando a categoria; repetição dos mesmos valores sem acrescentar histórico.
- Desconto fixo limitado ao subtotal após redução do preço e retomada do valor original quando o preço aumenta.
- Dados consultados novamente após requisições recusadas, sem alterações parciais.

Os testes novos estão em `precificacao-anemica/src/test/java/br/com/precificacao/anemico/CadastrosPostgresTest.java`. A configuração para reproduzi-los está na seção de testes do [README do módulo](../precificacao-anemica/README.md#testes). O PostgreSQL usado na validação foi exclusivo de testes.

## Coleção Postman

A [coleção atualizada](postman/precificacao-anemica.postman_collection.json) contém cinco pastas e 59 requisições. A primeira prepara identificadores, CPFs e e-mails de exemplo para a execução; os demais cadastros e filtros guardam e reutilizam os códigos retornados. Os valores preenchidos durante o fluxo são reiniciados na preparação da execução seguinte.

A coleção foi importada e executada com Newman 6.2.1 em Node.js 22.22.3 contra a API ligada ao PostgreSQL temporário. Na revisão final, passaram duas iterações, totalizando 118 requisições e 162 verificações, incluindo as respostas `400` e `404` esperadas. O relatório está em `precificacao-anemica/target/newman-revisao-etapa9.json`; os relatórios JUnit ficam em `precificacao-anemica/target/surefire-reports`. Esses arquivos de execução ficam fora do versionamento.

Para repetir a coleção com uma API local já iniciada, use Node.js 16 ou superior e execute na raiz do projeto:

```bash
npx --yes newman@6.2.1 run documentacao/postman/precificacao-anemica.postman_collection.json --env-var 'baseUrl=http://localhost:8080' --bail
```

Também é possível importá-la no aplicativo Postman e executar as pastas em ordem pelo Collection Runner. A coleção usa apenas dados de exemplo e não contém senha de banco. O uso do runner segue a [documentação do Newman](https://learning.postman.com/docs/collections/using-newman-cli/installing-running-newman).
