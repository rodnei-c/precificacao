# Modelagem de domínio

Projeto para comparar duas implementações funcionalmente equivalentes de um sistema de precificação e faturamento. A pesquisa tem a manutenibilidade como foco principal.

## Estrutura

```text
modelagem-dominio/
├── pom.xml
├── precificacao-anemica/
│   ├── pom.xml
│   └── src/
├── modelo-rico/
│   ├── pom.xml
│   └── src/
└── documentacao/
```

O projeto Maven raiz é pai e agregador dos dois módulos. Ele centraliza a configuração de compilação e as versões dos plugins. Cada módulo gera seu próprio JAR executável e não depende do outro.

As regras de negócio serão implementadas separadamente. Requisitos, dados de exemplo e casos de verificação podem ser comuns, mas não existe um módulo compartilhado de domínio.

## Estado atual

O módulo `precificacao-anemica` contém a estrutura de controllers, services, repositories, models, enums, dtos e exceptions. Os modelos de dados e as interfaces de repositório estão criados; controllers e serviços possuem suas dependências preparadas, mas ainda não executam operações de negócio. Os DTOs permanecem reservados até a definição dos contratos da API.

O módulo `modelo-rico` mantém sua classe de entrada inicial, sem alterações de implementação. Os dois pontos de entrada imprimem uma mensagem e encerram. Ainda não há API HTTP ou conexão ao PostgreSQL.

Framework, mecanismo de persistência e bibliotecas de teste não foram escolhidos nem adicionados. Os diretórios de recursos e testes estão preparados, sem impor uma ferramenta de testes.

O compilador permanece configurado provisoriamente com `release` 17. O ambiente local utiliza JDK 24.0.2 e Maven 3.9.9. A configuração de compilação poderá ser revista junto do framework e do ambiente de execução.

## Compilar

É necessário ter um JDK 17 ou superior e Maven disponível.

Na raiz do projeto:

```powershell
mvn package
```

Para compilar e empacotar um módulo de cada vez:

```powershell
mvn -pl precificacao-anemica package
mvn -pl modelo-rico package
```

## Executar a estrutura inicial

Após o empacotamento, cada aplicação pode ser executada separadamente:

```powershell
java -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
java -jar modelo-rico/target/modelo-rico-1.0.0-SNAPSHOT.jar
```

Esses comandos apenas verificam os pontos de entrada. Eles não iniciam um servidor HTTP nesta etapa.

## Abrir na IDE

Importe o `pom.xml` da raiz como projeto Maven. A IDE deve reconhecer `precificacao-anemica` e `modelo-rico` como módulos do mesmo projeto.

## Continuidade

As funcionalidades previstas são cadastro e consulta de produtos, atualização de precificação e criação e consulta de faturas. O detalhamento dos casos de uso e dos diagramas permanece no planejamento do trabalho.

Antes de implementar a API e a persistência, é necessário escolher o framework, o acesso ao PostgreSQL e os contratos das operações. Essas escolhas devem atender às mesmas regras de negócio nas duas versões.
