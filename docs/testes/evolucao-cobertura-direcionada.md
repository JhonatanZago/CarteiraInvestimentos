# Evolução da cobertura direcionada

## Escopo

Foram exercitados ramos adicionais de `AcaoRegistrationService`,
`DashboardCarteiraService` e `MovimentacaoFinanceiraService`, sem alteração de
produção, fórmulas financeiras, frontend ou autenticação.

## Métodos e cenários

- `AcaoRegistrationService.registrar`: ticker ausente nos dois mercados,
  falha do provedor com mercado alternativo, falha em ambos os mercados,
  ticker retornado divergente, resposta nula e histórico duplicado.
- `DashboardCarteiraService.custoAberto`: compras, venda parcial, venda total e
  posição encerrada removida da composição.
- `MovimentacaoFinanceiraService.validar`: request nulo, tipo, quantidade,
  preço, data nulos ou inválidos e data futura.

## Contadores JaCoCo

| Contador | Antes | Depois |
|---|---:|---:|
| Ramos cobertos | 715/1.051 (68,03%) | 737/1.051 (70,12%) |
| Ramos adicionais | — | 22 |
| Linhas | 88,38% | 89,12% |
| Instruções | 86,38% | 87,28% |
| Métodos | 85,88% | 85,88% |
| Classes | 97,24% | 97,24% |

## Validação

```powershell
.\gradlew.bat --offline --no-daemon test jacocoTestReport --console=plain
.\gradlew.bat --offline --no-daemon jacocoTestCoverageVerification --console=plain
```

Resultado: **234 testes**, 0 falhas, 0 erros e 1 ignorado. O relatório e o
portão JaCoCo passaram.

## TDD

Os cenários já estavam atendidos pela implementação atual. Não houve defeito
real que justificasse alteração de produção; os testes são Green-first. Nenhum
ciclo RED foi fabricado.

## Próximas prioridades

Ainda há 314 ramos não cobertos. As próximas prioridades são:

1. `AcaoRegistrationService.revalidar` e `revalidarTodos`.
2. `DashboardCarteiraService.analisarMoedas` e conversões nulas.
3. `MovimentacaoFinanceiraService.registrar` com falhas de carteira, ativo,
   corretora e câmbio.
4. `VendaService.listar` e ownership autenticado.
5. `PortfolioSnapshotService` com atualizações e histórico de taxas.

Para 80% são necessários 841 ramos cobertos; faltam **104**.
