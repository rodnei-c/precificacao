# Precificação com Domínio Rico

A API usa Takes e o banco `precificacao_rica`. `TkCadastroProduto` atende `POST /api/produtos`, `TkConsultaProduto` atende `GET /api/produtos/{codigo}`, `TkCadastroCategoria` atende `POST /api/categorias` e `TkListaCategorias` atende `GET /api/categorias`.

## Cálculos de faturas

O pacote `br.com.modelagemdominio.rico.faturas` reúne o domínio de faturamento, no mesmo nível de `produtos`:

~~~text
faturas/
├── Fatura.java
├── Faturas.java
├── FiltrosFaturas.java
├── ResumoFatura.java
├── FaturaConstante.java
├── CalculoFatura.java
├── ValoresFatura.java
├── ValoresItemFatura.java
├── ItemFatura.java
├── Quantidade.java
├── VinculosFatura.java
├── FormaPagamento.java
├── FalhaFatura.java
├── FaturaInvalida.java
├── http/
│   ├── TkCadastroFatura.java
│   ├── TkConsultaFatura.java
│   ├── TkListaFaturas.java
│   ├── TkSimulacaoFatura.java
│   ├── PedidoFatura.java
│   └── RespostasFatura.java
├── pg/
│   ├── PgFatura.java
│   ├── PgFaturas.java
│   ├── ConsultaSqlDeFaturas.java
│   ├── ConsultaSqlDeResumosFaturas.java
│   └── CabecalhoFatura.java
└── descontos/
    ├── Desconto.java
    ├── DescontoReconstituido.java
    ├── DescontoFixo.java
    ├── DescontoPercentual.java
    ├── SemDesconto.java
    ├── TipoDesconto.java
    └── DescontoContratado.java
~~~

`Fatura` define a entidade identificada e delega seus cálculos a `CalculoFatura`; `Faturas` define cadastro, consulta, simulação e listagem da coleção. `VinculosFatura` representa cliente e forma de pagamento opcionais, com as mesmas cinco modalidades do anêmico. `PgFaturas` grava cabeçalho e itens na mesma transação, usando as tabelas existentes. `FaturaConstante` conserva os valores materializados na operação, sem consultas por atributo. Cadastro, GET por código, simulação e listagem filtrada já estão expostos. A [comparação final com o anêmico](../documentacao/Validacao-Equivalencia-Modelos.md) foi concluída na etapa 21.

`PgFaturas` recebe `DataSource`, `ProdutosNaConexao` e `ClientesNaConexao` no construtor primário. O construtor que recebe somente o banco monta as implementações padrão e delega ao primário. `AplicacaoRica` fornece os objetos de produtos e clientes já compostos na montagem da API. Os três atributos são finais, e a construção não executa consultas.

Os contratos `ProdutosNaConexao` e `ClientesNaConexao` pertencem aos pacotes PostgreSQL. Recebem a conexão da operação e distinguem consulta protegida para cadastro de leitura sem bloqueio para simulação. `PgFaturas` abre, confirma, desfaz e encerra sua conexão; os colaboradores a utilizam sem assumir esse controle. Assim, a injeção preserva a transação do cadastro e a leitura coerente da simulação.

`CalculoFatura` exige itens não vazios e produtos distintos. Sua construção valida a composição sem consultar produtos. `preparar()` calcula os valores para cadastro ou simulação e rejeita desconto fixo acima do subtotal inicial; `calcular()` recalcula com os preços observados e limita o desconto fixo quando necessário. Esses cálculos podem ser usados sem inventar código ou data de uma fatura salva.

`ItemFatura` recebe a coleção `Produtos`, o código do produto e uma `Quantidade` positiva com até 15 dígitos inteiros e quatro casas decimais. A cada cálculo, recupera o preço e produz `ValoresItemFatura`. `ValoresFatura` reúne esses valores, soma os subtotais exatos, aplica o desconto e arredonda somente o total final. Por exemplo, `1.01 × 0.5 + 1.05 × 0.5` resulta em `1.03`; os itens não são arredondados separadamente.

Os valores de um resultado não fazem novas consultas. Quando o item recebe a coleção de produtos, uma nova chamada de cálculo lê novamente os produtos. Quando recebe um produto materializado, conserva o preço daquela leitura. O cadastro usa produtos carregados em conjunto na sua transação e materializa o retorno antes do commit. O GET usa uma consulta conjunta de cabeçalho, itens e produtos; uma nova requisição lê os preços atuais, e os campos de uma mesma resposta usam os valores materializados dessa leitura.

Os descontos usam composição pelo contrato `Desconto`. `DescontoPercentual` calcula o abatimento sem arredondamento intermediário. `DescontoFixo` mantém seu valor original e limita o abatimento ao subtotal atual; quando os preços sobem, o valor original volta a ser aplicado até esse limite. `SemDesconto` representa ausência de abatimento. O desconto percentual segue a regra inicial; a proposta de mínimo de R$ 200,00 foi descartada pelo usuário.

`Desconto.registro()` fornece tipo e valor original para persistência; a ausência de registro representa `SemDesconto`. `DescontoReconstituido` compõe a estratégia correspondente ao registro e delega cálculo, validação do subtotal inicial e registro. Esse decorator substitui a fábrica estática `Desconto.reconstituir`. Um desconto fixo ou percentual de zero continua tendo registro, preservando sua diferença em relação à ausência de desconto.

`DescontoReconstituido` é final, mantém um atributo final e possui quatro construtores e os três métodos de comportamento do contrato `Desconto`. Pode ser construído para ausência, registro presente, registro opcional ou estratégia já composta. Os construtores secundários delegam ao primário, que conserva a estratégia recebida. Na leitura PostgreSQL, `CabecalhoFatura` compõe o desconto a partir das colunas existentes; as fórmulas e validações continuam nas estratégias, sem novas consultas.

`CabecalhoFatura` recebe a linha SQL no construtor e delega ao construtor canônico com código, data, desconto e vínculos já materializados. Substitui a fábrica estática `daLinha`, conserva quatro componentes imutáveis e compõe a fatura com os itens da mesma consulta. O cursor SQL não fica guardado no cabeçalho nem na fatura. Cliente e forma de pagamento podem estar presentes ou ausentes de forma independente.

Os testes de cálculo, quantidade, item e descontos podem ser executados sem PostgreSQL pelo comando de testes do módulo.

## Objetos de valor da precificação

`Dinheiro` representa custo ou preço de venda positivo, com até 17 dígitos inteiros e duas casas decimais. `Percentual` aceita zero, até 15 dígitos inteiros e quatro casas. Ambos rejeitam valores ausentes ou fora desses limites e normalizam a escala, aceitando zeros decimais excedentes, sem arredondar entradas inválidas. A escala normalizada faz valores equivalentes terem a mesma igualdade e o mesmo hash.

