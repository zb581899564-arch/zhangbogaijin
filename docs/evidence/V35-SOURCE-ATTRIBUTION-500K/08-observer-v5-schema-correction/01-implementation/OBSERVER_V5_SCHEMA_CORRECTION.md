# Observer V5 schema correction

## Purpose

V4 correctly reproduced the frozen A4 failure trajectory, but its exported data did not satisfy the Phase A0 source-attribution contract. V5 is an independent diagnostic artifact that corrects observation only; it does not replace or rebuild the formal algorithm Jar.

## Corrected contract

- `source-ledger.csv` exports `actualFE`, 25k `nominalFE`, `generation`, `outerCycle`, and `qRound`.
- `source-lifecycle-events.csv` is a true streaming event ledger. It records generated candidates, descendants, improving descendants, merge-pool entry, PDDR selection, working-population survival, personal-archive entry, Qg/Qp teacher use, and the actual Qp action.
- B0 is exported as the strict three-objective nondominated set of the first 100 evaluated initial solutions.
- parent-vector and parent-objective lookup is keyed by `parentLineageId`, not the child lineage.
- The evaluation ledger points Qp action analysis to the lifecycle ledger instead of writing a fabricated constant.
- The observer-owned memory estimate explicitly includes the new parent caches and lifecycle writer buffer. Large ledgers remain disk-streamed.

Lifecycle event FE is an observation timestamp: selection events that occur between evaluations carry the most recent successful decoder FE. It must not be interpreted as a new evaluation or as an exact causal completion timestamp.

## Identity

```ini
observerSchema=v35-source-attribution-observer-schema-v2
observerRunner=v35-source-attribution-observer-runner-v5
observerJarSha256=1A73E3CF025F7CFDB47BDE38A7B34E8F8B0810958F61323A5D3CBC35272C8C9E
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
formalJarChanged=false
algorithmSemanticsChanged=false
```

The previous V4 Jar and the already completed V4 SA-HARD run are retained as superseded diagnostic evidence. The V4 run remains valid for deterministic failure reproduction, but not for source-attribution conclusions.
