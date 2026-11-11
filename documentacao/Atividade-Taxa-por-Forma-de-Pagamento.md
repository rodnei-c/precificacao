# Atividade: taxa por forma de pagamento na simulação de faturas

## Contexto

O sistema permite cadastrar produtos, calcular descontos e emitir faturas. A forma de pagamento já pode ser informada, mas ainda não interfere no valor cobrado.

A empresa deseja mostrar ao usuário quanto uma fatura custaria conforme a forma de pagamento escolhida. Sua tarefa é acrescentar esse cálculo à simulação nas duas versões do projeto: o modelo anêmico e o modelo rico. As duas implementações devem produzir os mesmos resultados, respeitando a organização de cada modelo.

## Branch de trabalho

Use a branch `anemico` como ponto de partida. A partir dela, crie uma branch própria e faça as alterações dos dois modelos nessa nova branch.

Dentro da pasta do projeto, execute:

```bash
git switch anemico
git pull --ff-only
git switch -c atividade/taxa-pagamento-seu-nome
```

Substitua `seu-nome` pelo seu nome para identificar a branch. Faça os commits da atividade na branch criada.

## O que deve ser implementado

Implemente o cálculo somente nesta operação:

| Operação | Endpoint | Resposta de sucesso |
| --- | --- | --- |
| Simular uma fatura, sem gravá-la | `POST /api/faturas/simulacao` | `200` |

A resposta da simulação deve apresentar o novo campo numérico `taxaPagamento`, inclusive quando a taxa for zero. O campo `total` deve passar a incluir essa taxa. Os demais campos da resposta devem ser preservados.

O formato da requisição permanece igual: o usuário informa `formaPagamento` e o sistema calcula a taxa. Não deve ser possível informar manualmente o valor da taxa.

Cadastro, consulta e listagem de faturas ficam fora da atividade. Preserve o comportamento desses fluxos ao alterar componentes compartilhados. A atividade utiliza o endpoint e os dados existentes, sem exigir mudanças no banco.

## Regras da taxa

Primeiro, calcule os itens e aplique o desconto existente. O saldo restante será a base de cálculo da taxa.

| Forma de pagamento | Valor enviado na requisição | Taxa |
| --- | --- | --- |
| Sem taxa | `PIX`, `DINHEIRO`, `CARTAO_DEBITO`, campo omitido ou `null` | Zero |
| Boleto | `BOLETO` | R$ 5,00 por simulação |
| Cartão de crédito | `CARTAO_CREDITO` | 2% do saldo após o desconto |

Se o desconto consumir todo o subtotal, o saldo, a taxa e o total devem ser zero, inclusive para boleto.

O cálculo segue esta ordem:

```text
subtotal = soma dos subtotais dos itens
saldo = subtotal - desconto aplicado
taxa de pagamento = valor definido para a forma de pagamento, considerando o saldo
total = saldo + taxa de pagamento
```

Mantenha a precisão dos valores durante o cálculo. Arredonde somente o total final para duas casas decimais, seguindo o critério já utilizado pelo projeto (`HALF_EVEN`). O valor retornado em `taxaPagamento` deve conservar a precisão calculada; ele pode ter mais de duas casas decimais.

Preserve as regras atuais de itens, descontos e validação. A simulação deve continuar utilizando os preços atuais dos produtos, sem gravar dados ou consumir um código de fatura.

## Fluxo esperado

1. Receber os itens, o desconto e a forma de pagamento.
2. Validar a entrada e consultar os preços atuais dos produtos.
3. Calcular os subtotais dos itens e o subtotal da fatura.
4. Aplicar o desconto e encontrar o saldo restante.
5. Calcular a taxa de pagamento sobre esse saldo.
6. Somar a taxa ao saldo e arredondar o total final.
7. Retornar os valores da simulação, sem gravar a fatura.

## Exemplo para conferir no Postman

Prepare um produto com preço de venda de **R$ 100,00**. Utilize o código retornado pelo cadastro desse produto no lugar do `1` abaixo. Cada modelo possui seu próprio banco e pode gerar códigos diferentes.

Envie este corpo para `POST /api/faturas/simulacao`:

```json
{
  "itens": [
    {
      "codigoProduto": 1,
      "quantidade": 1
    }
  ],
  "desconto": {
    "tipo": "VALOR_FIXO",
    "valor": 10.00
  },
  "formaPagamento": "CARTAO_CREDITO"
}
```

O saldo após o desconto será R$ 90,00. A taxa de 2% será R$ 1,80, resultando em R$ 91,80. O trecho correspondente da resposta deve ser:

```json
{
  "subtotal": 100.00,
  "descontoAplicado": 10.00,
  "taxaPagamento": 1.80,
  "total": 91.80
}
```

Esse trecho mostra apenas os campos de valores. Mantenha os demais campos previstos na resposta da simulação. Troque `formaPagamento` por `PIX` e por `BOLETO` para conferir os outros dois tipos de cobrança.

As coleções existentes estão em [Postman do anêmico](postman/precificacao-anemica.postman_collection.json) e [Postman do rico](postman/precificacao-rica.postman_collection.json). As URLs padrão são `http://localhost:8080` para o anêmico e `http://localhost:8081` para o rico. Confira se o ambiente selecionado no Postman não está substituindo a variável `baseUrl` da coleção.

## Onde procurar no projeto

