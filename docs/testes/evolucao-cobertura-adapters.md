# Evolução da cobertura dos adapters de mercado

## Escopo

Foram exercitados `BrapiIncomeAdapter`, `BrapiCotacaoAdapter`,
`BrapiMarketIndicatorAdapter`, `TwelveDataCotacaoAdapter` e
`YahooFinanceIncomeAdapter` com `MockRestServiceServer`. Nenhum teste acessa
BRAPI, Yahoo Finance ou TwelveData pela internet e nenhum token real é usado.

## Cenários adicionados

- Proventos legados com símbolo presente e ausente.
- Proventos v2, lista vazia, taxas não positivas, data inválida e fallback após
  erro do provedor.
- Dividendos Yahoo válidos, negativos, zero, timestamp zero e ausência de
  resultados.
- Cotação BRAPI sem resultados, moeda não suportada e preservação de logos
  HTTPS já coberta pelos testes existentes.
- Indicadores BRAPI disponíveis, payload aninhado, payload incompleto, valor
  zero, timestamp inválido, resposta vazia e indisponibilidade.
- TwelveData sem chave, 401, cotação com moeda/preço inválidos e logo HTTP.

## Contadores JaCoCo

| Contador | Antes | Depois |
|---|---:|---:|
| Ramos cobertos | 593/1.051 (56,42%) | 665/1.051 (63,27%) |
| Ramos adicionais | — | 72 |
| Linhas | 80,36% | 86,51% |
| Instruções | 78,75% | 84,65% |
| Métodos | 81,51% | 85,29% |
| Classes | 96,55% | 97,24% |

Execução final:

```powershell
.\gradlew.bat --offline --no-daemon test jacocoTestReport --console=plain
.\gradlew.bat --offline --no-daemon jacocoTestCoverageVerification --console=plain
```

Resultado: **209 testes**, 0 falhas, 0 erros e 1 ignorado; relatório e
verificação JaCoCo passaram.

## TDD e limitações

Os testes novos foram Green-first: a implementação existente já atendia os
comportamentos observados. Não houve correção de produção nem novo ciclo RED
legítimo. O ciclo RED -> GREEN -> REFACTOR anterior permanece em
`docs/testes/tdd-aula15.md`.

## Próximas prioridades

Ainda há 386 ramos não cobertos. As próximas prioridades por impacto são:

1. `BrapiIncomeAdapter` — caminhos de parsing e respostas parciais restantes.
2. `BrapiCotacaoAdapter` — fallback legado, respostas HTTP 5xx e payloads
   incompletos, condicionados à compatibilidade do conversor JSON do harness.
3. `AcaoRegistrationService` — validações de cadastro e revalidação.
4. `DashboardCarteiraService` e `MovimentacaoFinanceiraService` — decisões
   financeiras e estados de câmbio sem alterar fórmulas.
5. `VendaService` e `AtivoCarteiraService` — fronteiras de venda, posição e
   ownership.

A meta acadêmica de 80% de ramos requer pelo menos 841 ramos cobertos; faltam
176 ramos cobertos para esse patamar.
