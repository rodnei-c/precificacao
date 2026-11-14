# Precificação anêmica

API HTTP de produtos, precificação, faturas, categorias, cidades e clientes, com Java 17, Spring Boot 4.1.2, JDBC, PostgreSQL e Flyway.

## Responsabilidades

- Controller: recebe JSON, lê o código da URL e define a resposta HTTP.
- Service: valida os dados, executa as regras de negócio e coordena as transações.
- Repository: executa SQL parametrizado e mapeia os registros do banco.
- Model: mantém dados, construtores e getters/setters, sem regras de negócio.

Os contratos dos serviços ficam em `services`, e suas implementações em `services/impl`: `ProdutoServiceImpl`, `PrecificacaoServiceImpl`, `FaturaServiceImpl`, `CategoriaServiceImpl`, `CidadeServiceImpl` e `ClienteServiceImpl`. Os controllers recebem as interfaces dos serviços.

As interfaces de repositório ficam em `repositories`. As implementações JDBC em `repositories/impl` executam o acesso ao PostgreSQL.

## Banco de dados

As instruções abaixo são executadas na raiz do repositório, em PowerShell.

Se precisar de um PostgreSQL local e tiver Docker:

~~~powershell
$env:DB_PASSWORD = "defina-uma-senha-local"
docker compose up -d postgres
~~~

O Compose cria o banco precificacao_anemica e mantém seus dados em um volume. Usa a porta local 5432; se ela já estiver ocupada, ajuste a publicação em compose.yml e a variável DB_URL da aplicação.

Se utilizar um PostgreSQL existente, configure a conexão. A aplicação cria o banco indicado em DB_URL quando ele ainda não existe:

~~~powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/precificacao_anemica"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "senha-do-banco"
~~~

A aplicação lê essas variáveis do processo que a inicia. Para manter a senha entre execuções locais, crie `precificacao-anemica/.env.anemico` com uma linha `DB_PASSWORD=sua-senha-local`. O módulo carrega esse arquivo opcional, que o Git ignora. Ao executar na raiz do repositório, também aceita `.env.anemico` na raiz; se os dois arquivos existirem, o arquivo da pasta do módulo tem prioridade. As variáveis de ambiente têm prioridade sobre os arquivos.

No IntelliJ, execute `AplicacaoAnemica` com o diretório de trabalho na raiz do repositório ou na pasta `precificacao-anemica`. O arquivo da pasta do módulo é encontrado nos dois casos.

Ao iniciar, a aplicação tenta conectar ao banco configurado. Se o PostgreSQL informar que ele não existe, a aplicação conecta ao banco de manutenção `postgres` no mesmo servidor e cria o banco com o mesmo usuário e senha. O usuário precisa ser superusuário ou possuir a permissão `CREATEDB` para essa primeira criação. Bancos existentes são preservados e não exigem essa permissão adicional. Falhas de senha ou de conexão interrompem a inicialização com uma mensagem de erro.

Depois, o Flyway aplica as migrações em `src/main/resources/db/migration`, que criam as tabelas `produto`, `fatura`, `item_fatura`, `historico_precificacao`, `categoria`, `cidade` e `cliente`. O servidor PostgreSQL precisa estar instalado e em execução; a aplicação cria o banco e as tabelas dentro dele.

A criação automática fica habilitada por padrão. `DB_CREATE_ENABLED=false` permite desativá-la, mantendo as migrações normais do Flyway. `DB_MAINTENANCE_DATABASE` permite escolher outro banco de manutenção existente. Essas opções também podem ser definidas no `.env.anemico`.

## Compilar e iniciar

É necessário JDK 17 ou superior e Maven 3.6.3 ou superior. Configure JAVA_HOME para um JDK compatível; um Java 8 usado por outro projeto não executa esta aplicação.

Exemplo com o JDK 17 instalado nesta máquina:

~~~powershell
$env:JAVA_HOME = "C:\Java\jdk-17.0.12"
$env:DB_PASSWORD = "a-mesma-senha-do-banco"
mvn -s configuracao-maven.xml -pl precificacao-anemica package
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
~~~

configuracao-maven.xml usa somente o Maven Central para este comando. Isso permite compilar o projeto pessoal quando a configuração padrão do Maven aponta para o Nexus corporativo, sem alterar o arquivo de configuração global.

Também é possível iniciar durante o desenvolvimento:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-anemica spring-boot:run
~~~

