# Documentação do estudo

Este diretório reúne as coleções Postman, as evidências de execução e o enunciado da atividade de manutenção dos dois modelos.

## Conteúdo disponível

| Arquivo | Conteúdo |
| --- | --- |
| [Coleção do anêmico](postman/precificacao-anemica.postman_collection.json) | 59 requisições em cinco pastas; `baseUrl` padrão `http://localhost:8080`. |
| [Coleção do rico](postman/precificacao-rica.postman_collection.json) | 75 requisições em seis pastas; `baseUrl` padrão `http://localhost:8081`. |
| [Resultados de testes e comparação](evidencias/etapa21-2026-11-10.json) | Ambiente, resultados JUnit e comparação HTTP. |
| [Resultados de testes do rico e comparação HTTP](evidencias/modelagem-etapa34-2026-11-10.json) | Resultados JUnit do rico e comparação de 150 pares de respostas HTTP. |
| [Atividade de manutenção](Atividade-Taxa-por-Forma-de-Pagamento.md) | Enunciado da taxa por forma de pagamento na simulação de faturas. |

As coleções preparam seus dados e guardam os códigos gerados em variáveis. Execute cada coleção desde a primeira requisição e confira se um ambiente do Postman está sobrescrevendo `baseUrl`.

## APIs e testes

Ambos os módulos possuem produtos, precificação, histórico, categorias, cidades, clientes e faturas. As instruções de configuração, os endpoints e os testes ficam nos READMEs do [anêmico](../precificacao-anemica/README.md) e do [rico](../precificacao-rica/README.md).

As evidências JSON são registros datados de execuções anteriores. As contagens de testes não representam uma quantidade fixa nem uma medição de manutenibilidade. Para repetir a comparação e conhecer suas limitações, consulte o [executor HTTP](../verificacao/README.md).

## Cenário de manutenção

A atividade de [taxa por forma de pagamento na simulação de faturas](Atividade-Taxa-por-Forma-de-Pagamento.md) contém regras, fluxo, pontos de alteração e critérios de validação para os dois modelos. Ela envolve somente a simulação, com três tipos de cobrança, e deve ser realizada em uma branch própria criada a partir da `anemico`. A taxa ainda não foi implementada no código da aplicação.

Os registros do estudo devem distinguir decisões de projeto, código implementado e resultados efetivamente observados.
