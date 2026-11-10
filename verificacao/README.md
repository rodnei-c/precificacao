# Comparação local por HTTP

`equivalencia.js` executa a coleção rica contra os dois módulos e compara suas respostas. Usa Node.js 12 ou superior e somente módulos nativos, sem instalação por npm. Foi usado no [fechamento da etapa 21](../documentacao/Validacao-Equivalencia-Modelos.md).

## Preparar as APIs

Gere os JARs na raiz do projeto:

```powershell
mvn -s configuracao-maven.xml -pl precificacao-anemica,precificacao-rica package
```

Use um servidor PostgreSQL exclusivo para a comparação e dois bancos vazios diferentes. As APIs podem criar os bancos ausentes quando o usuário possui `CREATEDB`; ambas aplicam as próprias migrações. Não aponte estes exemplos para bancos com dados de desenvolvimento: a coleção cria e altera seus cadastros de exemplo.

Em um terminal PowerShell na raiz, configure e inicie o anêmico, ajustando a porta e as credenciais do servidor de testes:

```powershell
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/precificacao_anemica_comparacao"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "senha-do-servidor-de-testes"
$env:SERVER_PORT = "8080"
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-anemica/target/precificacao-anemica-1.0.0-SNAPSHOT.jar
```

Em outro terminal, inicie o rico:

```powershell
$env:RICO_DB_URL = "jdbc:postgresql://127.0.0.1:5432/precificacao_rica_comparacao"
$env:RICO_DB_USER = "postgres"
$env:DB_PASSWORD = "senha-do-servidor-de-testes"
$env:RICO_SERVER_PORT = "8081"
& "$env:JAVA_HOME\bin\java.exe" -jar precificacao-rica/target/precificacao-rica-1.0.0-SNAPSHOT.jar
```

`JAVA_HOME` precisa apontar para um JDK compatível. Se as portas estiverem ocupadas, escolha outras e ajuste o arquivo de configuração abaixo. Não é necessário igualar os códigos gerados. Na validação da etapa 21, os códigos do rico começaram em 1001 e os do anêmico em 1.

## Executar

Crie um arquivo JSON, por exemplo `target/comparacao-local.json`, com as APIs de teste:

```json
{
  "anemico": "http://127.0.0.1:8080",
  "rico": "http://127.0.0.1:8081",
  "rodadas": 2
}
```

Na raiz, execute em um terceiro terminal:

```powershell
node verificacao/equivalencia.js target/comparacao-local.json target/comparacao-resultado.json
```

São executadas pelo menos duas rodadas, com dados novos por rodada. O processo retorna código zero somente se as verificações e comparações passarem. O relatório JSON contém resultados por requisição, verificações, diferenças de erro e eventual falha. Não contém credenciais de banco.

Para repetir o comando desde o início, use novamente bancos vazios exclusivos. O executor associa as identidades aos cadastros da própria execução; registros anteriores nas listagens não têm essa associação. A execução direta da coleção no Postman permite repetir desde a primeira requisição, gerando novos dados, sem essa restrição do comparador.

## O que o executor verifica

As respostas de sucesso precisam ter os mesmos campos e valores, inclusive precisão decimal e ordenação das listas. A comparação associa códigos gerados à mesma operação de cadastro e desconsidera horários gerados independentemente, depois de conferir seu formato. Nos erros, exige os mesmos status e valida o tipo e os campos essenciais da resposta; diferenças textuais ficam registradas.

O executor usa HTTP real e interpreta os scripts da coleção com um subconjunto de `pm`: variáveis da coleção, substituição de variáveis e GUID, `pm.test`, `pm.expect` com as comparações usadas no arquivo e leitura/status da resposta. Não é um executor Postman genérico. Novas APIs `pm` na coleção exigem adaptação ou execução em uma ferramenta Postman compatível.

Para usar o aplicativo Postman, importe [a coleção rica](../documentacao/postman/precificacao-rica.postman_collection.json), confira `baseUrl` e execute as seis pastas na ordem, começando pela preparação. As variáveis de código pertencem à coleção. O comparador local não valida a interface de importação do Postman.