Endereço padrão: http://localhost:8080. SERVER_PORT permite mudar a porta. O servidor escuta em 127.0.0.1 por padrão; SERVER_ADDRESS permite configurar outro endereço.

No Git Bash, já na raiz do projeto e com a senha configurada:

~~~bash
JAVA_HOME='C:/Java/jdk-17.0.12' mvn -s configuracao-maven.xml -pl precificacao-anemica spring-boot:run
~~~

## Endpoints para o Postman

Importe [precificacao-anemica.postman_collection.json](../documentacao/postman/precificacao-anemica.postman_collection.json). A coleção tem 59 requisições em cinco pastas. A variável `baseUrl` começa em `http://localhost:8080`. Confira se o ambiente selecionado no Postman está sobrescrevendo esse valor; selecione `No environment` para usar somente as variáveis da coleção. Execute as pastas e suas requisições na ordem pelo Collection Runner, começando pela primeira requisição de preparação.

A preparação gera nomes, e-mails e CPFs de exemplo para cada execução e limpa as variáveis preenchidas pelo fluxo anterior. Os cadastros guardam `codigoCidade`, `codigoOutraCidade`, `codigoCategoria`, `codigoOutraCategoria`, `codigoCliente`, `codigoOutroCliente`, `codigoProduto`, `codigoFatura` e `codigoFaturaSemOpcionais` nas variáveis da coleção. Os filtros e as alterações seguintes usam esses códigos, sem pressupor que os registros começam em `1`. A variável `totalSimulado` guarda o total usado na comparação entre simulação e cadastro.

As pastas cobrem preparação dos cadastros, produtos e histórico, faturas, atualização de clientes e renomeação de categorias. Incluem respostas esperadas de sucesso, entradas inválidas e conferências dos dados depois de operações rejeitadas. Para executar uma requisição isolada, execute antes as requisições das quais ela depende. O teste do cadastro de fatura com PIX compara seu total com o da simulação imediatamente anterior. A fatura sem campos opcionais usa outro código, permitindo consultá-la e identificá-la na listagem.

### Cadastrar produto

POST http://localhost:8080/api/produtos

Cabeçalho: Content-Type: application/json.

~~~json
{
  "nome": "Produto de exemplo",
  "custo": 100.00,
  "percentual": 20.0000,
  "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
}
~~~

Resposta: 201 Created, com cabeçalho Location e corpo:

~~~json
{
  "codigo": 1,
  "nome": "Produto de exemplo",
  "custo": 100.00,
  "precoVenda": 120.00,
  "percentual": 20.0000,
  "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
}
~~~

O código é gerado pelo banco e pode ser diferente de 1.

Informe a modalidade e exatamente dois valores entre custo, precoVenda e percentual. Omita o terceiro ou envie null. Zero é um valor informado, não um campo ausente.

| Informados | Calculado |
|---|---|
| custo e percentual | precoVenda |
| precoVenda e percentual | custo |
| custo e precoVenda | percentual |

As modalidades aceitas são ACRESCIMO_SOBRE_CUSTO e MARGEM_SOBRE_VENDA. Percentuais são enviados na forma 20 para 20%, não 0.20.

Exemplo de cadastro com cálculo inverso do custo:

~~~json
{
  "nome": "Produto com custo calculado",
  "precoVenda": 125.00,
  "percentual": 20.0000,
  "modalidadePrecificacao": "MARGEM_SOBRE_VENDA"
}
~~~

O custo calculado de 100.00 é gravado no produto cadastrado.

### Cadastrar e listar categorias

Envie `POST http://localhost:8080/api/categorias` com `Content-Type: application/json` e o corpo abaixo.

```json
{"nome": "Periféricos"}
```

A resposta `201 Created` contém `codigo` e `nome`. Nomes repetidos, como `Periféricos` e `periféricos`, retornam `400`. Consulte as categorias em `GET http://localhost:8080/api/categorias`.

Para cadastrar um produto com categoria, inclua `"codigoCategoria": 1` no JSON do cadastro, usando o código devolvido pela API. O campo é opcional e aparece na resposta dos produtos que têm categoria.

Para vincular ou trocar a categoria de um produto, envie `PATCH http://localhost:8080/api/produtos/1/categoria` com o código da categoria no corpo.

```json
{"codigoCategoria": 2}
```

A resposta `200 OK` contém o produto atualizado. Um código de categoria inválido retorna `400` e uma categoria inexistente retorna `404`.

### Renomear categoria

