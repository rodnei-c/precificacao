# Precificação anêmica

Módulo organizado em controllers, services, repositories e models, com pacotes separados para enums, DTOs e exceções.

## Estrutura criada

```text
br/com/precificacao/anemico/
├── AplicacaoAnemica.java
├── controllers/
│   ├── ProdutoController.java
│   └── FaturaController.java
├── services/
│   ├── ProdutoService.java
│   ├── PrecificacaoService.java
│   └── FaturaService.java
├── repositories/
│   ├── ProdutoRepository.java
│   └── FaturaRepository.java
├── models/
│   ├── Produto.java
│   ├── Fatura.java
│   └── ItemFatura.java
├── enums/
│   ├── ModalidadePrecificacao.java
│   └── TipoDesconto.java
├── dtos/
│   └── package-info.java
└── exceptions/
    ├── ValidacaoException.java
    └── RegistroNaoEncontradoException.java
```

Os modelos mantêm os dados, construtores e getters/setters. Os valores monetários, percentuais e quantidades utilizam `BigDecimal`. Não há fórmulas ou regras de negócio nos modelos.

Os controllers recebem seus serviços pelo construtor. `ProdutoService` recebe o repositório de produtos e o serviço de precificação. `FaturaService` recebe os repositórios de faturas e produtos, preparando a consulta aos preços atuais.

Os repositórios são interfaces com operações de gravação, busca e listagem. Não possuem implementação de banco. Os itens são tratados dentro da fatura, sem controller ou repositório próprios.

## O que ainda não está implementado

Esta etapa cria a estrutura e as dependências entre as classes. Os serviços ainda não executam cadastro, consultas, cálculos ou validações de negócio. Os controllers não expõem endpoints HTTP.

O pacote `dtos` está reservado. Os contratos de entrada e saída, o framework HTTP e o mecanismo de persistência precisam ser definidos antes da integração. Nenhuma biblioteca adicional foi incluída.

O sistema ainda não realiza conexão ao PostgreSQL. Também não implementa o cenário de manutenção do valor mínimo para desconto percentual.

## Compilar e executar

Na raiz do projeto:

```powershell
mvn -pl precificacao-anemica package
java -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
```

A execução imprime o estado da estrutura e encerra; ela não inicia um servidor.

## Verificação estrutural

Depois da compilação, é possível executar uma verificação dos modelos, dependências e exceções, sem escolher um framework de testes:

```powershell
java -cp "precificacao-anemica/target/classes;precificacao-anemica/target/test-classes" br.com.precificacao.anemico.EstruturaAnemicaVerificacao
```

Essa verificação é executada separadamente. Ela não é um teste funcional das operações de precificação ou faturamento, que ainda não foram implementadas.
