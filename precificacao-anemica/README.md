# Precificação anêmica

API HTTP de cadastro, consulta e alteração da precificação de produtos e criação e consulta de faturas, com Java 17, Spring Boot 4.1.2, JDBC, PostgreSQL e Flyway.

## Responsabilidades

- Controller: recebe JSON, lê o código da URL e define a resposta HTTP.
- Service: valida o cadastro, executa as fórmulas e decide se o produto pode ser gravado.
- Repository: executa SQL parametrizado e converte os registros do banco em dados de produto.
- Model: mantém dados, construtores e getters/setters, sem regras de negócio.

Os contratos dos serviços ficam em `services`, e suas implementações em `services/impl`: `ProdutoServiceImpl`, `PrecificacaoServiceImpl` e `FaturaServiceImpl`. Os controllers recebem as interfaces dos serviços.

As interfaces de repositório ficam em `repositories`. As implementações JDBC em `repositories/impl` executam o acesso ao PostgreSQL.

## Banco de dados

As instruções abaixo são executadas na raiz do repositório, em PowerShell.

Se precisar de um PostgreSQL local e tiver Docker:

~~~powershell
$env:DB_PASSWORD = "defina-uma-senha-local"
docker compose up -d postgres
~~~

O Compose cria o banco precificacao_anemica e mantém seus dados em um volume. Usa a porta local 5432; se ela já estiver ocupada, ajuste a publicação em compose.yml e a variável DB_URL da aplicação.

Se utilizar um PostgreSQL existente, crie um banco separado para o projeto e configure:

~~~powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/precificacao_anemica"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "senha-do-banco"
~~~

A aplicação lê essas variáveis do processo que a inicia. Para manter a senha entre execuções locais, crie `precificacao-anemica/.env.anemico` com uma linha `DB_PASSWORD=sua-senha-local`. O módulo carrega esse arquivo opcional, que o Git ignora. Uma variável `DB_PASSWORD` definida no terminal tem prioridade sobre o arquivo.

Ao iniciar, o Flyway aplica as migrações em `src/main/resources/db/migration`, que criam as tabelas `produto`, `fatura`, `item_fatura`, `historico_precificacao`, `categoria`, `cidade` e `cliente`. Prepare o servidor e o banco PostgreSQL antes de iniciar a aplicação para que o Flyway possa criar as tabelas.

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

## Endpoints para o Postman

Importe documentacao/postman/precificacao-anemica.postman_collection.json. A coleção usa a variável baseUrl e guarda o código retornado no cadastro para a consulta seguinte.

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

Envie `POST http://localhost:8080/api/faturas/simulacao` com o mesmo JSON do cadastro. A resposta `200 OK` traz os itens com os preços atuais, o subtotal, o desconto aplicado e o total. A simulação é uma prévia e não grava uma fatura.

O cálculo e as validações são os mesmos do cadastro. Dados inválidos retornam `400`. Produtos ou clientes inexistentes retornam `404`.

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

Sem precisar iniciar um banco:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-anemica test
~~~

Os testes verificam as duas modalidades, os três cálculos, arredondamento, limites, rejeição de entradas e respostas HTTP. A camada HTTP usa um repositório de teste em memória; essa implementação fica somente no código de testes.

Os testes PostgresProdutoRepositoryTest e AplicacaoPostgresTest usam PostgreSQL real quando estas variáveis estão presentes. O segundo inicia o servidor em uma porta aleatória e envia requisições HTTP:

~~~powershell
$env:BANCO_TESTE_URL = "jdbc:postgresql://localhost:5432/precificacao_teste"
$env:BANCO_TESTE_USUARIO = "postgres"
$env:BANCO_TESTE_SENHA = "senha-do-banco-de-teste"
mvn -s configuracao-maven.xml -pl precificacao-anemica test
~~~

Crie um banco separado para essa verificação: os testes executam a migração e inserem produtos. Sem BANCO_TESTE_URL, esses dois testes de integração são ignorados. A aplicação sempre utiliza PostgreSQL; não existe persistência em memória no código de produção.

Na validação pelo ambiente isolado do Codex no Windows, foi necessário usar um diretório temporário curto para os sockets internos do Java. Para reproduzir nesse ambiente, acrescente ao comando Maven:

~~~powershell
"-DargLine=-Djdk.net.unixdomain.tmpdir=C:\Java\Workspace\tmp"
~~~