Envie `PATCH http://localhost:8080/api/categorias/1`, usando o código retornado no cadastro:

```json
{"nome": "Acessórios"}
```

A resposta `200 OK` contém o mesmo `codigo` e o novo `nome`. O nome é obrigatório, tem até 200 caracteres e segue a mesma validação do cadastro. Duplicidade, inclusive com capitalização diferente, retorna `400`; categoria inexistente retorna `404`. A alteração preserva os vínculos com produtos e os registros do histórico de precificação.

### Listar produtos

Envie `GET http://localhost:8080/api/produtos` para listar os produtos em ordem de código. Use os filtros opcionais `nome` e `modalidadePrecificacao` na aba Params do Postman. Por exemplo, `nome=teclado` busca esse trecho no nome, ignorando maiúsculas e minúsculas. Também é possível filtrar por modalidade e pelo código da categoria, como em `?nome=teclado&modalidadePrecificacao=ACRESCIMO_SOBRE_CUSTO&categoria=1`. O produto precisa atender a todos os filtros informados.

A resposta é `200 OK` com uma lista, que fica vazia quando nenhum produto corresponde. Uma modalidade inválida retorna `400`.

### Consultar produto

GET http://localhost:8080/api/produtos/1

Resposta: 200 OK com os mesmos campos do cadastro, ou 404 quando o código não existir.

### Alterar a precificação

Envie `PATCH http://localhost:8080/api/produtos/1/precificacao` com `Content-Type: application/json`. Substitua `1` pelo código do produto cadastrado.

~~~json
{
  "custo": 120.00,
  "percentual": 20.0000
}
~~~

Informe exatamente dois valores, usando as mesmas combinações do cadastro. O terceiro é calculado com a modalidade do produto. Código, nome e modalidade são preservados. No exemplo, um produto com acréscimo sobre custo passa a ter preço de venda de 144.00.

A resposta é `200 OK` com o produto atualizado. Um código válido sem registro retorna `404` e valores inválidos retornam `400`.

### Consultar histórico de precificação

Envie `GET http://localhost:8080/api/produtos/1/historico-precificacao`, substituindo `1` pelo código do produto.

A resposta `200 OK` traz as alterações da mais recente para a mais antiga. Cada registro inclui o código da alteração, o código do produto, a data e os valores anteriores e novos de custo, preço de venda e percentual. O histórico inclui alterações efetivas feitas após o cadastro. Reenviar os mesmos valores preserva a lista de alterações.

Um produto sem alterações retorna `[]`. Um código inválido retorna `400` e um produto inexistente retorna `404`.

### Cadastrar e listar cidades

Envie `POST http://localhost:8080/api/cidades` com `Content-Type: application/json` e o corpo abaixo.

```json
{"nome": "Curitiba", "uf": "PR"}
```

A resposta `201 Created` contém `codigo`, `nome` e `uf`. A API aceita a sigla em minúsculas e grava em maiúsculas. Uma UF inválida ou uma cidade com o mesmo nome na mesma UF retorna `400`. A comparação de nomes ignora maiúsculas e minúsculas.

Envie `GET http://localhost:8080/api/cidades` para listar as cidades em ordem de código.

Os filtros `nome` e `uf` são opcionais e podem ser combinados:

| Consulta | Resultado |
|---|---|
| `GET /api/cidades?nome=curi` | Cidades cujo nome contém o trecho, ignorando maiúsculas e minúsculas. |
| `GET /api/cidades?uf=pr` | Cidades do Paraná; a UF é normalizada para `PR`. |
| `GET /api/cidades?nome=curitiba&uf=PR` | Cidades que atendem aos dois filtros. |

Espaços nas pontas são removidos e filtros em branco são tratados como ausentes. Uma UF brasileira inválida retorna `400`. Sem correspondências, a resposta é `200` com `[]`. A ordem continua sendo pelo código. Os caracteres `%` e `_` no nome são buscados como texto literal; configure o parâmetro na aba Params do Postman para que a URL seja codificada corretamente.

### Cadastrar e listar clientes

Envie `POST http://localhost:8080/api/clientes` com `Content-Type: application/json` e o corpo abaixo. Use o código de uma cidade já cadastrada em `codigoCidade`.

```json
{
  "nome": "Cliente de exemplo",
  "cpf": "123.456.789-09",
  "email": "cliente@example.com",
  "telefone": "(41) 99999-0000",
  "endereco": {
    "cep": "80000-000",
    "logradouro": "Rua de exemplo",
    "numero": "12A",
    "bairro": "Centro",
    "codigoCidade": 1
  }
}
```

