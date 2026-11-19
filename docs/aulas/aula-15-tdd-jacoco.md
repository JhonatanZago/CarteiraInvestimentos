# Aula 15 - TDD, testes e JaCoCo

## Objetivo

Demonstrar testes orientados a comportamento, ciclos TDD verificaveis e medicao
de cobertura no backend da Carteira de Investimentos.

## Ferramentas

- Java 21.
- Gradle Wrapper 9.5.1.
- JUnit 5.
- Mockito.
- MockMvc e testes Spring Boot.
- JaCoCo 0.8.14.

## Organizacao dos testes

Os testes unitarios isolam servicos e regras com Mockito. Os testes de
integracao usam Spring Boot, H2 e MockMvc para validar HTTP, autenticacao,
autorizacao e ownership. Adapters externos usam servidores HTTP simulados; nao
ha dependencia de chamadas reais para a suite automatizada.

## Ciclo TDD 1 - MovimentacaoFinanceiraService

### Defeito

Uma taxa cambial negativa informada explicitamente era aceita e podia levar a
consulta ao provedor e persistencia indevida da movimentacao.

### Evidencia

- Teste: `rejectsExplicitlyInvalidExchangeRateInsteadOfSilentlyReplacingIt`.
- RED: o teste falhou porque a implementacao antiga nao rejeitava a taxa antes
  de continuar o fluxo.
- GREEN: foi adicionada a validacao minima em
  `MovimentacaoFinanceiraService`.
- REFACTOR: a regra foi extraida para `validarCambioInformado`, preservando o
  contrato externo.
- Arquivos: `MovimentacaoFinanceiraService.java` e
  `MovimentacaoFinanceiraServiceTddTest.java`.

Este e um ciclo RED -> GREEN -> REFACTOR comprovado pela documentacao existente.

## Ciclo TDD 2 - ExchangeRateService

### Defeito

A validacao de cotacao futura usava o relogio global do sistema, ignorando o
`Clock` injetado no servico. Um teste deterministico passou a falhar conforme a
data real avancou.

### Evidencia

- Teste: `rejectsUnavailableAndFutureProviderDataWithoutInventingRate`.
- RED: a cotacao fixa foi considerada futura pelo uso de
  `OffsetDateTime.now()` em vez do relogio controlado.
- GREEN: a validacao passou a comparar o instante efetivo com o `Clock` do
  servico.
- REFACTOR: nao ha refatoracao adicional de comportamento comprovada; a
  alteracao minima foi mantida.
- Arquivos: `ExchangeRateService.java` e `ExchangeRateServiceTest.java`.

Este e o segundo ciclo RED -> GREEN comprovado. Nao ha evidencia para declarar
um terceiro ciclo RED completo.

## Green-first, regressao e integracao

- Green-first: o teste e escrito para um comportamento ja correto; ele confirma
  uma regra, mas nao possui evidencia de uma falha anterior.
- TDD RED -> GREEN -> REFACTOR: exige falha real, correcao e preservacao do
  comportamento apos a reorganizacao.
- Regressao: protege comportamento anteriormente aprovado, como JWT, RBAC,
  ownership e calculos financeiros.
- Teste unitario: executa uma unidade isolada, normalmente com Mockito.
- Teste de integracao: executa varias camadas reais, como controller,
  seguranca, persistencia H2 e MockMvc.

Exemplos Green-first incluem regras do `UsuarioAdministracaoService`, limite de
32 caracteres do segredo JWT e cenarios dos adapters de mercado. Eles nao sao
contados como ciclos RED artificiais.

## JaCoCo

O `build.gradle` configura JaCoCo 0.8.14 com relatorios HTML, XML e CSV.

```text
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Relatorios:

- HTML: `build/reports/jacoco/test/html/index.html`.
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`.
- CSV: `build/reports/jacoco/test/jacocoTestReport.csv`.

O portao minimo configurado verifica 80% de linhas e 55% de ramos no bundle.
Esses limites sao obrigatorios para a verificacao Gradle. A meta adicional de
80% de ramos e academica/aspiracional e nao foi configurada como limite para
nao impedir a geracao do relatorio enquanto a evolucao continua.

## Resultado atual

- 241 testes.
- 0 falhas.
- 0 erros.
- 1 ignorado.
- Linhas: 89,56% (1.441/1.609).
- Ramos: 70,98% (746/1.051).
- Instrucoes: 87,75%.
- Metodos: 86,08%.
- Classes: 97,24%.
- `jacocoTestCoverageVerification`: PASS.

Cobertura de linhas indica quanto das linhas executaveis foi visitado. Cobertura
de ramos mede caminhos de decisoes, como lados verdadeiro e falso de condicoes.
Uma cobertura alta nao prova ausencia de defeitos: testes podem repetir o mesmo
caminho, omitir integracoes externas ou nao verificar resultados relevantes.

## Teste ignorado

`PostgreSqlSmokeTest.startsWithPostgreSqlAndCreatesTheSchemaConstraints()` e
condicional a `smoke.db.url`. Ele exige PostgreSQL externo e as propriedades
`smoke.db.url`, `smoke.db.username`, `smoke.db.password` e `smoke.db.driver`.
Para executar em ambiente controlado:

```text
./gradlew test -Dsmoke.db.url=jdbc:postgresql://host:5432/carteira \
  -Dsmoke.db.username=<usuario> -Dsmoke.db.password=<senha> \
  -Dsmoke.db.driver=org.postgresql.Driver
```

Credenciais devem ser fornecidas fora do repositorio. O teste nao foi ativado
nem modificado nesta etapa.

## Limitacoes e conclusao

Os dois ciclos RED legitimos estao documentados. Os demais testes sao
Green-first ou regressao. A suite e o portao minimo JaCoCo estao aprovados,
mas a meta academica de 80% de ramos ainda nao foi atingida: faltam 95 ramos
para 841/1.051. A apresentacao deve distinguir claramente o limite minimo
automatizado da meta adicional.
