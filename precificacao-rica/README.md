# Precificação com Domínio Rico

A API usa Takes e o banco `precificacao_rica`. `TkCadastroProduto` atende `POST /api/produtos`, `TkConsultaProduto` atende `GET /api/produtos/{codigo}`, `TkCadastroCategoria` atende `POST /api/categorias` e `TkListaCategorias` atende `GET /api/categorias`.

## Estrutura inicial de faturas

O pacote `br.com.modelagemdominio.rico.faturas` reúne o início do domínio de faturamento, no mesmo nível de `produtos`:

~~~text
faturas/
├── Fatura.java
├── Faturas.java
├── ItemFatura.java
├── Quantidade.java
├── FalhaFatura.java
├── FaturaInvalida.java
└── descontos/
    ├── Desconto.java
    ├── DescontoFixo.java
    ├── DescontoPercentual.java
    └── SemDesconto.java
~~~

`Fatura` define o contrato da entidade e `Faturas` define cadastro, consulta e listagem da coleção. Seus contratos registram a necessidade de pelo menos um item, a rejeição de desconto fixo superior ao subtotal no cadastro e o arredondamento somente do total final. As tabelas de faturamento já fazem parte das migrações. A implementação concreta da fatura, os cálculos do subtotal geral e do total, a persistência em Java e as rotas HTTP ainda estão pendentes.

`ItemFatura` já calcula seu subtotal. Ele recebe a coleção `Produtos`, o código do produto e uma `Quantidade` positiva com até quatro casas decimais. Cada cálculo consulta novamente a coleção e multiplica o preço atual pela quantidade, mantendo toda a precisão. Isso também permite usar `PgProdutosConstantes`: cada consulta à coleção faz uma nova leitura, mesmo que o produto devolvido por ela preserve os valores daquela leitura. A construção do item não acessa o banco.

Os descontos usam composição pelo contrato `Desconto`. `DescontoPercentual` calcula o abatimento sem arredondamento intermediário. `DescontoFixo` mantém seu valor original e limita o abatimento ao subtotal atual; quando os preços sobem, o valor original volta a ser aplicado até esse limite. `SemDesconto` representa ausência de abatimento. O desconto percentual segue a regra inicial, sem o mínimo de R$ 200,00 previsto para o cenário posterior de manutenção.

Os testes de quantidade, item e descontos podem ser executados sem PostgreSQL pelo comando de testes do módulo.

## Objetos de consulta

`PgProduto` representa o registro no PostgreSQL e pode consultar nome e precificação sob demanda. A coleção `PgProdutosConstantes`, usada pela API, carrega os dados do produto em um único `SELECT` e passa o mesmo `DataSource` ao `PgProduto`.

