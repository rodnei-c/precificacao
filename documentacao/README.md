# Documentação do estudo

Este diretório fica reservado aos casos de uso, diagramas e registros da comparação.

O registro de implementação e testes dos nove passos de clientes, cidades e categorias está em [Validacao-Plano-Anemico.md](Validacao-Plano-Anemico.md). A [coleção Postman do anêmico](postman/precificacao-anemica.postman_collection.json) inclui produtos, histórico, faturas e os novos contratos verificados.

O [estado atual e plano do modelo rico](Documentacao-e-Plano-Modelo-Rico.md) registra as 21 etapas concluídas em 10 de novembro de 2026. A [validação final](Validacao-Equivalencia-Modelos.md) reúne os resultados reais dos testes e da comparação por HTTP, incluindo as diferenças de mensagens de erro. A [coleção Postman do rico](postman/precificacao-rica.postman_collection.json) contém o fluxo completo com dados novos e códigos gerados. As instruções para repetir a comparação estão em [verificacao](../verificacao/README.md).

A proposta de desconto mínimo de R$ 200 foi descartada pelo usuário. As etapas 1 a 15 da revisão de modelagem estão concluídas: valores de precificação, dependências de `PgFaturas`, textos do cliente, operações de alteração de campo, telefone opcional, conferência dos dígitos do CPF, reconstituição do desconto por composição, construção do cabeçalho da fatura, leitura do vínculo opcional de categoria, conversão exata dos códigos de categoria e cidade, interpretação da sigla de UF, materialização da cidade e da categoria por composição e classificação das falhas de renomeação de categoria. A etapa 16 aguarda aprovação individual, conforme a seção 12 do plano.

## Planejamento atual

Os cinco casos de uso propostos são:

1. Cadastrar produto.
2. Consultar produtos.
3. Atualizar a precificação de um produto.
4. Criar fatura.
5. Consultar faturas.

As descrições completas ainda precisam ser elaboradas. O primeiro caso previsto para detalhamento é Cadastrar produto.

O conjunto inicial de diagramas proposto contém um diagrama de casos de uso comum às duas versões, dois diagramas de classes e dois de sequência. Os diagramas de sequência devem representar a mesma operação, ainda a confirmar.

## Cenário de manutenção

O cenário de manutenção anteriormente proposto, com subtotal mínimo para desconto percentual, foi descartado. Nenhuma regra substituta foi definida. Os registros do estudo devem distinguir decisões de projeto, código implementado e resultados efetivamente observados.