Nome, CPF e e-mail são obrigatórios. Telefone e endereço são opcionais. O telefone aceita número fixo ou celular brasileiro com DDD, com prefixo `+55` opcional. Quando informado, o endereço exige CEP, logradouro, número, bairro e o código da cidade. O complemento é opcional.

A resposta `201 Created` contém o cliente cadastrado e o código gerado pelo banco. CPF e CEP podem ser enviados com ou sem pontuação e são retornados somente com os dígitos. O CPF é validado pelos dígitos verificadores. CPF duplicado, e-mail duplicado ou dados inválidos retornam `400`. A comparação de e-mails ignora maiúsculas e minúsculas. Cidade inexistente retorna `404`.

Envie `GET http://localhost:8080/api/clientes` para listar os clientes em ordem de código.

### Consultar cliente pelo código

Envie `GET http://localhost:8080/api/clientes/1`. A resposta `200 OK` tem os mesmos campos do cadastro, com `endereco: null` quando ausente. Código inválido, como `0` ou `abc`, retorna `400`; um código positivo sem cliente retorna `404`.

### Atualizar os contatos e o endereço do cliente

Envie `PATCH http://localhost:8080/api/clientes/1` com um ou mais dos campos `email`, `telefone` e `endereco`:

```json
{
  "email": "novo@example.com",
  "telefone": "(41) 99999-0000",
  "endereco": {
    "cep": "80000-000",
    "logradouro": "Rua de exemplo",
    "numero": "12A",
    "bairro": "Centro",
    "codigoCidade": 2
  }
}
```

Use uma cidade cadastrada em `codigoCidade`. O objeto de endereço substitui o endereço inteiro; `complemento` é opcional. Os campos seguem as mesmas validações do cadastro.

| Campo | Omitido | `null` | Valor informado |
|---|---|---|---|
| `email` | Mantém o atual. | Retorna `400`. | Atualiza se válido e único, ignorando capitalização para a unicidade. |
| `telefone` | Mantém o atual. | Remove o telefone. | Valida e atualiza; texto em branco é tratado como ausência de telefone, como no cadastro. |
| `endereco` | Mantém o atual. | Remove todos os dados do endereço, inclusive a cidade. | Exige um endereço completo e uma cidade existente. |

Exemplos de remoção:

```json
{"telefone": null}
```

```json
{"endereco": null}
```

A resposta `200 OK` traz o cliente atualizado. Corpo vazio, campos desconhecidos, tentativa de alterar nome ou CPF e valores inválidos retornam `400`. Cliente ou cidade inexistente retorna `404`. Todos os campos são validados antes da gravação: uma falha em qualquer um deles preserva o cadastro inteiro. O comando de atualização inclui somente as colunas correspondentes aos campos enviados; quando o endereço é enviado, todas as suas colunas são tratadas em conjunto. A leitura para atualização usa bloqueio de linha na mesma transação da gravação, preservando também os campos omitidos em alterações simultâneas. O código do cliente e seus vínculos com faturas são mantidos.

### Criar fatura

Envie `POST http://localhost:8080/api/faturas` com `Content-Type: application/json`. Use códigos de produtos cadastrados e, se quiser vincular a fatura a um cliente, informe o código dele.

~~~json
{
  "itens": [
    { "codigoProduto": 1, "quantidade": 2.5 }
  ],
  "desconto": { "tipo": "PERCENTUAL", "valor": 10 },
  "codigoCliente": 1,
  "formaPagamento": "PIX"
}
~~~

A fatura aceita ao menos um item, com códigos distintos e quantidades positivas de até quatro casas decimais e 15 dígitos inteiros. O desconto, `codigoCliente` e `formaPagamento` são opcionais. `PERCENTUAL` aceita de 0 a 100 com até quatro casas decimais. `VALOR_FIXO` aceita um valor não negativo com até duas casas decimais, limitado ao subtotal na criação.

O campo `formaPagamento` aceita `PIX`, `DINHEIRO`, `CARTAO_DEBITO`, `CARTAO_CREDITO` ou `BOLETO`.

O `201 Created` retorna código, data de criação, itens na ordem enviada, subtotal, desconto aplicado e total. Os campos `codigoCliente` e `formaPagamento` aparecem quando informados. Os itens incluem o preço atual e o subtotal calculado. Os cálculos usam a precisão disponível até o total final, arredondado para duas casas com `HALF_EVEN`. Dados inválidos retornam `400`. Produtos ou clientes inexistentes retornam `404`.

