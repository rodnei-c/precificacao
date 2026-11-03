# Precificação com Domínio Rico

A API usa Takes. `TkCadastroProduto` atende `POST /api/produtos`, e `TkConsultaProduto` atende `GET /api/produtos/{codigo}`. Os dois usam o banco `precificacao_rica`.

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

`Fatura` define o contrato da entidade e `Faturas` define cadastro, consulta e listagem da coleção. Seus contratos registram a necessidade de pelo menos um item, a rejeição de desconto fixo superior ao subtotal no cadastro e o arredondamento somente do total final. A implementação concreta da fatura, os cálculos do subtotal geral e do total, a persistência e as rotas HTTP ainda estão pendentes. Por isso, nenhuma migração de faturamento foi adicionada nesta etapa.

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

No Postman, envie um POST para `http://127.0.0.1:8081/api/produtos` com `Content-Type: application/json`. Informe nome, modalidade e dois dos três valores de precificação.

~~~json
{
  "nome": "Produto de exemplo",
  "custo": 100,
  "percentual": 20,
  "modalidadePrecificacao": "ACRESCIMO_SOBRE_CUSTO"
}
~~~

O cadastro calcula o preço de venda de 120,00 e responde com `201 Created`, o produto salvo e o cabeçalho `Location` com o caminho do GET.

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
