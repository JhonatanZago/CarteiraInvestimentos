# Matriz de auditoria de campos — TASK 0.4

Classificação: **defeito confirmado** = comportamento observado no código; **risco** = pode produzir inconsistência dependendo dos dados; **dívida técnica** = ausência estrutural que limita evolução; **hipótese** = precisa ser reproduzida com dados reais; **melhoria opcional** = não bloqueia a regra atual.

## Campos financeiros por tela

| Tela/campo | Origem real | Moeda esperada | Fórmula/formatação atual | Situação |
|---|---|---|---|---|
| Ativos — cotação | `AcoesApiService.list()` → `Acao.cotacaoAtual`/`moeda` | Moeda persistida no ativo | `pages.ts` usa `money(valor, item.moeda)` | Funcional; cobertura de moedas depende do backend/provedor |
| Ativos — histórico | `AcoesApiService.history()` → `HistoricoCotacao.valor` | Moeda do ativo | `HistoricoPage` chama `money(item.valor)` sem moeda | Defeito confirmado: fallback implícito BRL |
| Carteiras — prévia da posição | `selectedAcao.cotacaoAtual` e formulário | Moeda do ativo | `money(resumo.precoMedio/valorAtual)` sem moeda | Defeito confirmado: pode exibir USD como BRL |
| Carteiras — tabela de posições | `/carteiras/{id}/posicoes` → `Posicao` | `position.moeda` | Maioria usa `money(..., position.moeda)` | Funcional quando moeda vem preenchida; legado sem moeda cai em comportamento inconsistente |
| Carteiras — total consolidado | `/dashboard/carteiras/{id}` | BRL/base da carteira | Backend converte USD via `ExchangeRateService`; `DashboardCarteiraService` soma convertidos | Parcial: conversão só cobre USD e ausência de taxa produz total atual nulo |
| Dashboard — card patrimônio | `DashboardApiService.getPortfolio()` | BRL/base | `InsightsDashboardPage.money()` defaulta `BRL` | Risco confirmado para carteiras internacionais sem contrato explícito de moeda-base |
| Dashboard — composição | `DashboardCarteiraResponse.composicao` | Moeda original por posição | Backend calcula `quantidade × preço`; frontend formata com `item.moeda || 'BRL'` em alguns pontos | Defeito confirmado: ausência de moeda é mascarada como BRL |
| Dashboard — evolução | `/carteiras/{id}/evolucao` → snapshots | Moeda do snapshot/base | `InsightsDashboardPage` formata pontos com BRL | Dívida técnica: snapshot não carrega moeda/taxa histórica |
| Dashboard — distribuição | `dashboard.composicao` ou `/analise-moedas` | Percentual em moeda-base; linha em moeda original | Backend calcula análise convertida; fallback frontend soma valores crus por moeda | Defeito confirmado: fallback mistura BRL/USD diretamente |
| Dashboard — lucro/prejuízo | `analise-moedas` + composição | Resultado separado por moeda | `resultForCurrency()` separa por código, mas templates usam fallback BRL | Risco confirmado quando DTO não envia moeda |
| Dashboard — análise de risco | composição atual | Percentual em moeda-base | `countryShare()` soma `valorAtual` sem câmbio | Defeito confirmado para carteira mista |
| Dashboard — desempenho | composição atual | Moeda original | `money(item.cotacaoAtual, item.moeda || 'BRL')` | Defeito confirmado: fallback BRL indevido |
| Histórico — cotação | `/acoes/{id}/historico` | Moeda do ativo | valor sem moeda no DTO `HistoricoCotacaoResponse` | Dívida/defeito confirmado de contrato |

## Matriz de contratos e fórmulas

| Contrato/serviço | Evidência | Situação |
|---|---|---|
| `AcaoResponse` | Contém `Moeda moeda`, mercado, país/listagem, bolsa e logo | Funcional no contrato Java; deve ser validado ponta a ponta |
| `AtivoCarteiraResponse` | Contém `Moeda moeda`, valores investido/atual/resultado | Funcional no DTO; construtor de compatibilidade defaulta `Moeda.BRL` | Defeito confirmado: default mascara dados legados |
| `ComposicaoCarteiraResponse` | Contém moeda opcional no construtor legado | Defeito confirmado: compatibilidade permite `null` |
| `CalculadoraFinanceira` | `investido = quantidade × precoMedio`; `atual = quantidade × cotacaoAtual`; resultado por diferença | Correto para uma moeda; não trata conversão cambial histórica |
| `DashboardCarteiraService` | Converte USD→BRL para totais consolidados | Parcial; composição individual permanece original, mas risco e alguns fallbacks não convertem |
| `PortfolioSnapshotService` | Snapshot diário/eventos com BigDecimal | Dívida técnica: snapshot não registra FX nem moeda por componente |
| `InsightsApiService` | Chama indicadores, evolução, proventos e análise de moedas separadamente | Risco: chamadas complementares podem falhar independentemente; requer estados explícitos |

## Achados classificados

- **Defeitos confirmados (P0/P1):** fallbacks BRL em telas internacionais; análise de risco soma moedas sem conversão; construtores Java e modelos TypeScript permitem ausência de moeda; histórico não transporta moeda.
- **Riscos confirmados (P1):** total consolidado fica nulo quando câmbio USD/BRL está indisponível; snapshots não preservam câmbio histórico; filtros/evolução não conseguem auditar moeda do ponto.
- **Dívidas técnicas (P1/P2):** ausência de migrations versionadas; posição registra somente preço médio/data inicial, sem operações e câmbio por transação; respostas de indicadores/proventos ainda são estruturas reduzidas.
- **Hipóteses a reproduzir (P1):** ativos americanos já persistidos como BRL; falhas de build Angular podem ser específicas do ambiente sandbox; resposta real dos provedores pode não preencher `moeda` em todos os adapters.
- **Melhorias opcionais (P3):** centralizar pipe monetário; adicionar testes de contrato gerados; telemetria de latência e cache por adapter.

## Ordem recomendada derivada da matriz

1. Corrigir contratos/defaults de moeda e erros tipados (P0).
2. Validar/revalidar ativos existentes e impedir cadastro incompatível (P0/P1).
3. Modelar câmbio histórico por movimentação e snapshot (P0/P1).
4. Corrigir consolidação, risco, distribuição e resultados (P1).
5. Padronizar frontend, logos, estados e acessibilidade (P2).
