# Implementação da taxa por forma de pagamento

A [atividade](Atividade-Taxa-por-Forma-de-Pagamento.md) está implementada nos dois modelos, na branch `atividade/taxa-pagamento-nicolas-felipe`.

## Comportamento

Somente `POST /api/faturas/simulacao` retorna `taxaPagamento`, sempre como número, e inclui a taxa no `total`:

| Pagamento | Taxa sobre o saldo após desconto |
| --- | --- |
| Pix, dinheiro, débito, omitido ou `null` | Zero |
| Boleto | R$ 5,00 por simulação |
| Cartão de crédito | 2% do saldo exato |
| Qualquer pagamento com saldo zero | Zero |

A taxa mantém todas as casas decimais do cálculo. Apenas o total final é arredondado para duas casas com `HALF_EVEN`. Por exemplo, saldo `0.505` no cartão produz taxa `0.0101` e total `0.52`.

Cadastro e consulta mantêm o total sem taxa; cadastro, consulta e listagem não expõem o novo campo. A simulação continua validando itens, desconto, cliente e pagamento, consultando preços atuais e sem gravar faturas, itens ou consumir códigos. Enviar `taxaPagamento` manualmente é rejeitado com HTTP 400. Nenhuma migração foi necessária.

## Organização

No anêmico, `FaturaServiceImpl.simular` reaproveita a preparação dos itens e do desconto, calcula a taxa sobre o saldo exato e compõe `ValoresFaturaResposta`. O cálculo compartilhado de cadastro e consulta permanece sem taxa.

No rico, `TaxaPagamento` define o comportamento de cobrança. `SemTaxaPagamento`, `TaxaFixa` e `TaxaPercentual` implementam estratégias imutáveis. `FormaPagamento` delega à estratégia correspondente e `VinculosFatura` representa também a ausência de pagamento. `ValoresFatura.comTaxa` compõe um novo resultado imutável, preservando os valores originais e sem consultar novamente os produtos. `PgFaturas.simular` faz essa composição somente na simulação; `RespostasFatura` usa uma resposta específica para acrescentar o campo ao JSON. As fórmulas permanecem no domínio.

## Verificação realizada em 18/11/2026

- Empacotamento dos dois módulos e 476 testes sem PostgreSQL: aprovados.
- Suíte completa: 183 testes do anêmico e 513 do rico, totalizando **696 testes aprovados**, sem falhas, erros ou casos ignorados.
- Comparação HTTP em bancos exclusivos: coleção rica com 93 requisições executada contra ambas as APIs em duas rodadas, totalizando **372 requisições, 628 asserções e 186 pares de respostas equivalentes**.
- Os testes verificam taxa numérica inclusive zero, todas as formas de pagamento, pagamento omitido/nulo, descontos integrais, precisão antes do arredondamento, saldo positivo menor que um centavo, ausência de gravação/consumo de sequência, rejeição de entradas e preservação do cadastro e da consulta.

As coleções Postman de ambos os módulos receberam uma pasta com 18 requisições para a atividade. O anêmico tem 77 requisições e o rico, 93. A comparação foi executada com o [executor HTTP local](../verificacao/README.md), que suporta as asserções usadas pelas coleções.

Para repetir os testes:

```bash
mvn -s configuracao-maven.xml package
./scripts/dev.sh testar
```

Para repetir a comparação, siga as [instruções de preparação de bancos exclusivos e execução HTTP](../verificacao/README.md). As [evidências resumidas](evidencias/taxa-pagamento-2026-11-18.json) registram o ambiente e os resultados desta execução.
