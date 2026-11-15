# Legacy Currency Inventory

## Scope and evidence

This inventory is a read-only classification of the persisted model and its currency evidence. The repository does not contain a durable production database dump, and the development/test profiles use in-memory H2 databases. No database rows were rewritten, migrated, or queried outside the repository.

Classification values:

- `RESOLVED`: the record has the required currency evidence in the current model.
- `ESTIMATED`: the original currency is known, but historical conversion evidence is incomplete.
- `UNRESOLVED`: a required monetary context is absent or cannot be proven from the record.

## Record-class inventory

| Record class | Currency evidence | Historical FX evidence | Classification | Reason / next action |
|---|---|---|---|---|
| `Carteira` | No `moedaBase` field exists | Not applicable | `UNRESOLVED` | Portfolio base currency is not persisted. Define it in a later additive persistence task; do not infer it from the first position. |
| `Acao` | `moeda` is mandatory and stored as an enum | Not applicable for the quote itself | `RESOLVED` | The quotation's original currency is explicit. Rows with a null value would violate the current schema and must be reported as invalid rather than treated as BRL. |
| `AtivoCarteira` | Original currency is derived from the required `Acao.moeda` | `cambioHistoricoComprovado` defaults to false; `isConversaoEstimada()` identifies non-BRL positions without proof | `RESOLVED` for original currency; `ESTIMATED` for historical BRL conversion when proof is false | Preserve the original amount and surface the estimated state. Do not replace the missing historical rate with the current rate. |
| `MovimentacaoFinanceira` | `moeda` and `valorOriginal` are mandatory | `cambioParaBrl`, `cambioEm`, and `cambioFonte` are optional | Conditional: `RESOLVED` only when same-currency/identity or all FX fields are valid; otherwise `UNRESOLVED` | Validate each row before using `valorBrl`. Missing, non-positive, or incomplete FX metadata cannot be converted to zero or BRL by default. |
| `PortfolioSnapshot` | No currency or base-currency field exists | No rate, effective timestamp, or source field exists | `UNRESOLVED` for mixed-currency interpretation | Existing snapshots remain unchanged. They cannot be retroactively reconstructed without historical evidence; future persistence changes belong to task 1.3. |

## Field-level rules

### Portfolio

`Carteira` contains identity, owner, name, description, and creation date, but no base currency. Every existing portfolio is therefore `UNRESOLVED` for base-currency reporting until an explicit value is introduced and safely backfilled.

### Position

`AtivoCarteira` does not duplicate currency; it references `Acao`, whose `moeda` column is non-null. This resolves the original currency when the referenced asset is valid. It does not prove the historical USD/BRL rate used when the position was acquired. The existing `cambioHistoricoComprovado` flag and `isConversaoEstimada()` method are the authoritative compatibility signal for that gap.

### Financial operation

`MovimentacaoFinanceira` stores both the original value and optional BRL conversion provenance. A row is resolved for conversion only when:

1. `moeda` and `valorOriginal` are present;
2. BRL identity conversion is explicit, or `cambioParaBrl` is positive;
3. `cambioEm` and `cambioFonte` are present for non-BRL conversion; and
4. `valorBrl` is consistent with the stored original value and rate.

Rows failing any condition are `UNRESOLVED`; the original value remains authoritative.

### Snapshot

`PortfolioSnapshot` stores aggregate values but no monetary context. Because its values may represent positions in more than one currency, every existing snapshot is `UNRESOLVED` for auditable base-currency interpretation. No current rate may be applied retroactively.

## Compatibility decision

- Existing public DTOs and entities are unchanged by this inventory.
- No default BRL value is introduced.
- No missing rate is generated.
- No historical amount is recalculated.
- The inventory is evidence for tasks 1.3 and 2.x; it is not a migration or a data repair.

## Verification

The classification was checked against:

- `src/main/java/com/example/carteirainvestimento/domain/Carteira.java`
- `src/main/java/com/example/carteirainvestimento/domain/Acao.java`
- `src/main/java/com/example/carteirainvestimento/domain/AtivoCarteira.java`
- `src/main/java/com/example/carteirainvestimento/domain/MovimentacaoFinanceira.java`
- `src/main/java/com/example/carteirainvestimento/domain/PortfolioSnapshot.java`
- `src/main/java/com/example/carteirainvestimento/repository/CarteiraRepository.java`
- `src/main/java/com/example/carteirainvestimento/repository/AtivoCarteiraRepository.java`
- `src/main/java/com/example/carteirainvestimento/repository/MovimentacaoFinanceiraRepository.java`
- `src/main/java/com/example/carteirainvestimento/repository/PortfolioSnapshotRepository.java`

All four requested legacy record families—portfolios, positions, financial operations, and snapshots—have an explicit classification and handling rule. Actual row counts remain unavailable because no persistent database was in scope.