### Simular fatura

Envie `POST http://localhost:8080/api/faturas/simulacao` com o mesmo JSON do cadastro. A resposta `200 OK` traz os itens com os preços atuais, o subtotal, o desconto aplicado, a taxa de pagamento e o total. A simulação é uma prévia e não grava uma fatura.

O cálculo e as validações são os mesmos do cadastro. Dados inválidos retornam `400`. Produtos ou clientes inexistentes retornam `404`.

A taxa depende da forma de pagamento: Pix, dinheiro e cartão de débito não cobram taxa; boleto cobra R$ 5,00; cartão de crédito cobra 2% do saldo após o desconto. A taxa é calculada apenas na simulação e o campo `taxaPagamento` sempre aparece na resposta, inclusive quando for zero.

### Listar faturas

Envie `GET http://localhost:8080/api/faturas?inicio=2026-11-01&fim=2026-11-08&codigoCliente=1`. O filtro `codigoCliente` é opcional e aceita um código inteiro positivo. Os parâmetros `inicio` e `fim` são opcionais, usam o formato `AAAA-MM-DD` e incluem o dia informado por inteiro.

A resposta `200 OK` traz uma lista com `codigo`, `dataCriacao` e `quantidadeItens` de cada fatura, das mais recentes para as mais antigas. O resumo inclui `codigoCliente` e `formaPagamento` quando informados. Uma busca sem resultados devolve `[]`. Datas inválidas ou um início posterior ao fim retornam `400`.

### Consultar fatura

Envie `GET http://localhost:8080/api/faturas/1`, substituindo `1` pelo código retornado no cadastro. A resposta `200 OK` usa o mesmo formato do cadastro, com os preços de venda atuais. Um código inválido retorna `400` e uma fatura ausente retorna `404`.

O desconto fixo aplicado na consulta fica limitado ao subtotal atual. O valor original permanece gravado e volta a ser aplicado integralmente se o subtotal voltar a comportá-lo.

## Validações implementadas

- Nome obrigatório, com até 200 caracteres.
- Modalidade obrigatória e exatamente dois valores de precificação.
- Custo maior que zero e venda maior ou igual ao custo.
- Percentual não negativo; margem sobre venda inferior a 100%.
- Entradas monetárias com até duas casas decimais e percentual com até quatro. Zeros adicionais à direita são aceitos.
- Resultados arredondados com HALF_EVEN, sem arredondar etapas intermediárias.
- Percentual positivo que resulte em venda igual ao custo após arredondar invalida o cálculo.
- Limites de armazenamento: 17 dígitos inteiros nos valores monetários e 15 no percentual.
- Código da consulta maior que zero.

Requisições inválidas recebem 400 e não gravam o produto. Produto ausente recebe 404. Falhas de persistência recebem 503 com mensagem genérica. JSON malformado, campos desconhecidos, números no lugar da modalidade e modalidades inexistentes são recusados.

## Testes

O comando padrão executa somente os testes unitários, sem iniciar ou acessar PostgreSQL:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-anemica test
~~~

Os testes verificam as duas modalidades, os três cálculos, arredondamento, limites, rejeição de entradas e respostas HTTP. `FaturaServiceTest` confere precisão, quantidades e descontos válidos e inválidos. `ProdutoControllerTest` usa um repositório de teste em memória e `ClienteControllerTest` usa mocks para simular falhas de persistência. Essas implementações ficam somente no código de testes.

Para validar a suíte completa, incluindo as quatro classes de integração com PostgreSQL, use o perfil `integracao`. Na raiz do repositório, em Bash:

~~~bash
export BANCO_TESTE_URL='jdbc:postgresql://localhost:5432/precificacao_teste'
export BANCO_CRIACAO_TESTE_URL='jdbc:postgresql://localhost:5432/postgres'
export BANCO_TESTE_USUARIO='postgres'
export BANCO_TESTE_SENHA='senha-do-banco-de-teste'
mvn -s configuracao-maven.xml -pl precificacao-anemica -Pintegracao test
~~~

Em PowerShell:

~~~powershell
$env:BANCO_TESTE_URL = "jdbc:postgresql://localhost:5432/precificacao_teste"
$env:BANCO_CRIACAO_TESTE_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:BANCO_TESTE_USUARIO = "postgres"
$env:BANCO_TESTE_SENHA = "senha-do-banco-de-teste"
mvn -s configuracao-maven.xml -pl precificacao-anemica -Pintegracao test
~~~

