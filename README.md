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

O módulo precificacao-anemica expõe cadastro e consulta de produtos por HTTP, com Spring Boot 4.1.2, JDBC, PostgreSQL e migração Flyway. Os controllers tratam HTTP, os services concentram cálculos e validações, e os repositories executam o acesso ao banco. Os modelos permanecem sem regras de negócio.

O módulo precificacao-rica usa Takes e disponibiliza `POST /api/produtos` e `GET /api/produtos/{codigo}` na porta 8081 e usa o banco separado `precificacao_rica`. A coleção consulta todos os dados em uma leitura SQL e devolve um decorador imutável do produto. A preparação do banco, a execução da API e os testes estão descritos [nas instruções do módulo](precificacao-rica/README.md).

O rico também possui a estrutura inicial de faturas: contratos de entidade e coleção, item com quantidade validada e objetos de desconto. O item consulta o preço atual do produto para calcular seu subtotal. A implementação da fatura completa, sua persistência e seus endpoints continuam pendentes.

Faturamento e atualização de produtos existentes permanecem pendentes na implementação anêmica.

A implementação anêmica possui testes de cálculo e HTTP, além de um teste opcional de integração com PostgreSQL. Consulte [as instruções da API](precificacao-anemica/README.md) para configurar o banco, iniciar o servidor e importar os exemplos no Postman.

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

Configure JAVA_HOME para um JDK compatível, prepare o PostgreSQL e defina DB_URL, DB_USER e DB_PASSWORD conforme as instruções da API. Depois:

~~~powershell
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
~~~

A API atende em http://localhost:8080.

## Abrir na IDE

Importe o pom.xml da raiz como projeto Maven. A IDE deve reconhecer os dois módulos. Se a IDE utilizar o Nexus corporativo, selecione configuracao-maven.xml como configuração do Maven deste projeto.

## Continuidade

Os próximos casos de uso são atualização da precificação e criação e consulta de faturas. O módulo com comportamento encapsulado deve atender às mesmas regras funcionais e condições de comparação, com implementação própria.

Os casos de uso, diagramas e registros de comparação ficam em documentacao.
