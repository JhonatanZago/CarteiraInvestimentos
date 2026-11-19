# Evolução da cobertura dos serviços financeiros

## Escopo

Foram ampliados os testes unitários de `VendaService`,
`AtivoCarteiraService` e `MovimentacaoFinanceiraService`. Não foram alteradas
fórmulas financeiras, entidades, migrations, frontend ou configuração de
segurança.

## Cenários cobertos

- Compra repetida preservada e preço médio existente mantido.
- Venda parcial e total.
- Lucro, prejuízo e resultado realizado zero.
- Quantidade, preço, data e taxas inválidas.
- Carteira inexistente, posição inexistente e posição de outra carteira.
- Exclusão de posição própria e preservação do bloqueio de ownership.
- Filtros por mercado, classificação, busca e ordenações de posição.
- Saldo de compras e vendas.
- Resultado realizado com venda parcial, venda anterior à compra e custos
  ausentes.

## Contadores JaCoCo

| Contador | Antes | Depois |
|---|---:|---:|
| Ramos cobertos | 665/1.051 (63,27%) | 693/1.051 (65,94%) |
| Ramos adicionais | — | 28 |
| Linhas | 86,51% | 87,63% |
| Instruções | 84,65% | 85,81% |
| Métodos | 85,29% | 85,88% |
| Classes | 97,24% | 97,24% |

Execução final:

```powershell
.\gradlew.bat --offline --no-daemon test jacocoTestReport --console=plain
.\gradlew.bat --offline --no-daemon jacocoTestCoverageVerification --console=plain
```

Resultado: **218 testes**, 0 falhas, 0 erros e 1 ignorado; relatório e portão
JaCoCo passaram.

## TDD

Os comportamentos selecionados já estavam corretos na implementação atual.
Portanto, os testes foram Green-first e não houve correção de produção. Não há
evidência legítima de novo ciclo RED nesta etapa; a evidência RED anterior está
preservada em `docs/testes/tdd-aula15.md`.

## Próximas prioridades

Ainda há 358 ramos não cobertos. Para aproximar-se de 80%, priorizar:

1. `DashboardCarteiraService`: custo aberto, agregações BRL/USD e estados de
   indisponibilidade.
2. `AcaoRegistrationService`: revalidação em lote e respostas incompletas.
3. `MovimentacaoFinanceiraService`: validações restantes e resultado realizado
   com múltiplas vendas.
4. `VendaService`: listagem, ownership autenticado e fronteiras de custo zero.
5. `ContextoMonetario` e `ExchangeRateService`: estados monetários e taxas
   inválidas.

Meta de 80%: pelo menos 841/1.051 ramos; faltam **148 ramos cobertos**.