O decorador `ProdutoConstante` envolve esse `PgProduto`, delega `codigo()` e devolve o nome e a `Precificacao` já carregados. Uma nova requisição faz uma nova leitura. O desenho segue o exemplo de [SQL-speaking objects de Bugayenko](https://www.yegor256.com/2014/12/01/orm-offensive-anti-pattern.html#what-about-performance).

## Rodar a API

Com o banco preparado e a migração aplicada, execute na raiz do repositório:

~~~powershell
$env:DB_PASSWORD = "senha-do-banco"
mvn -s configuracao-maven.xml -pl precificacao-rica package
java -jar precificacao-rica/target/precificacao-rica-1.0.0-SNAPSHOT.jar
~~~

No Postman, faça um GET em `http://127.0.0.1:8081/api/produtos/1`, substituindo `1` pelo código de um produto salvo em `precificacao_rica`. Um código válido sem registro retorna `404`. A porta pode ser alterada com `RICO_SERVER_PORT`.

## Cadastrar um produto

No Postman, envie um POST para `http://127.0.0.1:8081/api/produtos` com `Content-Type: application/json`. Informe nome, modalidade e dois dos três valores de precificação. Para vincular uma categoria, inclua `codigoCategoria` com o código inteiro positivo de uma categoria cadastrada.

~~~json
{
  "nome": "Produto de exemplo",
  "custo": 100,
  "percentual": 20,
  "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
}
~~~

O cadastro calcula o preço de venda de 120,00 e responde com `201 Created`, o produto salvo e o cabeçalho `Location` com o caminho do GET.

O cadastro e a consulta devolvem `codigoCategoria` quando o produto tem vínculo. Omitir o campo ou enviá-lo como `null` cadastra sem categoria. Código inválido retorna `400` e categoria inexistente retorna `404`.

## Listar produtos

Envie um GET para `http://127.0.0.1:8081/api/produtos`. A resposta `200 OK` contém os produtos em ordem crescente de código, com `codigoCategoria` quando houver vínculo. Quando não houver produtos, a API devolve `[]`.

Os filtros `nome`, `modalidadePrecificacao` e `categoria` são opcionais e podem ser combinados. Por exemplo, `GET /api/produtos?nome=cafe&modalidadePrecificacao=ACRESCIMO_SOBRE_CUSTO&categoria=2` devolve apenas os produtos que atendem aos três critérios. O filtro de nome busca um trecho sem distinguir maiúsculas de minúsculas e trata `%` e `_` como caracteres literais.

## Alterar a precificação de um produto

Envie um PATCH para `http://127.0.0.1:8081/api/produtos/1/precificacao` com `Content-Type: application/json`. Substitua `1` pelo código do produto e informe dois dos três valores de precificação.

~~~json
{
  "custo": 100.00,
  "percentual": 25.00
}
~~~

A API calcula o terceiro valor pela modalidade atual do produto e devolve `200 OK` com o resultado. Quando há mudança, o produto e o histórico são gravados na mesma transação. Repetir valores numericamente equivalentes devolve o produto sem gerar outro registro no histórico.

## Consultar o histórico de precificação

Envie um GET para `http://127.0.0.1:8081/api/produtos/1/historico-precificacao`, substituindo `1` pelo código do produto.

A resposta `200 OK` traz os registros em ordem decrescente de código. Cada registro contém `codigo`, `codigoProduto`, `dataAlteracao` em formato ISO e os grupos `anteriores` e `novos`, ambos com `custo`, `precoVenda` e `percentual`.

Um produto sem alterações retorna `[]`. Código inválido retorna `400`, produto inexistente retorna `404` e banco indisponível retorna `503`.

## Trocar a categoria de um produto

Envie um PATCH para `http://127.0.0.1:8081/api/produtos/1/categoria` com `Content-Type: application/json`. Substitua `1` na URL pelo código do produto e informe uma categoria existente no corpo.

~~~json
{
  "codigoCategoria": 2
}
~~~

A resposta `200 OK` traz o produto com a nova categoria e preserva o nome e a precificação. Código de categoria inválido, omitido ou `null` retorna `400`. Produto ou categoria inexistente retorna `404`.

## Cadastrar uma categoria

Depois de aplicar a migração V3, envie um POST no Postman para `http://127.0.0.1:8081/api/categorias` com `Content-Type: application/json`.

~~~json
{
  "nome": "Alimentos"
}
~~~

A API responde com `201 Created` e devolve `codigo` e `nome`. Um nome já cadastrado retorna `400`, mesmo que a capitalização seja diferente.

## Listar categorias

Envie um GET para `http://127.0.0.1:8081/api/categorias`. A resposta `200 OK` traz os campos `codigo` e `nome` em ordem crescente de código. Se ainda não houver categorias, a resposta é `[]`.

## Renomear uma categoria

Envie um PATCH para `http://127.0.0.1:8081/api/categorias/1` com `Content-Type: application/json`. Substitua `1` pelo código da categoria e informe o novo nome.

~~~json
{"nome": "Alimentos e bebidas"}
~~~

A resposta `200 OK` devolve `codigo` e `nome`. É possível reenviar o próprio nome ou mudar sua capitalização. Um nome já usado por outra categoria retorna `400`, mesmo que a capitalização seja diferente. Categoria inexistente retorna `404`.

## Cadastrar uma cidade

Envie um POST para `http://127.0.0.1:8081/api/cidades` com `Content-Type: application/json`.

~~~json
{"nome": "Curitiba", "uf": "pr"}
~~~

A resposta `201 Created` devolve `codigo`, `nome` e `uf`. A UF é normalizada para maiúsculas e o nome aceita até 200 caracteres. A API retorna `400` para nome ou UF inválidos e para nomes repetidos na mesma UF, mesmo com capitalização diferente. O mesmo nome pode ser usado em UFs distintas.

## Listar cidades

Envie um GET para `http://127.0.0.1:8081/api/cidades`. A resposta `200 OK` contém `codigo`, `nome` e `uf` de cada cidade em ordem crescente de código. Quando não há cidades cadastradas, a resposta é `[]`. Se o banco estiver indisponível, a API retorna `503`.

Use `GET /api/cidades?nome=curitiba` para buscar por trecho do nome, sem distinguir maiúsculas de minúsculas. Espaços nas pontas são removidos. Um filtro vazio ou em branco equivale a listar todas as cidades. O texto pode ter até 200 caracteres e `%` e `_` são tratados como caracteres literais. Se nenhuma cidade corresponder ao filtro, a resposta é `200 OK` com `[]`.

Use `GET /api/cidades?uf=PR` para filtrar pela UF ou `GET /api/cidades?nome=curitiba&uf=PR` para exigir os dois critérios. A UF aceita minúsculas e espaços nas pontas. Uma UF em branco equivale a omitir o parâmetro e uma sigla inválida retorna `400`.

## Cadastrar um cliente

Envie um POST para `http://127.0.0.1:8081/api/clientes` com `Content-Type: application/json`. Em `codigoCidade`, informe o código de uma cidade já cadastrada.

~~~json
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
~~~

A resposta `201 Created` devolve o código gerado e os dados do cliente. CPF e CEP são devolvidos apenas com dígitos. A capitalização do e-mail é preservada.

Nome, CPF e e-mail são obrigatórios. Telefone e endereço podem ser omitidos e são devolvidos como `null` quando ausentes. Ao informar um endereço, apenas o complemento é opcional. Dados inválidos, CPF repetido ou e-mail já cadastrado com qualquer capitalização retornam `400`. Cidade inexistente retorna `404`.

## Listar clientes

Envie um GET para `http://127.0.0.1:8081/api/clientes`. A resposta `200 OK` traz os clientes em ordem crescente de código, com os mesmos campos do cadastro. Quando não houver clientes, a resposta é `[]`. Se o banco estiver indisponível, a API retorna `503`.

## Consultar um cliente pelo código

Envie um GET para `http://127.0.0.1:8081/api/clientes/1`, substituindo `1` pelo código do cliente. A resposta `200 OK` contém os mesmos campos do cadastro. Código inválido retorna `400`, cliente inexistente retorna `404` e banco indisponível retorna `503`.

## Atualizar os contatos de um cliente

Envie um PATCH para `http://127.0.0.1:8081/api/clientes/1` com `Content-Type: application/json`. Substitua `1` pelo código do cliente.

~~~json
{"email": "Novo.Email@example.com"}
~~~

Envie `email`, `telefone` ou os dois. Campos omitidos conservam o valor atual. Para remover o telefone, envie `{"telefone":null}`. Um telefone informado como texto deve ser um número brasileiro com DDD. O e-mail, quando enviado, precisa ser válido e não pode ser nulo.

A resposta `200 OK` devolve o cliente atualizado. Dados inválidos ou e-mail já usado por outro cliente retornam `400` e preservam os contatos anteriores. A comparação de e-mails ignora maiúsculas e minúsculas. Cliente inexistente retorna `404`.

## Banco de dados

O banco desta implementação é `precificacao_rica`, separado de `precificacao_anemica`. Os dados e o histórico de migrações são independentes.

Para preparar uma instalação nova, conecte-se ao banco `postgres` pelo DBeaver ou por outro cliente PostgreSQL e execute uma vez:

~~~sql
CREATE DATABASE precificacao_rica;
~~~

O Flyway cria as tabelas dentro desse banco. Ele precisa que o servidor PostgreSQL e o banco já existam.

## Aplicar as migrações

É necessário JDK 17 ou superior e Maven 3.6.3 ou superior. Execute os comandos em PowerShell, na raiz do repositório:

~~~powershell
cd C:\Java\workspace\modelagem-dominio
$env:FLYWAY_PASSWORD = "senha-do-banco"
mvn -s configuracao-maven.xml -pl precificacao-rica flyway:migrate
~~~

Substitua `senha-do-banco` pela senha do usuário `postgres`. A senha é lida da variável de ambiente do terminal. Nenhuma senha fica no POM ou na migração.

A conexão padrão é `jdbc:postgresql://localhost:5432/precificacao_rica`, com o usuário `postgres`. Se precisar de outra conexão, informe `-Dflyway.url` e `-Dflyway.user` no comando Maven. Variáveis `FLYWAY_URL` e `FLYWAY_USER` também podem sobrescrever a configuração padrão; confira se elas apontam para o banco desta implementação.

Para consultar o estado e validar o histórico:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-rica flyway:info flyway:validate
~~~

O Flyway registra as migrações aplicadas em `public.flyway_schema_history`. Executar `flyway:migrate` novamente aplica somente migrações pendentes. A operação `flyway:clean` está desativada na configuração do módulo.

O plugin é executado pelos comandos acima; compilar o projeto não aplica migrações no banco.

## Estrutura inicial

A migração `src/main/resources/db/migration/V1__criar_tabela_produto.sql` cria `public.produto`:

| Coluna | Tipo | Regra |
|---|---|---|
| `codigo` | `BIGINT` | Chave primária gerada pelo banco |
| `nome` | `VARCHAR(200)` | Obrigatório e não vazio |
| `custo` | `NUMERIC(19,2)` | Maior que zero |
| `preco_venda` | `NUMERIC(19,2)` | Maior ou igual ao custo |
| `percentual` | `NUMERIC(19,4)` | Não negativo |
| `modalidade_precificacao` | `VARCHAR(30)` | `ACRESCIMO_SOBRE_CUSTO` ou `MARGEM_SOBRE_VENDA` |

Todas as colunas são obrigatórias. A migração V2 rejeita NaN no custo, no preço de venda e no percentual. Na margem sobre venda, o percentual deve ser inferior a 100. Um percentual positivo exige venda superior ao custo.

As migrações V3 a V10 completam o esquema de negócio com categoria, histórico de precificação, cidade, cliente, fatura e item de fatura. Elas também adicionam os vínculos de produto com categoria e de fatura com cliente, além da forma de pagamento da fatura. O SQL dessas tabelas e vínculos segue o módulo anêmico.

O banco mantém os valores calculados e suas restrições de integridade. As fórmulas e as validações das entradas pertencem aos objetos Java. A divisão dos objetos em classes não exige uma tabela para cada classe.

## Compilar

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-rica package
~~~

## Testes

Os testes unitários usam o fake reutilizável `DataSourceSemConexao`, que falha se algum objeto tentar abrir uma conexão. Os casos cobrem os construtores e a rejeição de códigos não positivos:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-rica test
~~~

Para a integração, crie um banco separado chamado `precificacao_rica_teste`, aplique a migração nele e configure as variáveis no mesmo terminal:

~~~powershell
$env:FLYWAY_PASSWORD = "senha-do-banco-de-teste"
mvn -s configuracao-maven.xml -pl precificacao-rica "-Dflyway.url=jdbc:postgresql://localhost:5432/precificacao_rica_teste" flyway:migrate
$env:RICO_BANCO_TESTE_URL = "jdbc:postgresql://localhost:5432/precificacao_rica_teste"
$env:RICO_BANCO_TESTE_USUARIO = "postgres"
$env:RICO_BANCO_TESTE_SENHA = "senha-do-banco-de-teste"
mvn -s configuracao-maven.xml -pl precificacao-rica test
~~~

`PgProdutosTest` cadastra um produto de apoio para cada teste e remove esse registro ao terminar. Ele verifica leitura, texto com acentos e apóstrofo, atualização observada por PgProduto, valores preservados pelo decorador, códigos ausentes e substituição da implementação de produto. Sem `RICO_BANCO_TESTE_URL`, esses testes de integração são ignorados.
