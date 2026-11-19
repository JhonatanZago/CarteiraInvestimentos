# Evidências TDD — Aula 15

## Ambiente e baseline

- Java 21.0.12.1 (Eclipse Adoptium).
- Gradle Wrapper 9.5.1.
- JUnit 5 e Mockito já fornecidos pelas dependências de teste do Spring Boot.
- JaCoCo 0.8.14.
- Cache: `.gradle-user-home-validation`.

Baseline anterior:

- Linhas: 72,69%.
- Ramos: 48,90%.
- Testes: 174, com 0 falhas e 1 ignorado.

## Ciclo 1 — Movimentação financeira

Comportamento: rejeitar taxa cambial negativa informada explicitamente, sem
substituí-la silenciosamente pela cotação do provedor.

### RED

Teste: `rejectsExplicitlyInvalidExchangeRateInsteadOfSilentlyReplacingIt`.

Comando:

```powershell
.\gradlew.bat --offline --no-daemon test --tests "com.example.carteirainvestimento.service.MovimentacaoFinanceiraServiceTddTest.rejectsExplicitlyInvalidExchangeRateInsteadOfSilentlyReplacingIt" --console=plain
```

Resultado: **FAIL**. A implementação antiga chamava o provedor e persistia a
operação, em vez de rejeitar a taxa negativa.

### GREEN

Foi adicionada a validação mínima em `MovimentacaoFinanceiraService`: uma taxa
informada e não positiva gera `BusinessRuleException`; taxa ausente continua
permitindo consulta ao provedor.

Resultado: **PASS**.

### REFACTOR

A regra foi extraída para `validarCambioInformado`, sem alterar o contrato
externo. O teste relacionado continuou **PASS**.

Testes adicionais do mesmo comportamento cobrem venda acima do saldo, taxa do
provedor disponível, provedor indisponível e preservação de `BigDecimal`.

## Ciclo 2 — Administração de usuários

Comportamentos selecionados:

- rebaixamento do último ADMIN ativo;
- desativação do último ADMIN ativo;
- e-mail duplicado;
- senha divergente antes da persistência.

Testes criados em `UsuarioAdministracaoServiceTddTest`:

- `rejectsDemotionOfTheLastActiveAdmin`;
- `rejectsDeactivationOfTheLastActiveAdmin`;
- `rejectsDuplicateEmailWhenUpdatingUser`;
- `rejectsMismatchedPasswordBeforePersisting`.

Resultado: **PASS** desde a primeira execução contra a implementação existente.
Não foi aplicada alteração de produção, pois não havia defeito comprovado.
O refactor limitou-se à organização dos fixtures Mockito e à verificação de
que nenhum `save` ocorre em falhas de regra.

Essa evidência é registrada como validação Green-first, não como um Red
artificial.

## Ciclo 3 — JWT

Comportamento selecionado: aceitar exatamente 32 caracteres como tamanho
mínimo válido do segredo, rejeitando 31 caracteres.

Teste criado:

- `acceptsExactly32CharacterSecretOutsideTestProfile`.

Resultado: **PASS** contra a implementação correta. A validação de segredo,
emissor, assinatura HS256, expiração e tokens malformados já estava coberta por
testes anteriores; não houve correção de produção necessária.

## Mutação temporária

Foi alterada temporariamente a condição do limite de segredo de `< 32` para
`<= 32`. O teste de fronteira falhou com `IllegalStateException`, demonstrando
que ele detecta a mutação.

A condição original `< 32` foi restaurada imediatamente e o mesmo teste voltou
a passar. Nenhuma mutação permaneceu no código final.

## Cobertura após os testes

Comando:

```powershell
.\gradlew.bat --offline --no-daemon test jacocoTestReport --console=plain
.\gradlew.bat --offline --no-daemon jacocoTestCoverageVerification --console=plain
```

Resultado final da suíte:

- 183 testes executados.
- 0 falhas.
- 1 ignorado.
- `test`: PASS.
- `jacocoTestReport`: PASS.
- `jacocoTestCoverageVerification`: FAIL pelos limites configurados.

