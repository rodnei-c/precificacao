# Modelagem de domínio

Projeto para comparar duas implementações dos mesmos casos de uso de precificação e faturamento. A pesquisa tem a manutenibilidade como foco principal.

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
├── documentacao/
└── verificacao/
~~~

O projeto Maven raiz é pai e agregador dos dois módulos. Cada módulo gera seu próprio JAR e não depende do outro. As regras de negócio são implementadas separadamente; requisitos e casos de verificação podem ser comuns, mas não existe módulo compartilhado de domínio.

## Estado atual

Os dois módulos expõem APIs HTTP com estas funcionalidades:

- Produtos: cadastro, consulta, listagem com filtros, alteração de precificação, vínculo com categoria e histórico de alterações.
- Categorias: cadastro, listagem e renomeação.
- Cidades: cadastro e listagem com filtros por nome e UF.
- Clientes: cadastro, listagem, consulta e atualização parcial dos contatos e do endereço.
- Faturas: cadastro, simulação sem gravação, consulta e listagem com filtros por período e cliente.

| Módulo | Organização | Porta padrão | Banco padrão |
| --- | --- | --- | --- |
| [Anêmico](precificacao-anemica/README.md) | Spring Boot 4.1.2 e JDBC; controllers tratam HTTP, services concentram as regras e repositories acessam o banco. | `8080` | `precificacao_anemica` |
| [Rico](precificacao-rica/README.md) | Takes e objetos com comportamento; os pacotes `http` e `pg` adaptam as entradas, as respostas e a persistência. | `8081` | `precificacao_rica` |

Ambos utilizam PostgreSQL, criam o banco ausente quando essa opção está habilitada e aplicam as migrações Flyway ao iniciar. O anêmico possui oito migrações e o rico, dez; os bancos e seus históricos são independentes.

As faturas exigem itens com quantidades positivas, inclusive fracionárias. Desconto, cliente e forma de pagamento são opcionais. O cadastro grava cabeçalho e itens na mesma transação. A consulta usa os preços atuais dos produtos e preserva o desconto original; a simulação calcula sem gravar dados ou gerar código. Somente o total final da fatura é arredondado.

A forma de pagamento é informativa no código atual: não acrescenta taxa ao total. A taxa na simulação é o tema da [atividade de manutenção](documentacao/Atividade-Taxa-por-Forma-de-Pagamento.md), ainda a implementar pelos participantes.

Existem diferenças pontuais entre os modelos. Por exemplo, no PATCH de cliente, telefone em branco é tratado como remoção no anêmico e rejeitado no rico; ambos aceitam `null` para remover o telefone. As respostas de erro também podem ter textos diferentes. A comparação automatizada verifica os cenários presentes na coleção, sem afirmar equivalência para toda entrada possível.

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

Os comandos abaixo partem da raiz do repositório. Configure `JAVA_HOME` para um JDK compatível e mantenha o servidor PostgreSQL em execução. A primeira criação de cada banco exige um usuário com permissão `CREATEDB`.

Para o anêmico, crie `precificacao-anemica/.env.anemico` com a senha do PostgreSQL:

~~~properties
DB_PASSWORD=sua-senha-local
~~~

Para o rico, crie `.env.rico` na raiz:

~~~properties
RICO_DB_USER=postgres
DB_PASSWORD=sua-senha-local
~~~

Esses arquivos locais são ignorados pelo Git. As variáveis de ambiente têm prioridade sobre os valores dos arquivos. Os READMEs dos módulos detalham as opções de conexão e a execução pelo IntelliJ.

Após compilar, inicie cada API em um terminal:

~~~powershell
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
~~~

~~~powershell
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-rica/target/precificacao-rica-1.0.0-SNAPSHOT.jar
~~~

Os endereços padrão são `http://localhost:8080` e `http://localhost:8081`, respectivamente. O [Compose](compose.yml) pode fornecer um PostgreSQL local; ele cria inicialmente o banco do anêmico, e o rico prepara seu próprio banco ao iniciar.

## Testes

Por padrão, os dois módulos executam somente testes sem PostgreSQL:

~~~powershell
mvn -s configuracao-maven.xml test
~~~

O perfil `integracao` executa a suíte completa, incluindo os casos sem banco. Configure antes as quatro variáveis de teste de cada módulo, conforme as [instruções do anêmico](precificacao-anemica/README.md#testes) e as [instruções do rico](precificacao-rica/README.md#testes). Os testes de criação exigem um servidor PostgreSQL exclusivo e um usuário administrativo.

~~~powershell
mvn -s configuracao-maven.xml -pl precificacao-anemica -Pintegracao test
mvn -s configuracao-maven.xml -pl precificacao-rica -Pintegracao test
~~~

O perfil verifica a presença dos casos obrigatórios e rejeita falhas, testes ignorados e opções que pulem parte da suíte. Consulte os relatórios gerados em cada execução para obter as contagens atuais.

## Abrir na IDE

Importe o pom.xml da raiz como projeto Maven. A IDE deve reconhecer os dois módulos. Se a IDE utilizar o Nexus corporativo, selecione configuracao-maven.xml como configuração do Maven deste projeto.

## Documentação e comparação

A [documentação do estudo](documentacao/README.md) reúne a atividade de manutenção, as coleções Postman e as evidências disponíveis. A coleção do anêmico contém 59 requisições; a do rico, 75. Os códigos dos cadastros são guardados nas variáveis das coleções. Confira se um ambiente selecionado no Postman está sobrescrevendo `baseUrl`.

O [executor local](verificacao/README.md) compara as respostas dos dois módulos por HTTP. As instruções detalham a preparação das APIs, a execução da coleção e a leitura dos resultados.