Crie antes o banco separado `precificacao_teste`: os testes aplicam as migrações V1 a V8 e inserem dados. `PostgresProdutoRepositoryTest`, `AplicacaoPostgresTest`, `CadastrosPostgresTest` e `CriacaoBancoPostgresTest` têm a tag `integracao`. O comando sem perfil exclui essas classes, mesmo com as variáveis definidas. Os testes unitários ficam em `target/surefire-reports`.

O perfil exige as quatro variáveis antes de iniciar os testes. Senha vazia pode ser definida em ambientes que usem outro método de autenticação. Os relatórios da suíte completa ficam em uma pasta própria por execução, em `target/surefire-integracao/<data-hora>`, sem reutilizar resultados antigos. Ao terminar, a validação exige os relatórios das quatro classes de PostgreSQL e reprova suítes vazias, falhas, erros ou testes ignorados. As opções para pular testes, ignorar falhas ou selecionar somente parte da suíte são recusadas nesse perfil. Para executar um cenário individual, use a IDE com as variáveis do banco configuradas.

O perfil vale para `precificacao-anemica`; a configuração do módulo rico está no README correspondente. A seleção por tags usa o [Surefire](https://maven.apache.org/surefire-archives/surefire-3.5.4/maven-surefire-plugin/test-mojo.html), e a conferência dos relatórios é chamada pelo [AntRun](https://maven.apache.org/plugins/maven-antrun-plugin/usage.html). O verificador fica somente em `src/test`, sem acrescentar casos à contagem de testes. A aplicação sempre utiliza PostgreSQL; não existe persistência em memória no código de produção.

`CadastrosPostgresTest` verifica consulta de clientes com e sem endereço; atualização parcial e remoção de campos opcionais; rejeição de entradas inválidas sem gravações parciais; conflitos de unicidade; filtros de cidades, incluindo `%` e `_` literais; renomeação de categorias com preservação dos vínculos e do histórico; alterações concorrentes de contatos; faturas com as cinco formas de pagamento e comportamento dos descontos com preços atuais. Também confere a precisão entre simulação, cadastro e consulta, a rejeição de faturas inválidas e o desfazimento da alteração de preço quando a gravação do histórico falha.

O teste de atualização exclusiva de e-mail instala um gatilho apenas no banco de teste para recusar comandos que mencionem colunas omitidas, mesmo que tentem regravar os mesmos valores. O gatilho e sua função são removidos ao terminar. `ClienteControllerTest` verifica que uma falha de acesso ao banco durante a atualização continua retornando `503`, com mensagem genérica.

Para comparar as respostas dos dois modelos por HTTP, siga as [instruções do executor local](../verificacao/README.md).

O teste `CriacaoBancoPostgresTest` usa `BANCO_CRIACAO_TESTE_URL`, que deve apontar para o banco de manutenção `postgres` de um servidor exclusivo de testes. Ele reutiliza `BANCO_TESTE_USUARIO` e `BANCO_TESTE_SENHA`. O usuário deve conseguir criar bancos e papéis. O cenário de concorrência também exige um administrador autorizado a bloquear `pg_catalog.pg_database`; esse bloqueio temporário deve ser usado apenas no servidor exclusivo de testes. O teste cria bancos temporários com nomes aleatórios e os remove ao terminar; verifica criação antes do Flyway, preservação dos dados ao reiniciar, desativação da criação, identificadores com aspas, concorrência e falhas de permissão ou autenticação. A criação desativada exige especificamente o erro PostgreSQL de banco inexistente.

Os cenários de concorrência confirmam no PostgreSQL que as duas operações estão aguardando o mesmo bloqueio antes de liberá-las. O teste de contatos mantém bloqueado somente o cliente criado pelo próprio cenário. O teste de criação segura as duas instruções `CREATE DATABASE` para o mesmo nome. A observação tem limite de cinco segundos; os bloqueios são liberados e as tarefas são aguardadas durante a limpeza, inclusive quando alguma verificação falha.

Na validação pelo ambiente isolado do Codex no Windows, foi necessário usar um diretório temporário curto para os sockets internos do Java. Para reproduzir nesse ambiente, acrescente ao comando Maven:

~~~powershell
"-DargLine=-Djdk.net.unixdomain.tmpdir=C:\Java\Workspace\tmp"
~~~