| Métrica | Antes | Depois | Meta inicial |
|---|---:|---:|---:|
| Instruções | 71,49% | 73,95% | — |
| Linhas | 72,69% | 75,76% | 80% |
| Métodos | 77,49% | 78,73% | — |
| Classes | 92,41% | 93,10% | — |
| Ramos | 48,90% | 52,24% | 55% |

Relatório HTML:

`build/reports/jacoco/test/html/index.html`

O portão não foi reduzido: faltam 4,24 pontos percentuais em linhas e 2,76
em ramos para a meta inicial. A meta acadêmica final de 80% de ramos ainda
exige evolução adicional.

## Próximos comportamentos TDD

1. `MovimentacaoFinanceiraService`: resultado realizado em venda parcial,
   venda total e posição sem saldo.
2. `UsuarioAdministracaoService`: alteração de papel com dois ADMIN ativos,
   reativação e revogação de refresh tokens.
3. `AuthTokenService`: token sem `iss`, `iat`/`exp` inválidos e combinação de
   authorities não autorizada.

Nenhum frontend, migration, JWT fora dos testes de fronteira ou change
multimoeda foi alterado.

## Atualização — Comando 67

Para atingir os limites mínimos sem alterar regras financeiras, foram adicionados
testes de comportamento para caminhos ainda não exercitados:

- `AcaoRegistrationServiceTest.revalidatesAllAssetsAndReportsIndividualFailures`, cobrindo o processamento em lote com sucesso e falha individual.
- `MarketInsightEvolutionTest.returnsAvailableZeroWhenPortfolioHasNoPositions`, cobrindo carteira sem posições.
- `MarketInsightEvolutionTest.rejectsEvolutionForUnknownPortfolio`, cobrindo carteira inexistente.
- `MarketInsightEvolutionTest.marksDividendsUnavailableWhenProviderFails`, cobrindo indisponibilidade explícita do provedor.

Também foram corrigidas duas expectativas de teste para refletirem o contrato já
implementado: ausência de próximos proventos é `null` quando não há posições, e
falha do provedor produz `UNAVAILABLE`.

O teste focalizado de `AcaoRegistrationServiceTest` passou após a correção da
asserção. A suíte completa foi executada com o cache `.gradle-user-home-validation`:

- 197 testes.
- 0 falhas.
- 0 erros.
- 1 ignorado.
- `test jacocoTestReport`: PASS.
- `jacocoTestCoverageVerification`: PASS.

| Métrica | Baseline do Comando 66 | Resultado do Comando 67 | Meta mínima |
|---|---:|---:|---:|
| Instruções | 73,95% | 78,75% | — |
| Linhas | 75,76% | 80,36% | 80% |
| Métodos | 78,73% | 81,51% | — |
| Classes | 93,10% | 96,55% | — |
| Ramos | 52,24% | 56,42% | 55% |

Os dois ciclos adicionais RED reais solicitados não foram inventados: a análise
dos caminhos prioritários encontrou comportamentos já corretos. Assim, permanece
comprovado um ciclo RED -> GREEN -> REFACTOR (taxa cambial negativa); os demais
testes são evidências Green-first legítimas. A demonstração de mutação anterior
foi preservada.

O relatório HTML permanece em:

`build/reports/jacoco/test/html/index.html`

A meta acadêmica de 80% de ramos ainda não foi atingida; faltam 23,58 pontos
percentuais de cobertura de ramos.

## Atualização — Comando 70

Foram adicionados testes Green-first nos serviços financeiros, sem alterar
produção:

- `VendaServiceTest`: venda total com exclusão da posição, lucro, prejuízo,
  resultado zero, validações de data/preço/taxas e carteira/posição inexistente.
- `AtivoCarteiraServiceTest`: atualização e exclusão de posição própria,
  bloqueio de carteira/posição de terceiro e filtros/ordenações de listagem.
- `MovimentacaoFinanceiraServiceTddTest`: saldo e resultado realizado em venda
  parcial, venda antes de compra e custos ausentes.

A execução focada passou. A suíte completa passou com 218 testes, 0 falhas,
0 erros e 1 ignorado. O portão `jacocoTestCoverageVerification` passou sem
redução de limites.