Os arquivos abaixo servem como pontos de partida. Você deve decidir quais precisam mudar e se faz sentido criar novas classes.

### Modelo anêmico

O fluxo parte do controller, passa pelo service e utiliza os repositories para acessar os dados. As regras de cálculo ficam no service.

| Arquivo | Papel no fluxo e ponto de atenção |
| --- | --- |
| [FaturaController.java](../precificacao-anemica/src/main/java/br/com/precificacao/anemico/controllers/FaturaController.java) | O método `simular` recebe a chamada HTTP e delega ao service. |
| [CadastroFaturaRequisicao.java](../precificacao-anemica/src/main/java/br/com/precificacao/anemico/dtos/CadastroFaturaRequisicao.java) | Define a entrada da fatura e já possui `formaPagamento`. |
| [FaturaServiceImpl.java](../precificacao-anemica/src/main/java/br/com/precificacao/anemico/services/impl/FaturaServiceImpl.java) | Comece por `simular`. Observe como `prepararFatura`, `calcularValores` e `calcularDesconto` participam do cálculo existente e são compartilhados com outros fluxos. |
| [ValoresFaturaResposta.java](../precificacao-anemica/src/main/java/br/com/precificacao/anemico/dtos/ValoresFaturaResposta.java) | Representa os valores retornados na simulação. Verifique a inclusão de `taxaPagamento`. |
| [FaturaServiceTest.java](../precificacao-anemica/src/test/java/br/com/precificacao/anemico/services/FaturaServiceTest.java) | Reúne testes das regras e dos fluxos de fatura no service. |

Mantenha o acesso ao banco nos repositories, as regras de negócio no service e o tratamento HTTP no controller. Reaproveite o cálculo de itens e desconto que já existe.

### Modelo rico

Os objetos HTTP recebem a requisição e montam a resposta. O cálculo pertence aos objetos do domínio de faturas. Observe os componentes compartilhados para aplicar a taxa no caminho da simulação.

| Arquivo | Papel no fluxo e ponto de atenção |
| --- | --- |
| [TkSimulacaoFatura.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/http/TkSimulacaoFatura.java) | É a entrada HTTP da simulação. |
| [PedidoFatura.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/http/PedidoFatura.java) | Transforma a entrada em objetos usados pelo domínio. Verifique como a forma de pagamento chega ao cálculo. |
| [CalculoFatura.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/CalculoFatura.java) e [ValoresFatura.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/ValoresFatura.java) | Calculam os itens, aplicam o desconto e produzem os valores da fatura. São os pontos de partida para integrar o comportamento da taxa. |
| [VinculosFatura.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/VinculosFatura.java) e [FormaPagamento.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/FormaPagamento.java) | Representam a forma de pagamento e os vínculos opcionais da fatura. |
| [PgFaturas.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/pg/PgFaturas.java) | O método `simular` prepara os produtos e os vínculos recebidos. Observe como esses dados chegam aos objetos de cálculo. |
| [RespostasFatura.java](../precificacao-rica/src/main/java/br/com/modelagemdominio/rico/faturas/http/RespostasFatura.java) | O método `simulacao` monta o JSON. Inclua `taxaPagamento` no mesmo nível dos demais valores, preservando os outros fluxos que usam esta classe. |
| [CalculoFaturaTest.java](../precificacao-rica/src/test/java/br/com/modelagemdominio/rico/faturas/CalculoFaturaTest.java) e [SimulacaoFaturaPostgresTest.java](../precificacao-rica/src/test/java/br/com/modelagemdominio/rico/SimulacaoFaturaPostgresTest.java) | Exemplos de testes do cálculo e da simulação com produtos persistidos. |

Siga as convenções adotadas no modelo rico: objetos de domínio imutáveis, com comportamento e até quatro atributos de instância; composição de responsabilidades; ausência de getters/setters no estilo JavaBean e de auxiliares estáticos para regras de negócio. Os construtores devem permitir criar objetos válidos.

Organize as formas de cobrança por comportamentos que possam ser compostos e reutilizados. Uma estratégia de cálculo pode representar uma regra de taxa. A escolha das classes e dos nomes faz parte da atividade. As fórmulas da taxa devem permanecer no domínio, preservando o papel dos adaptadores HTTP e de persistência.

## Critérios de validação

Confira os três casos principais nas duas versões:

| Subtotal | Desconto aplicado | Forma de pagamento | Taxa esperada | Total esperado |
| --- | --- | --- | --- | --- |
| R$ 100,00 | R$ 10,00 | Pix | R$ 0,00 | R$ 90,00 |
| R$ 100,00 | R$ 10,00 | Boleto | R$ 5,00 | R$ 95,00 |
| R$ 100,00 | R$ 10,00 | Cartão de crédito | R$ 1,80 | R$ 91,80 |

Confira também estes comportamentos:

- **Saldo zero:** com subtotal e desconto aplicado de R$ 100,00, a taxa e o total devem ser zero, inclusive no boleto.
- **Contrato:** `taxaPagamento` deve aparecer como número no JSON, inclusive quando zero. A validação de formas de pagamento inválidas e de campos não permitidos deve continuar funcionando.
- **Simulação:** a operação deve continuar sem gravar uma fatura.

Automatize os três casos principais do cálculo e o caso de saldo zero. Confira também o retorno HTTP da simulação para validar a presença da taxa e o total. Reaproveite os testes existentes e atualize as expectativas afetadas pela mudança.
