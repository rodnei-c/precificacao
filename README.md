# Modelagem de domínio

Projeto para comparar duas implementações funcionalmente equivalentes de um sistema de precificação e faturamento. A pesquisa tem a manutenibilidade como foco principal.

## Estrutura

~~~text
precificacao/
├── pom.xml
├── configuracao-maven.xml
├── compose.yml
├── precificacao-anemica/
│   ├── pom.xml
│   └── src/
├── precificacao-rica/
│   ├── pom.xml
│   └── src/
└── documentacao/
~~~

O projeto Maven raiz é pai e agregador dos dois módulos. Cada módulo gera seu próprio JAR e não depende do outro. As regras de negócio são implementadas separadamente; requisitos e casos de verificação podem ser comuns, mas não existe módulo compartilhado de domínio.

## Estado atual

O módulo precificacao-anemica expõe cadastro, listagem com filtros, consulta, alteração de precificação, histórico de produtos e gestão de categorias, cidades e clientes por HTTP, com Spring Boot 4.1.2, JDBC, PostgreSQL e migração Flyway. Permite renomear categorias, filtrar cidades por nome e UF e consultar e atualizar os contatos e o endereço de um cliente. Os controllers tratam HTTP, os services concentram cálculos e validações, e os repositories executam o acesso ao banco. Os modelos permanecem sem regras de negócio.

O módulo precificacao-rica usa Takes e oferece as operações de produtos, categorias, cidades e clientes na porta 8081, com o banco separado `precificacao_rica`. A coleção de produtos consulta os dados em uma leitura SQL e devolve um decorador imutável. Na inicialização, a API cria o banco ausente quando habilitado e aplica as migrações antes de abrir a porta HTTP. A configuração, a execução e os testes estão descritos [nas instruções do módulo](precificacao-rica/README.md).

O rico também cadastra faturas por `POST /api/faturas`, consulta por `GET /api/faturas/{codigo}`, simula por `POST /api/faturas/simulacao` e lista por `GET /api/faturas`, com filtros por período e cliente. Itens são obrigatórios; desconto, cliente e forma de pagamento são opcionais. Cabeçalho e itens são gravados na mesma transação. A consulta usa os preços atuais e preserva o desconto original. A simulação reutiliza as validações e os cálculos do cadastro sem gravar dados ou gerar código. O cálculo mantém a precisão até o total final; os itens gravam a referência ao produto e a quantidade. A listagem retorna resumos sem carregar preços ou recalcular faturas.

O módulo anêmico cria e simula faturas, lista faturas por período e consulta faturas pelo código. A consulta recalcula os valores com os preços atuais, usando os itens e o desconto original gravados em PostgreSQL.

A implementação anêmica executa os testes unitários por padrão. A validação completa usa `mvn -s configuracao-maven.xml -pl precificacao-anemica -Pintegracao test`, exige as variáveis de PostgreSQL e reprova testes ignorados ou classes de integração ausentes. Consulte [as instruções da API](precificacao-anemica/README.md#testes) para configurar o banco, iniciar o servidor e importar os exemplos no Postman. As etapas implementadas e os cenários executados estão no [registro de validação do plano](documentacao/Validacao-Plano-Anemico.md).

O rico também executa somente testes sem banco por padrão. Para a suíte completa, configure `RICO_BANCO_TESTE_URL`, `RICO_BANCO_CRIACAO_TESTE_URL`, `RICO_BANCO_TESTE_USUARIO` e `RICO_BANCO_TESTE_SENHA` e execute `mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test`. O perfil aplica as migrações no banco de testes indicado e exige todos os casos obrigatórios, sem ignorados. Os testes de criação precisam de um servidor exclusivo com usuário administrativo. A configuração está nas [instruções de testes do rico](precificacao-rica/README.md#testes), e o andamento está no [plano do rico](documentacao/Documentacao-e-Plano-Modelo-Rico.md).

## Compilar

É necessário JDK 17 ou superior e Maven 3.6.3 ou superior.

Na raiz:

~~~powershell
mvn -s configuracao-maven.xml package
~~~

Para compilar um módulo de cada vez:

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-anemica package
mvn -s configuracao-maven.xml -pl precificacao-rica package
~~~

A configuração de Maven incluída permite usar o Maven Central neste projeto sem modificar a configuração corporativa da máquina.

## Executar

Configure JAVA_HOME para um JDK compatível, mantenha o servidor PostgreSQL em execução e defina DB_URL, DB_USER e DB_PASSWORD conforme as instruções da API. O módulo anêmico cria o banco configurado caso ele não exista e, em seguida, aplica as migrações. A primeira criação exige um usuário com permissão CREATEDB. Depois:

~~~powershell
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
~~~

A API atende em http://localhost:8080.

## Abrir na IDE

Importe o pom.xml da raiz como projeto Maven. A IDE deve reconhecer os dois módulos. Se a IDE utilizar o Nexus corporativo, selecione configuracao-maven.xml como configuração do Maven deste projeto.

## Continuidade

As 21 etapas do plano do rico foram concluídas. A [validação de equivalência](documentacao/Validacao-Equivalencia-Modelos.md) registra os fluxos comparados, os testes e as diferenças de mensagens de erro. A [coleção Postman do rico](documentacao/postman/precificacao-rica.postman_collection.json) contém 75 requisições com preparação dos dados e códigos gerados. O [executor local](verificacao/README.md) permite repetir a comparação por HTTP.

A proposta de desconto percentual somente a partir de R$ 200,00 foi descartada pelo usuário. A revisão de modelagem começou pelos objetos de valor do rico; os próximos ajustes dependem de aprovação entre as etapas, conforme o [registro do rico](documentacao/Documentacao-e-Plano-Modelo-Rico.md#12-revisão-de-modelagem--uma-etapa-por-vez).

Os casos de uso, diagramas e registros de comparação ficam em documentacao.