| Métrica | Comando 69 | Comando 70 |
|---|---:|---:|
| Ramos | 63,27% (665/1.051) | 65,94% (693/1.051) |
| Linhas | 86,51% | 87,63% |
| Instruções | 84,65% | 85,81% |
| Métodos | 85,29% | 85,88% |
| Classes | 97,24% | 97,24% |

Ramos adicionais cobertos: **28**. Para 80% ainda são necessários 148 ramos
adicionais, chegando a pelo menos 841 ramos cobertos.

Nenhum defeito de produção foi comprovado e nenhum novo ciclo RED -> GREEN ->
REFACTOR foi inventado. Os testes desta etapa são Green-first; o ciclo RED
legítimo e a mutação temporária anteriores permanecem registrados.

## Atualização — Comando 69

Foram adicionados 12 testes de comportamento, todos com HTTP simulado por
`MockRestServiceServer`, sem chamadas reais aos provedores:

- `BrapiIncomeAdapterTest`: lista vazia, eventos legados, símbolo ausente,
  resposta v2 e fallback após falha do provedor.
- `YahooFinanceIncomeAdapterTest`: eventos positivos válidos e descarte de
  valores não positivos, timestamps inválidos e respostas sem dividendos.
- `BrapiCotacaoAdapterTest`: resultados vazios e moeda não suportada.
- `BrapiMarketIndicatorAdapterTest`: indicadores disponíveis, payloads
  incompletos, timestamp malformado e resposta vazia.
- `TwelveDataCotacaoAdapterTest`: chave ausente, 401, cotação inválida e logo
  fora de HTTPS.

A validação focada passou com 21 testes. A suíte completa passou com 209 testes,
0 falhas, 0 erros e 1 ignorado. O teste de cobertura também passou.

| Métrica | Comando 67 | Comando 69 |
|---|---:|---:|
| Ramos | 56,42% (593/1.051) | 63,27% (665/1.051) |
| Linhas | 80,36% | 86,51% |
| Instruções | 78,75% | 84,65% |
| Métodos | 81,51% | 85,29% |
| Classes | 96,55% | 97,24% |

Ramos adicionais cobertos: **72**. A diferença para a meta acadêmica de 80%
continua sendo de 176 ramos cobertos.

Não foi identificado defeito comprovável nos adapters durante esta etapa; os
testes foram Green-first. Nenhum novo ciclo RED -> GREEN -> REFACTOR foi
fabricado. O ciclo RED legítimo e a mutação temporária anteriores permanecem
registrados.

## Atualização — Comando 71

Foram adicionados 9 testes Green-first para dashboard, contexto monetário,
câmbio e snapshots:

- análise de exposição BRL/USD e contagens de lucro, prejuízo e resultado zero;
- dashboard vazio e conversão cambial indisponível;
- contextos monetários incompletos, reconversão na mesma moeda e taxa inválida;
- pares cambiais nulos/não suportados, fonte inválida e exceção do provedor;
- snapshot vazio e snapshot com moeda não resolvida.

A suíte completa passou com 227 testes, 0 falhas, 0 erros e 1 ignorado.
JaCoCo report e verificação passaram.

| Métrica | Comando 70 | Comando 71 |
|---|---:|---:|
| Ramos | 65,94% (693/1.051) | 68,03% (715/1.051) |
| Linhas | 87,63% | 88,38% |
| Instruções | 85,81% | 86,38% |
| Métodos | 85,88% | 85,88% |
| Classes | 97,24% | 97,24% |

Ramos adicionais cobertos: **22**. Faltam 126 ramos para atingir 841/1.051,
correspondente a 80%.

## Atualização — Comando 72

Foram adicionados 7 testes direcionados:

- `AcaoRegistrationService`: mercados alternativos indisponíveis, falha externa,
  ticker divergente, resposta nula e histórico de cotação duplicado.
- `DashboardCarteiraService`: custo histórico de compras/vendas e exclusão de
  posição encerrada.
- `MovimentacaoFinanceiraService`: matriz de entradas inválidas antes do acesso
  aos repositórios.

