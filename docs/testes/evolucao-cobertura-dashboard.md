# Evolução da cobertura de dashboard, ativos e câmbio

## Escopo

Foram ampliados testes de `DashboardCarteiraService`,
`ContextoMonetario`, `ExchangeRateService` e `PortfolioSnapshotService`.
Nenhum código de produção, regra financeira, frontend ou change multimoeda foi
alterado.

## Cenários implementados

- Exposição cambial de carteira mista BRL/USD.
- Contagem de resultados positivos, negativos e neutros.
- Carteira sem posições.
- Conversão cambial indisponível sem total artificial.
- Contexto disponível incompleto, estimado, indisponível e reconversão inválida.
- Taxa cambial nula, par não suportado, indicador inválido e exceção do
  provedor.
- Snapshot vazio com base zero disponível.
- Snapshot com moeda não resolvida e indisponibilidade explícita.

## Contadores JaCoCo

| Contador | Antes | Depois |
|---|---:|---:|
| Ramos cobertos | 693/1.051 (65,94%) | 715/1.051 (68,03%) |
| Ramos adicionais | — | 22 |
| Linhas | 87,63% | 88,38% |
| Instruções | 85,81% | 86,38% |
| Métodos | 85,88% | 85,88% |
| Classes | 97,24% | 97,24% |

Execução final:

```powershell
.\gradlew.bat --offline --no-daemon test jacocoTestReport --console=plain
.\gradlew.bat --offline --no-daemon jacocoTestCoverageVerification --console=plain
```

Resultado: **227 testes**, 0 falhas, 0 erros e 1 ignorado; relatório e portão
JaCoCo passaram.

## TDD e limitações

Os testes foram Green-first: a implementação existente já atendia os cenários
selecionados. Nenhum defeito foi inventado e nenhum novo ciclo RED -> GREEN ->
REFACTOR foi declarado. O ciclo RED anterior está documentado em
`docs/testes/tdd-aula15.md`.

## Próximas prioridades

Ainda há 336 ramos não cobertos. As próximas classes prioritárias são:

1. `AcaoRegistrationService`, especialmente validações de cadastro e
   revalidação em lote.
2. `DashboardCarteiraService`, custo aberto e snapshots com histórico de
   movimentações.
3. `MovimentacaoFinanceiraService`, validações restantes e múltiplas vendas.
4. `VendaService`, listagem autenticada e fronteiras de custo zero.
5. `PortfolioSnapshotService`, caminhos de atualização e histórico de câmbio.

Para 80% são necessários pelo menos 841 ramos cobertos; faltam **126**.