Os valores podem ser construídos a partir de `BigDecimal`, texto decimal ou inteiro. Os construtores secundários delegam ao principal, que concentra validação e normalização, conforme a decisão local de validar valores na construção. Cada objeto mantém um único atributo de instância, sem setters.

`comparar` expressa a ordem entre valores do mesmo tipo. `Percentual.fracao()` produz a proporção decimal exata usada pelas estratégias de acréscimo e margem. `Precificacao.mesmosValores()` usa essas comparações. `Precificacoes` compõe os valores e a estratégia; as verificações de limite e precisão pertencem aos valores. A regra entre custo e venda e o arredondamento do resultado permanecem no fluxo de precificação.

`ValorInvalido` descreve a violação, e o cálculo a traduz para `PrecificacaoInvalida`, preservando a causa e a mensagem específica de custo, venda ou percentual. O limite inferior a 100% continua exclusivo da margem sobre venda; acréscimos maiores continuam permitidos.

## Textos do cadastro de clientes

`TextoCliente<T>` é o contrato interno para produzir um valor validado a partir de texto. `TextoObrigatorio` mantém campo, conteúdo e limite; seu método `valor()` exige presença, remove espaços das extremidades e rejeita comprimento excedente ou caractere nulo. `TextoOpcional` compõe esse comportamento quando houver conteúdo e conserva a ausência quando o texto estiver vazio ou em branco.

As duas classes são finais e possuem dois construtores e um método de comportamento. O primário recebe e conserva os valores ou objetos compostos; o secundário prepara a entrada e delega ao primário. `TextoObrigatorio` possui três atributos finais, e `TextoOpcional`, um. Não há métodos estáticos nesses objetos.

`NomeCliente`, `Cpf`, `Email`, `Telefone`, `Cep` e `LocalizacaoEndereco` usam esses objetos. Os valores públicos continuam validando na construção, conforme a decisão local, e mantêm os mesmos campos e mensagens. `TextoCliente`, `TextoObrigatorio` e `TextoOpcional` permanecem internos ao pacote de clientes, sem dependência de HTTP ou PostgreSQL.

`TelefoneOpcional` substitui a fábrica estática `Telefone.opcional` no cadastro e na leitura do cliente. Ao solicitar `valor()`, conteúdo nulo ou em branco produz ausência; conteúdo presente compõe `Telefone`, que mantém a validação de formato e limite e a remoção de espaços nas extremidades. O objeto é final, conserva um único atributo final e possui dois construtores, com delegação ao primário, e um método de comportamento. A regra de telefone não foi duplicada. No PATCH, o campo omitido continua preservado, `null` solicita remoção e texto em branco continua inválido.

`Cpf` mantém a validação de formato, a remoção da máscara e a rejeição de sequências repetidas. A conferência dos dois dígitos é delegada a `DigitosVerificadoresCpf`, que compõe duas instâncias de `DigitoVerificadorCpf`. Cada dígito confere o resultado do cálculo sobre seu prefixo; o segundo inclui o primeiro dígito recebido, e ambos precisam estar corretos. Os auxiliares estáticos de cálculo foram removidos de `Cpf`.

Os dois objetos de conferência são finais e internos ao pacote de clientes, sem getters ou setters. Cada um mantém dois atributos finais, dois construtores e um método de comportamento. Os construtores secundários compõem os objetos ou separam prefixo e dígito, delegando ao primário, que conserva esses valores. `Cpf` continua validando na construção e produzindo o mesmo valor normalizado e as mesmas mensagens.

## Alterações opcionais do cliente

`AlteracaoCampo<T>` é o contrato das operações de preservação, remoção e substituição. `CampoPreservado` mantém o resultado recebido e não executa a escrita. `CampoRemovido` executa a operação com ausência de valor. `CampoSubstituido` executa a operação com o novo valor, que não pode ser nulo.

`aplicar` permite compor a operação com o trabalho do consumidor. No SQL do cliente, a alteração de telefone acrescenta a coluna apenas quando a operação tiver sido solicitada. As consultas `informada()` e `valor()` são derivadas desse comportamento e mantêm a adaptação existente do endereço. A borda HTTP cria os objetos correspondentes a campo omitido, `null` ou valor informado, sem fábricas estáticas de `AlteracaoCampo`.

Os três tipos são finais. Preservação e remoção são políticas sem estado; substituição conserva uma referência final ao novo valor. Não foram acrescentados atributos ou construtores artificiais para os tipos sem estado. O contrato HTTP e as regras de atualização parcial permanecem os mesmos.

## Objetos de consulta

`PgProduto` representa o registro no PostgreSQL e pode consultar nome e precificação sob demanda. A coleção `PgProdutosConstantes`, usada pela API, carrega os dados do produto em um único `SELECT` e passa o mesmo `DataSource` ao `PgProduto`.