A suíte completa passou com 234 testes, 0 falhas, 0 erros e 1 ignorado.
`jacocoTestReport` e `jacocoTestCoverageVerification` passaram.

| Métrica | Comando 71 | Comando 72 |
|---|---:|---:|
| Ramos | 68,03% (715/1.051) | 70,12% (737/1.051) |
| Linhas | 88,38% | 89,12% |
| Instruções | 86,38% | 87,28% |
| Métodos | 85,88% | 85,88% |
| Classes | 97,24% | 97,24% |

Ramos adicionais cobertos: **22**. Faltam 104 ramos para atingir 841/1.051,
correspondente a 80%.

## Atualizacao — Comando 73

Foram adicionados 7 testes direcionados: revalidacao de ativos inexistentes e
falhas de provedor em `AcaoRegistrationService`, logo/fallback e contagem de
ativos sem logo, custo historico e posicao encerrada no dashboard, e entradas
invalidas rejeitadas antes do acesso a repositorios em movimentacoes financeiras.

Durante a execucao completa foi reproduzida uma falha real em
`ExchangeRateService`: a validacao de cotacoes futuras usava o relogio do
sistema em vez do `Clock` injetado. A correcao minima passou a comparar a data
da cotacao com o relogio controlado pelo servico, preservando a rejeicao de
dados futuros.

| Metrica | Comando 72 | Comando 73 |
|---|---:|---:|
| Testes | 234 | 237 |
| Ramos | 70,12% (737/1.051) | 70,41% (740/1.051) |
| Linhas | 89,12% | 89,12% |
| Instrucoes | 87,28% | 87,38% |
| Metodos | 85,88% | 86,08% |
| Classes | 97,24% | 97,24% |

Resultado: 0 falhas, 0 erros e 1 teste ignorado. `jacocoTestReport` e
`jacocoTestCoverageVerification` passaram. Faltam 101 ramos para atingir
841/1.051 (80%). O defeito temporal forneceu um ciclo RED -> GREEN legitimo;
os demais testes foram Green-first.

## Atualizacao - Comando 74

O ranking JaCoCo foi recalculado. Os maiores saldos de ramos nao cobertos foram
`BrapiCotacaoAdapter` (35), `AcaoRegistrationService` (26), `BrapiIncomeAdapter`
(16), `TwelveDataCotacaoAdapter` (14) e `DashboardCarteiraService` (11).

Foram adicionados 4 metodos de teste em `BrapiCotacaoAdapter` e
`TwelveDataCotacaoAdapter`, cobrindo fallback apos HTTP 5xx, payload legado
incompleto, erro de provedor, campos de topo, HTTP 500, JSON malformado e
ausencia de configuracao.

| Metrica | Comando 73 | Comando 74 |
|---|---:|---:|
| Testes | 237 | 241 |
| Ramos | 70,41% (740/1.051) | 70,98% (746/1.051) |
| Linhas | 89,12% | 89,56% |
| Instrucoes | 87,38% | 87,75% |
| Metodos | 86,08% | 86,08% |
| Classes | 97,24% | 97,24% |

Resultado: 0 falhas, 0 erros e 1 teste ignorado. `jacocoTestReport` e
`jacocoTestCoverageVerification` passaram. O ganho foi de 6 ramos; faltam 95
ramos para 841/1.051 (80%). Nenhum ciclo RED novo foi comprovado nesta etapa;
os testes foram Green-first.

Sobre a divergencia do Comando 73: o historico registra 7 cenarios, mas o
total passou de 234 para 237. Sem o XML daquela execucao nao e possivel
determinar se quatro cenarios ja estavam contados, se houve parametrizacao ou
se o relato agregou cenarios em vez de metodos. Nao foi encontrada evidencia de
remocao de testes no estado atual.

Nenhum defeito de produção foi comprovado e nenhum novo ciclo RED legítimo foi
declarado. Os testes foram Green-first; as evidências RED anteriores permanecem.

Não houve defeito de produção comprovado nem novo ciclo RED legítimo. Os testes
desta etapa são Green-first; as evidências RED anteriores permanecem intactas.
