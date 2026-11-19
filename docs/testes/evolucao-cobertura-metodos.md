# Evolucao de cobertura por metodos

## Comando 73

### Linha de base

- 234 testes.
- 737/1.051 ramos cobertos (70,12%).
- 1.434/1.609 linhas cobertas (89,12%).

### Cenarios adicionados

| Classe | Cenarios |
|---|---|
| `AcaoRegistrationService` | Revalidacao de ativo inexistente; falha do provedor; logo do provedor; fallback curado; contagem sem logo. |
| `DashboardCarteiraService` | Consolidacao de custo historico e exclusao de posicao encerrada. |
| `MovimentacaoFinanceiraService` | Entradas nulas, tipos ausentes, quantidades/precos invalidos e datas futuras, sem acesso aos repositorios. |

### Defeito corrigido

`ExchangeRateService` validava a data efetiva contra `OffsetDateTime.now()`,
ignorando o `Clock` injetado. Isso fazia uma cotacao fixa de teste tornar-se
indevidamente futura conforme o calendario avancava. A validacao agora usa o
instante do `Clock` do servico; nenhum fallback cambial foi introduzido.

### Resultado real

- 237 testes executados.
- 0 falhas.
- 0 erros.
- 1 ignorado.
- Instrucoes: 10.240/11.719 (87,38%).
- Ramos: 740/1.051 (70,41%).
- Linhas: 1.434/1.609 (89,12%).
- Metodos: 433/503 (86,08%).
- Classes: 141/145 (97,24%).
- `jacocoTestCoverageVerification`: PASS.

O ganho foi de 3 ramos, de 737 para 740. Para 80% sao necessarios 841 ramos
cobertos; permanecem 101 ramos.

### Evidencia TDD

O teste de cotacao futura falhou legitimamente antes da correcao (RED), passou
apos a menor alteracao necessaria (GREEN) e nao exigiu nova refatoracao de
comportamento. Os demais testes foram Green-first e nao devem ser apresentados
como novos ciclos RED.

### Proximas prioridades

1. `AcaoRegistrationService.revalidar` e `revalidarTodos`.
2. `DashboardCarteiraService.analisarMoedas` e lambdas de agregacao.
3. `MovimentacaoFinanceiraService.registrar` e `resultadoRealizado`.
4. `VendaService.calcular` e filtros de `listar`.
5. `PortfolioSnapshotService.preencherContextoBase` e `obterContexto`.

Os proximos testes devem continuar focados em decisoes observaveis, excecoes e
fronteiras, sem chamadas HTTP reais e sem alterar regras financeiras para obter
percentuais.