O decorador `ProdutoConstante` envolve esse `PgProduto`, delega `codigo()` e devolve o nome e a `Precificacao` já carregados. Uma nova requisição faz uma nova leitura. O desenho segue o exemplo de [SQL-speaking objects de Bugayenko](https://www.yegor256.com/2014/12/01/orm-offensive-anti-pattern.html#what-about-performance).

## Cadastrar uma fatura

Envie um POST para `http://127.0.0.1:8081/api/faturas`, com `Content-Type: application/json`. Substitua os códigos abaixo pelos produtos e cliente cadastrados no banco do rico:

~~~json
{
  "itens": [
    {"codigoProduto": 1, "quantidade": 0.5},
    {"codigoProduto": 2, "quantidade": 1.25}
  ],
  "desconto": {"tipo": "PERCENTUAL", "valor": 12.3456},
  "codigoCliente": 1,
  "formaPagamento": "PIX"
}
~~~

A resposta `201 Created` contém `codigo`, `dataCriacao`, `itens`, `subtotal`, `descontoAplicado` e `total`. Cada item informa `codigoProduto`, `quantidade`, `precoVenda` e `subtotal`, na ordem enviada. `codigoCliente` e `formaPagamento` aparecem somente quando informados.

Desconto, cliente e pagamento são opcionais e aceitam `null`. O desconto também pode ter tipo `VALOR_FIXO`; na criação, seu valor não pode superar o subtotal. São aceitos PIX, DINHEIRO, CARTAO_DEBITO, CARTAO_CREDITO e BOLETO. A fatura exige ao menos um item e cada produto pode aparecer uma única vez. O item armazena a referência ao produto e a quantidade; o preço unitário não é gravado na fatura.

Dados inválidos retornam `400`; produto ou cliente inexistente retorna `404`; falha de persistência retorna `503`. A validação das referências, o cálculo inicial, a gravação e a leitura do retorno usam a mesma conexão. Uma falha desfaz cabeçalho e todos os itens.

## Consultar uma fatura pelo código

Envie um GET para `http://127.0.0.1:8081/api/faturas/1`, substituindo `1` pelo código devolvido no cadastro. A resposta `200 OK` tem os mesmos campos do POST e acompanha os preços atuais dos produtos. Cliente e pagamento continuam ausentes do JSON quando não foram informados.

No desconto fixo, uma redução de preços limita o abatimento ao subtotal atual. Se os preços subirem, o desconto original volta a ser aplicado até esse limite. Por exemplo, um desconto contratado de `30.00` sobre subtotal de `100.00` resulta em total de `70.00`. Se o subtotal cair para `12.34`, o total passa a `0.00`; ao subir para `50.00`, o desconto volta a `30.00` e o total fica em `20.00`. A consulta preserva o valor contratado no banco.

O GET faz uma única consulta e não grava dados. Os subtotais e o desconto mantêm precisão; somente o total final é arredondado. Código inválido retorna `400`, fatura inexistente retorna `404` e falha de leitura retorna `503` genérico. Uma fatura armazenada sem itens também retorna `503`, sem alterar seus registros.

## Simular uma fatura

Envie um POST para `http://127.0.0.1:8081/api/faturas/simulacao` com `Content-Type: application/json`, usando o mesmo corpo do cadastro mostrado acima. A resposta `200 OK` contém somente `itens`, `subtotal`, `descontoAplicado` e `total`.

Cliente e pagamento, quando informados, passam pelas mesmas validações do cadastro, mesmo sem aparecer na resposta. O desconto fixo não pode superar o subtotal inicial. Entradas inválidas retornam `400`, referências inexistentes retornam `404` e falhas de leitura retornam `503`.

A simulação consulta os preços atuais e reutiliza a preparação e os objetos de cálculo do cadastro. A operação é somente de leitura: não cria fatura, itens ou código. O JSON e suas validações são compartilhados por `PedidoFatura`; as fórmulas continuam no domínio.

## Listar faturas por período e cliente

Envie um GET para `http://127.0.0.1:8081/api/faturas`. Os filtros são opcionais e podem ser combinados:

- `inicio=2026-11-01`: inclui faturas a partir desse dia.
- `fim=2026-11-30`: inclui todo esse dia, até seu último instante.
- `codigoCliente=1`: inclui somente as faturas desse cliente.

Exemplo: `http://127.0.0.1:8081/api/faturas?inicio=2026-11-01&fim=2026-11-30&codigoCliente=1`.

A resposta `200 OK` é uma lista de resumos com `codigo`, `dataCriacao` e `quantidadeItens`; `codigoCliente` e `formaPagamento` aparecem quando presentes. A quantidade de itens conta os produtos distintos, sem somar as quantidades fracionárias. A ordenação é por data e código decrescentes.

Os filtros combinam com `AND`. Parâmetros omitidos ou vazios são ignorados. Datas devem estar entre `0001-01-01` e `9999-12-31`; código de cliente deve ser inteiro positivo. Data inválida, intervalo invertido ou código inválido retorna `400`. Cliente positivo sem faturas, inclusive inexistente, retorna `[]`. Falha de leitura retorna `503` genérico.

`FiltrosFaturas` valida o período e a referência. `ConsultaSqlDeResumosFaturas` busca cabeçalhos e conta os itens em uma consulta, sem carregar produtos ou recalcular faturas. A listagem não grava dados.

## Rodar a API

Com o servidor PostgreSQL ativo, execute na raiz do repositório. A API prepara o banco antes de abrir a porta HTTP:

~~~powershell
$env:DB_PASSWORD = "senha-do-banco"
mvn -s configuracao-maven.xml -pl precificacao-rica package
java -jar precificacao-rica/target/precificacao-rica-1.0.0-SNAPSHOT.jar
~~~

No Git Bash, use `export DB_PASSWORD='senha-do-banco'` antes dos mesmos comandos Maven e Java.

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

Na leitura PostgreSQL, `ResultadoSqlDeCategoria` substitui o auxiliar estático de `ResultadoSqlDeProduto`. O objeto mantém somente a referência opcional lida, com um atributo final, dois construtores e um método que compõe `CodigoCategoria` ou conserva a ausência. `PgProduto` e `ResultadoSqlDeProduto` utilizam esse comportamento sem consultas adicionais. O produto individual acompanha o vínculo atual; o produto materializado conserva o vínculo da sua leitura.

`CodigoCategoria` e `CodigoCidade` compõem `InteiroExato`, do pacote `numeros`, para converter as entradas decimais. Cada código conserva a exigência de valor positivo no construtor canônico e fornece sua própria mensagem. O objeto compartilhado mantém dois atributos finais, dois construtores e um método de conversão. Ausência, fração e estouro numérico continuam rejeitados na construção do código. A conversão não usa ponto flutuante, truncamento ou arredondamento e preserva a causa dos erros numéricos.

## Listar produtos

Envie um GET para `http://127.0.0.1:8081/api/produtos`. A resposta `200 OK` contém os produtos em ordem crescente de código, com `codigoCategoria` quando houver vínculo. Quando não houver produtos, a API devolve `[]`.

Os filtros `nome`, `modalidadePrecificacao` e `categoria` são opcionais e podem ser combinados. Por exemplo, `GET /api/produtos?nome=cafe&modalidadePrecificacao=ACRESCIMO_SOBRE_CUSTO&categoria=2` devolve apenas os produtos que atendem aos três critérios. O filtro de nome busca um trecho sem distinguir maiúsculas de minúsculas e trata `%` e `_` como caracteres literais.

Os espaços nas extremidades do nome são removidos; nome em branco não restringe a consulta. Essa normalização pertence a `FiltrosProdutos` e vale também fora do HTTP. Modalidade ou código de categoria inválidos retornam `400`; nenhuma correspondência retorna `200` com `[]`. A listagem da API materializa os valores em uma consulta SQL, e uma nova requisição lê os dados atuais.

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

A validação e a chave estrangeira impedem gravar um vínculo com categoria inexistente, inclusive se ela for removida entre a consulta e a escrita. Falhas de persistência de outra origem retornam `503`. Uma nova consulta acompanha a troca; o decorador `ProdutoConstante` conserva os dados da leitura em que foi criado.

## Cadastrar uma categoria

Com a API iniciada, envie um POST no Postman para `http://127.0.0.1:8081/api/categorias` com `Content-Type: application/json`. A migração V3 é aplicada durante a preparação do banco.

~~~json
{
  "nome": "Alimentos"
}
~~~

A API responde com `201 Created` e devolve `codigo` e `nome`. O nome é obrigatório, tem até 200 caracteres após remover espaços das extremidades e não aceita o caractere nulo. Nome inválido ou já cadastrado retorna `400`, mesmo que a capitalização seja diferente. Falha de persistência retorna `503`, sem expor detalhes do banco.

Na persistência, `categorias.pg.CategoriaDaLinha` compõe a categoria com código e nome materializados, substituindo a fábrica estática `PgCategorias.categoriaDoResultado`. O objeto mantém dois atributos finais, dois construtores e um método de comportamento. Cadastro, renomeação e listagem utilizam a mesma composição, sem guardar o cursor SQL nem acrescentar consultas. Cada objeto retornado conserva os valores da sua leitura, mesmo depois de novas renomeações.

## Listar categorias

Envie um GET para `http://127.0.0.1:8081/api/categorias`. A resposta `200 OK` traz os campos `codigo` e `nome` em ordem crescente de código. Se ainda não houver categorias, a resposta é `[]`.

## Renomear uma categoria

Envie um PATCH para `http://127.0.0.1:8081/api/categorias/1` com `Content-Type: application/json`. Substitua `1` pelo código da categoria e informe o novo nome.

~~~json
{"nome": "Alimentos e bebidas"}
~~~

A resposta `200 OK` devolve `codigo` e `nome`. É possível reenviar o próprio nome ou mudar sua capitalização. Um nome já usado por outra categoria retorna `400`, mesmo que a capitalização seja diferente. Categoria inexistente retorna `404`.

A alteração preserva o código, os produtos vinculados e seu histórico de precificação. Reutiliza a validação do cadastro e aceita somente o campo `nome`. Outras falhas de persistência retornam `503` e conservam o cadastro anterior.

`FalhaNaRenomeacaoCategoria` substitui o auxiliar estático de classificação das falhas SQL. O objeto final mantém dois atributos finais, dois construtores e um método de tradução. Somente a violação de unicidade da restrição `ux_categoria_nome`, identificada pelos metadados do PostgreSQL, representa nome duplicado. As demais falhas continuam como indisponibilidade, com o mesmo logger, texto e severidade de registro. A causa original é preservada; detalhes da exceção não são anexados ao log.

## Cadastrar uma cidade

Envie um POST para `http://127.0.0.1:8081/api/cidades` com `Content-Type: application/json`.

~~~json
{"nome": "Curitiba", "uf": "pr"}
~~~

A resposta `201 Created` devolve `codigo`, `nome` e `uf`. A UF é normalizada para maiúsculas e o nome aceita até 200 caracteres após remover espaços das extremidades. O caractere nulo é rejeitado antes de acessar o banco. A API retorna `400` para nome ou UF inválidos e para nomes repetidos na mesma UF, mesmo com capitalização diferente. O mesmo nome pode ser usado em UFs distintas. Outras falhas de persistência retornam `503`, sem expor detalhes do banco.

`SiglaUf` interpreta o texto informado no cadastro e no filtro da listagem e produz `UnidadeFederativa`. O objeto final mantém um atributo final, dois construtores e um método de comportamento, substituindo a fábrica estática `UnidadeFederativa.daSigla`. Remove espaços nas extremidades e normaliza as letras de forma independente do idioma do sistema. As mensagens para ausência e sigla inválida, assim como suas causas, foram preservadas. Na listagem, UF vazia ou em branco continua sendo tratada como ausência de filtro antes da interpretação.

Na persistência, `ResultadoSqlDeCidade` compõe a cidade com código, nome e UF materializados, substituindo a fábrica estática `PgCidades.cidadeDoResultado`. O objeto mantém três atributos finais, dois construtores e um método de comportamento. É utilizado no retorno do cadastro e na listagem, sem guardar o cursor SQL nem acrescentar consultas. Os objetos já retornados conservam os valores da sua leitura; uma nova consulta recebe o estado atual do registro.

## Listar cidades

Envie um GET para `http://127.0.0.1:8081/api/cidades`. A resposta `200 OK` contém `codigo`, `nome` e `uf` de cada cidade em ordem crescente de código. Quando não há cidades cadastradas, a resposta é `[]`. Se o banco estiver indisponível, a API retorna `503`.

Use `GET /api/cidades?nome=curitiba` para buscar por trecho do nome, sem distinguir maiúsculas de minúsculas. Espaços nas pontas são removidos. Um filtro vazio ou em branco equivale a listar todas as cidades. O texto pode ter até 200 caracteres e `%` e `_` são tratados como caracteres literais. Os parâmetros são lidos em UTF-8, preservando acentos também no Windows; use a codificação de URL do Postman para símbolos como `+`, `&` e `%`. Se nenhuma cidade corresponder ao filtro, a resposta é `200 OK` com `[]`.

Use `GET /api/cidades?uf=PR` para filtrar pela UF ou `GET /api/cidades?nome=curitiba&uf=PR` para exigir os dois critérios. A UF aceita minúsculas e espaços nas pontas. UF vazia ou em branco equivale a omitir o parâmetro; uma sigla inválida retorna `400` antes de acessar o banco.

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

Nome, CPF e e-mail são obrigatórios. Telefone e endereço podem ser omitidos ou informados como `null` e são devolvidos como `null` quando ausentes. Telefone em branco também é tratado como ausente. Ao informar um endereço, apenas o complemento é opcional. Dados inválidos, CPF repetido ou e-mail já cadastrado com qualquer capitalização retornam `400`. Cidade inexistente retorna `404`; outras falhas de persistência retornam `503`.

## Listar clientes

Envie um GET para `http://127.0.0.1:8081/api/clientes`. A resposta `200 OK` traz os clientes em ordem crescente de código, com os mesmos campos do cadastro. Quando não houver clientes, a resposta é `[]`. Se o banco estiver indisponível, a API retorna `503`.

## Consultar um cliente pelo código

Envie um GET para `http://127.0.0.1:8081/api/clientes/1`, substituindo `1` pelo código do cliente. A resposta `200 OK` contém os mesmos campos do cadastro. Código inválido retorna `400`, cliente inexistente retorna `404` e banco indisponível retorna `503`.

Cada nova consulta usa os dados atuais do banco, incluindo os campos opcionais. Dados inválidos armazenados no registro também retornam `503`, com mensagem genérica.

## Atualizar contatos e endereço de um cliente

Envie um PATCH para `http://127.0.0.1:8081/api/clientes/1` com `Content-Type: application/json`. Substitua `1` pelo código do cliente.

~~~json
{"email": "Novo.Email@example.com"}
~~~

Envie `email`, `telefone`, `endereco` ou uma combinação deles. Campos omitidos conservam o valor atual. Para remover o telefone, envie `{"telefone":null}`. Um telefone informado como texto deve ser um número brasileiro com DDD. O e-mail, quando enviado, precisa ser válido e não pode ser nulo.

Para substituir o endereço, envie o objeto completo. Apenas o complemento é opcional; se omitido na substituição, ele passa a nulo. Por exemplo:

~~~json
{
  "endereco": {
    "cep": "80000-000",
    "logradouro": "Rua das Flores",
    "numero": "12A",
    "bairro": "Centro",
    "codigoCidade": 1
  }
}
~~~

Use o código de uma cidade cadastrada. Para remover o endereço inteiro, envie `{"endereco":null}`. Um endereço parcial é rejeitado. Nome e CPF continuam fora do PATCH.

A resposta `200 OK` devolve o cliente atualizado. Dados inválidos ou e-mail já usado por outro cliente retornam `400`. A comparação de e-mails ignora maiúsculas e minúsculas. Cliente ou cidade inexistente retorna `404`; falha de persistência retorna `503`. Endereço e contatos são alterados na mesma transação: um erro preserva o cadastro anterior por inteiro.

É permitido mudar somente a capitalização do próprio e-mail. Outras falhas de persistência retornam `503`.

## Banco de dados

O banco desta implementação é `precificacao_rica`, separado de `precificacao_anemica`. Os dados e o histórico de migrações são independentes.

Na inicialização, a aplicação verifica o banco configurado, cria somente quando ele está ausente e a opção está habilitada, e aplica as migrações Flyway. As rotas só ficam disponíveis depois da preparação. Reiniciar conserva os registros e aplica apenas migrações pendentes.

| Variável | Padrão | Finalidade |
| --- | --- | --- |
| `RICO_DB_URL` | `jdbc:postgresql://localhost:5432/precificacao_rica` | Banco da implementação rica, com nome explícito na URL. |
| `RICO_DB_USER` | `postgres` | Usuário das conexões, criação e migrações. |
| `DB_PASSWORD` | Obrigatória | Senha do usuário configurado; não é gravada no código. |
| `RICO_DB_CRIAR_SE_AUSENTE` | `true` | Aceita `true` ou `false`; controla somente a criação do banco ausente. |
| `RICO_DB_MANUTENCAO` | `postgres` | Banco existente no mesmo servidor usado para criar o banco ausente. |
| `RICO_SERVER_PORT` | `8081` | Porta HTTP; o endereço de escuta continua sendo `127.0.0.1`. |

O servidor PostgreSQL precisa estar ativo. Para criar um banco, o usuário precisa de `CREATEDB`. Se o banco já existir, essa permissão e a conexão ao banco de manutenção não são necessárias; continuam necessárias as permissões de uso e migração do banco da aplicação. Senha incorreta ou servidor indisponível interrompem a inicialização sem tentar criar o banco.

Para exigir a preparação manual do banco, defina `RICO_DB_CRIAR_SE_AUSENTE=false`. Se ele não existir, a aplicação encerra com a causa específica. O Flyway continua validando e aplicando as migrações em bancos existentes. Falha de migração impede a publicação da API e fecha o pool de conexões.

`BancoPostgres` coordena a preparação; `CriacaoBancoPostgres` executa a criação; `ConexoesPostgres` abre as conexões; `EnderecoPostgres` protege o nome SQL e preserva os servidores e as opções da URL ao trocar o banco de manutenção. Os objetos ficam em `infraestrutura/banco`. Seus construtores não fazem acesso ao PostgreSQL.

## Aplicar as migrações manualmente

Essa alternativa serve para uma manutenção explícita em um banco já existente. Iniciar a API não exige executar este comando antes. É necessário JDK 17 ou superior e Maven 3.6.3 ou superior. Em PowerShell:

~~~powershell
cd C:\Java\Workspace\precificacao
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

O plugin é executado pelos comandos acima; compilar o projeto não prepara o banco da API. A preparação automática ocorre ao executar o JAR. O Flyway também está entre as dependências de execução do módulo.

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

O comando padrão executa somente os testes sem PostgreSQL, mesmo quando as variáveis de banco estão definidas. O fake reutilizável `DataSourceSemConexao` falha se uma validação ou construção tentar abrir uma conexão:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-rica test
~~~

Para executar a suíte completa, use o perfil `integracao`. O servidor PostgreSQL e um banco exclusivo, por exemplo `precificacao_rica_teste`, precisam existir. O perfil aplica e valida as migrações nesse banco antes dos testes; não é necessário executar `flyway:migrate` separadamente.

No Git Bash, configure as quatro variáveis no mesmo terminal, na raiz do projeto:

~~~bash
export RICO_BANCO_TESTE_URL='jdbc:postgresql://localhost:5432/precificacao_rica_teste'
export RICO_BANCO_CRIACAO_TESTE_URL='jdbc:postgresql://localhost:5432/postgres'
export RICO_BANCO_TESTE_USUARIO='postgres'
export RICO_BANCO_TESTE_SENHA='senha-do-banco-de-teste'
mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test
~~~

Em PowerShell:

~~~powershell
$env:RICO_BANCO_TESTE_URL = "jdbc:postgresql://localhost:5432/precificacao_rica_teste"
$env:RICO_BANCO_CRIACAO_TESTE_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:RICO_BANCO_TESTE_USUARIO = "postgres"
$env:RICO_BANCO_TESTE_SENHA = "senha-do-banco-de-teste"
mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test
~~~

Use a senha do banco escolhido. Ela deve estar definida, e o servidor exclusivo de testes precisa exigir autenticação por senha para validar a senha incorreta. As URLs precisam ser PostgreSQL, e o usuário não pode estar em branco. A preparação da base de testes usa exclusivamente `RICO_BANCO_TESTE_*`; não recorre ao banco da API, às variáveis `FLYWAY_*` nem aos padrões do plugin de migração.

`RICO_BANCO_CRIACAO_TESTE_URL` aponta para o banco de manutenção de um **servidor exclusivo de testes**. Os testes de inicialização criam e removem bancos e papéis temporários com prefixo `rico_teste_`. Exigem um usuário administrativo, inclusive para coordenar duas criações concorrentes por um bloqueio observado no catálogo PostgreSQL. A limpeza dos bancos temporários usa `DROP DATABASE ... WITH (FORCE)`, disponível a partir do PostgreSQL 13. As credenciais são as mesmas de `RICO_BANCO_TESTE_USUARIO` e `RICO_BANCO_TESTE_SENHA`.

O perfil exige a suíte completa. Configuração ausente, opções para pular ou ignorar falhas, filtros de seleção de testes, casos ignorados e suítes ou casos obrigatórios ausentes provocam falha no comando. Os relatórios ficam em `target/surefire-integracao/<identificador-da-execucao>`, sem reaproveitar relatórios de execuções anteriores. O modo unitário usa `target/surefire-reports`.

Os casos PostgreSQL recebem `@Tag("integracao")`, na classe inteira ou no método, conforme a dependência. A lista de suítes e dos métodos de banco das classes mistas está em [src/test/ant/integracao.xml](src/test/ant/integracao.xml). Ao acrescentar, renomear ou reorganizar esses testes, atualizar a lista e manter os casos sem banco fora da tag. `PrepararBancoIntegracao` aplica as migrações; `VerificarIntegracao` confere os relatórios. São ferramentas do processo de teste e não entram na contagem de casos JUnit.

`ApiProdutosTest` inicia a API em porta temporária, confere cadastro e consulta pelo `Location` nas duas modalidades e verifica alteração e histórico. `PgProdutosTest` verifica os valores persistidos, texto com acentos e apóstrofo, consulta atualizada por `PgProduto` e valores preservados pelo decorador. Os testes de categorias, cidades e clientes exercitam os contratos já implementados e removem os registros de apoio ao terminar.

`CriacaoBancoPostgresTest` cobre a API com banco novo e após reinício, criação desativada, usuário sem `CREATEDB`, senha incorreta, identificador com aspas, criação concorrente e falha de migração antes da publicação da API.

`CadastroCategoriaTest` também provoca uma falha durante a inserção para verificar que ela retorna `503`, preserva os registros anteriores e não é confundida com nome duplicado. O gatilho de teste é temporário e é removido ao terminar.

Os testes do vínculo de produto também verificam categoria omitida, atualização observada em novas consultas, preservação da leitura anterior e erros de outra referência. A remoção da categoria entre a validação e a escrita é testada com a chave estrangeira real do PostgreSQL, tanto no cadastro quanto na alteração.

`ListaProdutosTest` confere ordenação por código com inserções em outra ordem, todos os campos da resposta, nova leitura e ausência de correspondências. `SqlContado` observa os comandos JDBC da aplicação e exige um por resposta; retirar a coleção materializada somente de uma cópia temporária fez o teste detectar sete comandos para dois produtos.

`ReprecificacaoPostgresTest` verifica os três cálculos nas duas modalidades, o custo calculado gravado, a repetição sem nova escrita, a mesma conexão para produto e histórico, o desfazimento após falha na inserção do histórico e duas alterações concorrentes. Requisições simultâneas com os mesmos valores geram somente um registro. As verificações negativas em cópias temporárias detectaram a retirada da transação e do bloqueio do produto.

O teste existente de renomeação verifica o próprio nome com outra capitalização, conflito com outra categoria e preservação do produto e do histórico. `RenomeacaoCategoriaPostgresTest` provoca uma falha após a atualização, exige `503`, confere o cadastro preservado e repete a operação com sucesso após retirar a falha. Em uma cópia temporária, retirar a conferência da restrição de nome fez esse teste detectar a resposta incorreta `400`.

`CadastroCidadeTest` confere o limite válido de 200 caracteres, normalização da UF, duplicidade somente na mesma UF e rejeição de entradas inválidas antes da conexão. Também provoca uma falha de outra restrição após a inserção, exige `503`, conserva os registros anteriores e repete o cadastro com sucesso após retirar o gatilho. `ListaCidadesTest` verifica a ordem por código, a resposta vazia e a indisponibilidade; o cenário PostgreSQL exige uma conexão e uma consulta para montar a resposta, sem acesso ao banco durante a construção das rotas.

Os testes do filtro de nome também conferem acentos, apóstrofo, símbolos codificados na URL e 200 caracteres com espaços nas extremidades. O cenário com acento detectou a dependência da codificação padrão do Java no Windows; a leitura do parâmetro passou a usar UTF-8 explícito. Em uma cópia temporária, trocar a busca literal por `LIKE` sem escape fez o teste detectar uma cidade indevida.

O cenário específico de UF em `ListaCidadesTest` cadastra cidades pela API, incluindo duas com o mesmo nome em UFs diferentes. Confere filtros isolados e combinados, minúsculas com espaços, campos em branco e os mesmos códigos e campos devolvidos no cadastro. Cada busca exige uma conexão e uma consulta. Uma cópia temporária que combinava os filtros com `OR` foi rejeitada pelo teste por incluir uma cidade de outra UF.

`CadastroClientePostgresTest` confere os limites válidos dos campos, normalização, opcionais nulos e os dados relidos na listagem. Também provoca falhas de outras restrições após a inserção e remove a cidade entre a validação e a escrita para exercitar a chave estrangeira real. Os erros inesperados exigem `503` e preservação dos registros anteriores; retirar a conferência da restrição somente numa cópia fez os testes detectar `400` e `404` indevidos. O fluxo de cadastro/listagem existente exige uma conexão e uma consulta para montar a lista.

`ConsultaClienteTest` reúne as consultas com e sem endereço, a leitura de dados atualizados, o cliente removido e a continuidade da listagem. Cada GET exige uma conexão e uma consulta. Também confere `503` para um registro existente com telefone inválido e a preservação dessa linha após a leitura. Em uma cópia temporária, classificar falhas de consulta como cliente ausente fez dois casos detectarem `404` indevido.

`AlteracaoEmailPostgresTest` concentra as verificações de e-mail antes misturadas ao cadastro. Confere limite válido de 254 caracteres, capitalização, duplicidade, preservação dos dados e escrita somente da coluna solicitada. Também força falha de outra restrição e falha na materialização depois do UPDATE, com um marcador que comprova a escrita alcançada. A transação preserva o e-mail anterior; retirá-la somente numa cópia temporária fez o teste detectar o valor novo indevidamente gravado após `503`.

`AlteracaoTelefonePostgresTest` reúne substituição, remoção, omissão e envio conjunto dos contatos, preservando os demais dados do cliente. Confere também que um conflito de e-mail impede alterar o telefone e que uma falha após a escrita desfaz os dois contatos. Nos casos concorrentes, observa as duas requisições aguardando o bloqueio da mesma linha pelo grafo de `pg_blocking_pids` e verifica os valores finais no banco. Uma cópia que gravava valores antigos foi rejeitada pelos dois casos concorrentes, para telefone substituído e removido.

Na validação final da etapa 21 em 10 de novembro de 2026, passaram **256 casos sem banco, em 22 suítes**, e **368 na suíte completa, em 36 suítes**, sem falhas, erros ou ignorados. A suíte completa inclui os casos sem banco; as contagens não representam cobertura nem precisam coincidir com as do anêmico. Os dois JARs foram gerados e executados contra bancos novos para a comparação por HTTP.

`FiltroProdutosUtf8Test` executa a rota de listagem em outra JVM, com codificação padrão Windows-1252. Confere acentos, símbolos e parâmetros combinados. O teste reproduziu o erro anterior de decodificação e passou após a leitura explícita em UTF-8.

Na etapa 1 da revisão de modelagem, também em 10 de novembro de 2026, passaram **273 casos sem banco em 23 suítes** e **385 completos em 37 suítes**, sem falhas, erros ou ignorados. A suíte completa inclui os casos sem banco. `ValoresDePrecificacaoTest` verifica diretamente as invariantes, os limites válidos, a fração exata e a igualdade numérica dos valores. `PrecificacoesTest` também confere as duas direções dos cálculos fracionários e a tradução das falhas com mensagem e causa preservadas. A preparação automática, a persistência, o histórico e as faturas foram exercitados em PostgreSQL descartável. Os próximos ajustes de modelagem aguardam aprovação.

Na etapa 2 da revisão de modelagem, passaram **273 casos sem banco em 23 suítes** e **389 completos em 38 suítes**, sem falhas, erros ou ignorados. `ComposicaoFaturasPostgresTest` acrescenta quatro casos para cadastro e simulação com colaboradores recebidos pelo construtor: uso da mesma conexão, seleção do modo protegido ou somente de leitura, resultado calculado e recusa de referência sem consultar produtos ou gravar. A consulta real de produtos é composta com `DataSourceSemConexao`, de modo que abrir outra conexão faz o teste falhar; o SQL usa a conexão fornecida por `PgFaturas`. A construção dos dois caminhos do construtor também permanece verificada sem banco. O perfil completo exige a nova suíte. Os ajustes da etapa 3 aguardam aprovação.

Na etapa 3 da revisão de modelagem, passaram **276 casos sem banco em 24 suítes** e **392 completos em 39 suítes**, sem falhas, erros ou ignorados. `TextosClienteTest` acrescenta três regressões: espaços Unicode nas extremidades com preservação do conteúdo interno, tratamento distinto de texto obrigatório e opcional em branco acima do limite, e rejeição de caractere nulo no complemento. Os mesmos casos passaram antes e depois da refatoração. Os testes existentes de cadastro, consulta e alteração de clientes, inclusive limites e mensagens, foram preservados. O perfil completo exige a nova suíte. A etapa 4 aguarda aprovação.

Na etapa 4 da revisão de modelagem, passaram **280 casos sem banco em 25 suítes** e **396 completos em 40 suítes**, sem falhas, erros ou ignorados. `AlteracoesCampoTest` verifica as três operações compondo uma escrita, a execução somente para campos informados, as consultas derivadas e a rejeição de substituição nula. Os testes PostgreSQL existentes de contatos e endereço também passaram, incluindo escrita apenas das colunas enviadas, desfazimento da operação e atualizações concorrentes. O perfil completo exige a nova suíte. A etapa 5 aguarda aprovação.

Na etapa 5 da revisão de modelagem, passaram **283 casos sem banco em 26 suítes** e **399 completos em 41 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. `TelefoneOpcionalTest` acrescenta três casos para ausência, formatação preservada e rejeição do conteúdo inválido pela composição com `Telefone`. Os testes HTTP e PostgreSQL existentes de cadastro, consulta e alteração de clientes também passaram, inclusive a rejeição de texto em branco no PATCH. O perfil completo exige a nova suíte. O PostgreSQL temporário foi removido. A etapa 6 aguarda aprovação.

Na etapa 6 da revisão de modelagem, passaram **295 casos sem banco em 27 suítes** e **411 completos em 42 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Os 12 casos de `CpfTest` passaram antes e depois da refatoração: normalização, restos zero e um no cálculo dos dois dígitos, zeros iniciais, falha em cada dígito, sequência repetida e formatos inválidos. O caso de primeiro dígito incorreto mantém o segundo coerente com o prefixo recebido, para detectar a omissão da primeira conferência. Os testes HTTP e PostgreSQL existentes também passaram. O perfil completo exige a nova suíte. O PostgreSQL temporário foi removido. A etapa 7 aguarda aprovação.

Na etapa 7 da revisão de modelagem, passaram **296 casos sem banco em 27 suítes** e **412 completos em 42 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Os cinco casos de reconstituição de `DescontosTest` foram adaptados para o novo objeto, preservando ausência, desconto zero, tipo, valor original e precisão. Foi acrescentada uma regressão para a validação do subtotal inicial do desconto fixo, inclusive o limite válido. Os 21 casos da suíte de descontos passaram antes e depois da troca. Os testes PostgreSQL existentes também passaram, incluindo desconto zero, preços atuais e redução e retomada do desconto fixo. O PostgreSQL temporário foi removido. A etapa 8 aguarda aprovação.

Na etapa 8 da revisão de modelagem, passaram **296 casos sem banco em 27 suítes** e **414 completos em 42 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. O teste integrado dos vínculos foi ampliado para as quatro combinações de cliente e pagamento, verificando presença independente, igualdade entre cadastro e consulta, leitura sem escrita e um único comando SQL. Também passaram os testes existentes de descontos, precisão, preços atuais e rollback. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 9 aguarda aprovação.

Na etapa 9 da revisão de modelagem, passaram **296 casos sem banco em 27 suítes** e **415 completos em 42 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Um novo caso integrado verifica ausência, vínculo e remoção da categoria nos dois caminhos de leitura, distinguindo o produto que consulta o registro atual daquele que conserva os valores materializados. Os testes HTTP, listagens e faturas existentes também passaram. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 10 aguarda aprovação.

Na etapa 10 da revisão de modelagem, passaram **305 casos sem banco em 28 suítes**, antes e depois da refatoração, e **424 completos em 43 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Os nove casos de `CodigoCategoriaTest` verificam precisão, limites, ausência, fração, código positivo, mensagens e causas pelos construtores públicos. A suíte foi registrada como obrigatória no perfil completo. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 11 aguarda aprovação.

Na etapa 11 da revisão de modelagem, passaram **311 casos sem banco em 29 suítes**, antes e depois da refatoração, e **430 completos em 44 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Os seis casos de `CodigoCidadeTest` verificam a composição com a conversão compartilhada, as mensagens de cidade, precisão, causas e código positivo. Os nove casos de categoria continuam conferindo os limites numéricos. A nova suíte foi registrada como obrigatória no perfil completo. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 12 aguarda aprovação.

Na etapa 12 da revisão de modelagem, passaram **317 casos sem banco em 30 suítes**, antes e depois da refatoração, e **436 completos em 45 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Os seis casos de `SiglaUfTest` conferem normalização Unicode sob idioma turco, ausência, vazio, branco e siglas inválidas, incluindo mensagens e causas. Os testes existentes mantêm a verificação de UF obrigatória no cadastro, filtro opcional na listagem e acesso ao banco somente após a validação. A suíte nova foi registrada como obrigatória. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 13 aguarda aprovação.

Na etapa 13 da revisão de modelagem, passaram **317 casos sem banco em 30 suítes** e **437 completos em 45 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Um novo caso integrado confirma que os objetos do cadastro e da listagem conservam seus dados depois de alterar nome e UF no banco, enquanto uma nova leitura recebe os valores atuais. Também verifica uma conexão e um comando por operação. A suíte existente já era obrigatória. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 14 aguarda aprovação.

Na etapa 14 da revisão de modelagem, passaram **317 casos sem banco em 30 suítes** e **438 completos em 45 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Um novo caso integrado confere os objetos devolvidos pelo cadastro, listagem e renomeação após duas alterações de nome, além de uma conexão e um comando por operação. A leitura mais recente recebe o nome atual. A suíte existente já era obrigatória. O JAR foi gerado e o PostgreSQL temporário removido. A etapa 15 aguarda aprovação.

Na etapa 15 da revisão de modelagem, passaram **323 casos sem banco em 31 suítes** e **444 completos em 46 suítes**, sem falhas, erros ou ignorados. O completo inclui os unitários. Os seis casos de `FalhaNaRenomeacaoCategoriaTest` conferem a classificação precisa da duplicidade, outras falhas, metadados ausentes, causa original e logs. A suíte foi registrada como obrigatória. Os testes PostgreSQL e HTTP existentes também passaram. O JAR foi gerado e o PostgreSQL temporário removido.

Na etapa 16 da revisão de modelagem, passaram **325 testes sem banco** e **446 no perfil completo**, sem falhas, erros ou casos ignorados. Os novos casos de `CidadesDaConsultaTest` conferem execução sob demanda e fechamento do cursor SQL em sucesso e falha. O JAR foi gerado e o PostgreSQL descartável foi removido.

Na etapa 17 da revisão de modelagem, passaram **332 testes sem banco** e **453 no perfil completo**, sem falhas, erros ou casos ignorados. `PrecificacaoTest` e `EntidadesMaterializadasTest` verificam as regras na construção direta dos objetos.

Na etapa 18 da revisão de modelagem, passaram **335 testes sem banco** e **456 no perfil completo**. Os novos casos verificam a ordem das especificações, as mensagens e o limite após arredondamento.

Na etapa 19 da revisão de modelagem, passaram **335 testes sem banco** e **456 no perfil completo**. O teste HTTP do histórico compara os códigos e datas da resposta com as linhas gravadas no PostgreSQL.

Na etapa 20 da revisão de modelagem, passaram **335 testes sem banco** e **456 no perfil completo**. A listagem HTTP usou uma conexão e um comando SQL. O teste integrado também verificou o preço de um produto materializado e o de um produto sob demanda após uma alteração no banco.

Na etapa 21 da revisão de modelagem, passaram **335 testes sem banco** e **456 no perfil completo**. Os testes de reprecificação confirmaram a conexão compartilhada, o desfazimento das gravações em caso de falha e a concorrência.

Na etapa 22 da revisão de modelagem, passaram **341 testes sem banco** e **462 no perfil completo**. `FalhaNaGravacaoProdutoTest` verifica a classificação de categoria ausente, a preservação da causa e o conteúdo do log de persistência.

Na etapa 23 da revisão de modelagem, passaram **341 testes sem banco** e **462 no perfil completo**. O teste de consulta lê telefone e endereço após o fechamento do cursor e verifica uma conexão e um comando.

Na etapa 24 da revisão de modelagem, passaram **341 testes sem banco** e **462 no perfil completo**. A integração conferiu a conexão compartilhada na atualização de endereço, a escrita dos campos solicitados, o desfazimento e as alterações concorrentes.

Na etapa 25 da revisão de modelagem, passaram **345 testes sem banco** e **466 no perfil completo**. Os novos casos verificam a classificação da falha e a preservação da causa quando o rollback também falha.

Na etapa 26 da revisão de modelagem, passaram **345 testes sem banco** e **466 no perfil completo**. A integração verificou uma conexão para o cadastro de fatura, incluindo itens e retorno, e o desfazimento após falha no segundo item ou na materialização.

Na etapa 27 da revisão de modelagem, passaram **353 testes sem banco** e **474 no perfil completo**. Os novos casos verificam as restrições na cadeia de exceções de gravação, a tradução de falhas de leitura e a preservação da exceção original quando o rollback também falha.

Na etapa 28 da revisão de modelagem, passaram **353 testes sem banco** e **474 no perfil completo**. As listagens de cidades, produtos e faturas usam o mesmo leitor de parâmetros UTF-8. Os testes verificam filtros combinados, espaços e a prioridade da primeira ocorrência de um parâmetro repetido.

Na etapa 29 da revisão de modelagem, passaram **355 testes sem banco** e **476 no perfil completo**. Os testes de categoria e PATCH de cliente verificam entradas inválidas, omissão, remoção e substituição. Os novos casos conferem e-mail em branco e a precedência do erro de formato.

Na etapa 30 da revisão de modelagem, passaram **355 testes sem banco** e **476 no perfil completo**. Cadastro e simulação compartilham a leitura do pedido e a conversão exata dos códigos. Os testes verificam as mensagens para códigos inválidos nas duas rotas e o intervalo inclusivo da listagem.

Na etapa 31 da revisão de modelagem, passaram **355 testes sem banco** e **476 no perfil completo**. As respostas de produto, cliente e fatura passaram a compor grupos de dados mantendo o formato público. O executor da coleção Postman conferiu **150 pares de respostas HTTP** dos dois módulos.

Na etapa 32 da revisão de modelagem, passaram **357 testes sem banco** e **479 no perfil completo**. A configuração do PostgreSQL passou a ser composta antes da abertura de conexões. Os testes conferem a opção de criação, a preservação da causa quando o banco recusa conexões e os cenários de inicialização e migração.

Na etapa 33 da revisão de modelagem, passaram **370 testes sem banco** e **492 no perfil completo**. A montagem das rotas passou a um objeto compartilhado pela aplicação e pelos testes. Os 13 novos casos verificam o retorno 405 e os métodos permitidos antes de abrir conexão.

A etapa 34 encerrou a revisão de modelagem com **369 testes sem banco** e **491 no perfil completo**, sem falhas, erros ou casos ignorados. A coleção confirmou 150 pares de respostas HTTP comparadas. O [registro final](../documentacao/Validacao-Equivalencia-Modelos.md#validacao-final-modelagem) apresenta as evidências e os exemplos de código.

## Coleção Postman e equivalência

Importe [precificacao-rica.postman_collection.json](../documentacao/postman/precificacao-rica.postman_collection.json). A variável `baseUrl` começa em `http://localhost:8081`. Execute as seis pastas na ordem pelo Collection Runner, desde a primeira requisição: ela prepara nomes, CPFs válidos e e-mails novos. Os códigos das respostas são armazenados nas variáveis desta coleção.

As 75 requisições incluem produtos e histórico, categorias, cidades, clientes, alterações parciais, faturas, precisão e preços atuais. A última pasta verifica a redução e retomada do desconto fixo e a preservação dos vínculos após alterações. A coleção cria e altera seus dados de exemplo; repetir desde o início gera novos cadastros.

A etapa 21 executou os scripts da coleção por HTTP real, com o [executor local](../verificacao/README.md), em duas rodadas por modelo. Não foi uma execução pelo aplicativo Postman ou Newman. O [registro de equivalência](../documentacao/Validacao-Equivalencia-Modelos.md) descreve o escopo e os resultados.
