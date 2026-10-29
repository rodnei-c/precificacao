# Precificação anêmica

API HTTP de cadastro e consulta de produtos, com Java 17, Spring Boot 4.1.2, JDBC, PostgreSQL e Flyway.

## Responsabilidades

- Controller: recebe JSON, lê o código da URL e define a resposta HTTP.
- Service: valida o cadastro, executa as fórmulas e decide se o produto pode ser gravado.
- Repository: executa SQL parametrizado e converte os registros do banco em dados de produto.
- Model: mantém dados, construtores e getters/setters, sem regras de negócio.

Os contratos dos serviços ficam em `services`, e suas implementações em `services/impl`: `ProdutoServiceImpl`, `PrecificacaoServiceImpl` e `FaturaServiceImpl`. Os controllers recebem as interfaces dos serviços.

As interfaces de repositório ficam em `repositories`. A implementação `JdbcProdutoRepository`, em `repositories/impl`, executa o acesso ao PostgreSQL. As faturas continuam na estrutura inicial; seus endpoints e regras ainda não foram implementados.

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

A aplicação lê essas variáveis do processo que a inicia. Não é necessário colocar credenciais no código. Arquivos .env não são carregados automaticamente pela aplicação.

Ao iniciar, o Flyway executa src/main/resources/db/migration/V1__criar_tabela_produto.sql e cria a tabela produto e o histórico de migrações. O servidor e o banco PostgreSQL precisam existir antes; a migração cria as tabelas, não o banco.

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

O custo calculado de 100.00 é gravado no produto cadastrado. Este endpoint cria um produto; a alteração de produtos existentes fica para uma etapa posterior.

### Consultar produto

GET http://localhost:8080/api/produtos/1

Resposta: 200 OK com os mesmos campos do cadastro, ou 404 quando o código não existir.

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
