# Design

## Context

O backend já calcula posições e dashboards com `BigDecimal`, possui movimentações com campos de câmbio e expõe moeda em parte dos DTOs. Entretanto, `PortfolioSnapshotService` ainda agrega valores sem contexto cambial, e telas Angular mantêm fallbacks implícitos para BRL. A solução precisa ser aditiva, preservar operações existentes e distinguir valor original, valor convertido e disponibilidade.

## Goals / Non-Goals

**Goals:**

- Tornar a moeda-base da carteira explícita.
- Transportar moeda original e valor convertido sem perder o valor original.
- Registrar taxa, instante efetivo, fonte e estado da conversão.
- Impedir agregações financeiras com moedas incompatíveis.
- Preservar o custo histórico das operações e snapshots já registrados.
- Tratar dados legados sem moeda ou câmbio comprovável como unresolved/estimated.

**Non-Goals:**

- Alterar regras de autenticação, autorização, BRAPI ou Security Gate.
- Inventar ou buscar taxas históricas ausentes.
- Recalcular retroativamente snapshots sem evidência da taxa histórica.
- Alterar fórmulas de preço médio, venda ou conversão já estabelecidas.
- Implementar compras repetidas; isso pertence à change específica de operações de compra.

## Decisions

### 1. Contexto monetário explícito

Cada carteira terá uma moeda-base explícita. Cada valor de posição continuará carregando sua moeda original. Respostas consolidadas carregarão ambos os contextos e não usarão `null` como sinônimo de BRL.

Alternativa rejeitada: manter BRL como fallback no frontend. Isso mascara dados legados e produz totais incorretos.

### 2. Proveniência cambial

Uma conversão será representada por taxa positiva, instante efetivo, fonte, moeda de origem e moeda de destino. O response deverá distinguir `AVAILABLE`, `ESTIMATED` e `UNAVAILABLE`. Uma taxa atual só poderá ser usada para valor atual quando válida no momento da consulta; ela não poderá reescrever o custo histórico.

Alternativa rejeitada: usar a cotação atual para reconstruir todo o passado. Isso altera o significado financeiro dos snapshots.

### 3. Agregação em moeda-base

O backend fará a conversão antes de somar valores de moedas diferentes. Cada valor original permanecerá disponível para a composição. Se qualquer parcela necessária não puder ser convertida com taxa válida, o total afetado será `null` ou terá estado explícito de indisponibilidade, conforme o contrato, nunca zero ou BRL implícito.

Alternativa rejeitada: somar números crus e converter o total depois, pois isso mistura unidades incompatíveis.

### 4. Histórico e snapshots

Snapshots novos armazenarão total original quando aplicável, total na moeda-base, moeda-base, taxa utilizada e metadados de origem. Snapshots antigos não serão reescritos automaticamente. Quando não houver evidência histórica, a evolução exibirá estado não auditável/estimado.

### 5. Compatibilidade legada

Posições com moeda comprovada continuarão legíveis e poderão participar de agregações. Posições sem moeda comprovável permanecerão consultáveis, mas serão marcadas como unresolved e não entrarão silenciosamente em totais convertidos. A revisão ou correção desses registros será uma ação explícita.

### 6. Precisão e apresentação

As fórmulas financeiras existentes serão preservadas. Operações manterão a precisão interna já contratada; a API e a UI não reduzirão a precisão antes da agregação. O arredondamento de apresentação seguirá o formatador existente, sempre acompanhado do código de moeda.

## Risks / Trade-offs

- [Dados legados incompletos] → Exibir indisponibilidade ou estado estimado e fornecer relatório de registros que precisam de revisão.
- [Provedores sem taxa histórica] → Não reconstruir o passado; permitir somente valores cuja taxa esteja comprovada.
- [Mudança de DTO] → Usar campos aditivos e manter campos antigos durante a transição, com testes de compatibilidade.
- [Maior payload] → Reutilizar um objeto de contexto cambial e evitar duplicar metadados por campo quando a taxa for compartilhada.
- [Falha de câmbio bloquear totais] → Preservar valores originais e explicar a indisponibilidade na UI, em vez de apresentar um total enganoso.

## Migration Plan

1. Adicionar contratos e campos aditivos para moeda-base e contexto cambial.
2. Criar migration somente após definir o backfill auditável; registros sem evidência permanecerão marcados como unresolved.
3. Atualizar backend e testes antes do frontend.
4. Atualizar frontend para remover fallbacks monetários silenciosos.
5. Validar carteiras BRL, USD, mistas, sem câmbio e com dados legados.
6. Rollback: manter campos antigos e desativar a exposição dos novos agregados sem reescrever dados históricos.
