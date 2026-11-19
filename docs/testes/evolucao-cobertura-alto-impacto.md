# Evolucao de cobertura de alto impacto

## Comando 74

### Ranking recalculado

| Classe | Ramos cobertos | Ramos nao cobertos | Total |
|---|---:|---:|---:|
| `BrapiCotacaoAdapter` | 37 | 35 | 72 |
| `AcaoRegistrationService` | 60 | 26 | 86 |
| `BrapiIncomeAdapter` | 36 | 16 | 52 |
| `TwelveDataCotacaoAdapter` | 26 | 14 | 40 |
| `DashboardCarteiraService` | 95 | 11 | 106 |
| `BrapiMarketIndicatorAdapter` | 23 | 5 | 28 |
| `YahooFinanceIncomeAdapter` | 13 | 5 | 18 |

### Testes adicionados

- `BrapiCotacaoAdapterTest`: fallback apos HTTP 500, payload legado
  incompleto, resposta com `error=true` e campos no nivel superior.
- `TwelveDataCotacaoAdapterTest`: HTTP 500, JSON malformado e propriedades
  ausentes sem chamada ao provedor.

Todos os HTTPs foram simulados com `MockRestServiceServer`; nenhuma chamada
externa ou credencial real foi utilizada.

### Resultado real

- 241 testes executados.
- 0 falhas.
- 0 erros.
- 1 ignorado.
- Instrucoes: 10.284/11.719 (87,75%).
- Linhas: 1.441/1.609 (89,56%).
- Ramos: 746/1.051 (70,98%).
- Metodos: 433/503 (86,08%).
- Classes: 141/145 (97,24%).
- `jacocoTestCoverageVerification`: PASS.

O ganho foi de 6 ramos, de 740 para 746. A meta de 80% exige 841 ramos
cobertos; permanecem 95.

### TDD

Nenhum defeito de producao foi demonstrado por estes testes. Nao foi registrado
novo ciclo RED -> GREEN -> REFACTOR. Os testes desta etapa sao Green-first.

### Contagem liquida do Comando 73

O relato anterior listou 7 cenarios/metodos, mas a contagem liquida observada
foi de apenas 3 testes (234 para 237). O XML anterior nao esta disponivel para
distinguir cenarios agregados, parametrizacao ou testes ja presentes na linha
de base. A auditoria atual confirma os 7 metodos nomeados no codigo e nao
encontrou indicio verificavel de remocao. Nesta etapa, foram adicionados 4
metodos e a contagem passou de 237 para 241.

### Proximas prioridades

1. `AcaoRegistrationService`.
2. `BrapiIncomeAdapter` com respostas parciais e indisponibilidade total.
3. `TwelveDataCotacaoAdapter` com campos opcionais e falhas de logo.
4. `DashboardCarteiraService` em agregacoes monetarias.
5. `AuthProperties`, `MarketInsightService` e `VendaService`.
