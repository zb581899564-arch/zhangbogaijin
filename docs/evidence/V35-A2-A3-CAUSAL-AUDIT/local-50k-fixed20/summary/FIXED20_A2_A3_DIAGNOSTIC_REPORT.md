# Fixed `20_2_3_1` A2→A3 causal diagnostic

- Scope: local independent JVM, FM3/Shift.NONE, population 100, maxFEs 50,000.
- Seeds: `20260822`, `20260823`, `20260824`; arms: `A2`, `A3`; completed runs: 6/6.
- This report is based only on `local-50k-fixed20`; earlier `local-50k` outputs used `100_2_3_1` and are not substituted here.
- `V35FairRunner` exports observations after the algorithm returns; `writeRecord` does not feed any value back into control flow.

## Run and evidence gate

| check | result |
|---|---:|
| fixed scope + COMPLETED + 50,000 FE + 50,000 decoder calls + zero illegal/duplicate | 6/6 |
| evidence-sha256.tsv recomputed with listed-file set equal to actual-file set | 6/6 |
| initial population hash equal within each A2/A3 seed pair | 3/3 |

## Paired metrics

`delta` is A3 minus A2. For minima, negative is better for A3; for HV, positive is better; for IGD, negative is better.

| seed | Δmin Cmax | Δmin TEC | Δmin TWC | ΔHV | ΔIGD | A3 Qp total/retained | pair class |
|---:|---:|---:|---:|---:|---:|---:|---|
| 20260822 | -4.453616 | 290.257398 | 228.669230 | -0.247788 | 0.161113 | 13326/4096 | `REGRESSION_SIGNAL` |
| 20260823 | 1.540485 | 142.116709 | -102.328517 | -0.061869 | 0.037697 | 13291/4096 | `REGRESSION_SIGNAL` |
| 20260824 | 4.666786 | 95.876420 | -263.371683 | -0.053279 | 0.053533 | 13268/4096 | `REGRESSION_SIGNAL` |

## Mechanism evidence

- A2 is the disabled-Qp control: Qp total `0`, action counts `0`, table hash `disabled`; lineage and dual-Q event totals are also `0`.
- A3 has Qp totals `13326, 13291, 13268`, retained payload `4096, 4096, 4096`, and action counts `5100, 5100, 5100`; its Q-table hash, Qp stream hash, lineage stream hash, and dual-Q stream hash are exported per run.
- A3 lineage totals are `653, 668, 668`; dominated/duplicate removals are `16, 22, 38`/`50, 43, 36`; truncations are `0, 0, 0`.
- A3 dual-Q phase counts are WARMUP/P/G = `49/26/25` in each run.
- Qp action counts (KEEP/DIRECTIONAL/EPSILON/COMPLEMENTARY) and percentages are:
  - seed `20260822`: `3571/650/838/41` = `70.02%/12.75%/16.43%/0.80%`.
  - seed `20260823`: `4446/273/368/13` = `87.18%/5.35%/7.22%/0.25%`.
  - seed `20260824`: `4658/169/270/3` = `91.33%/3.31%/5.29%/0.06%`.
- Qp average rewards (KEEP/DIRECTIONAL/EPSILON/COMPLEMENTARY) are:
  - seed `20260822`: `-6.12662e+08/-9.3698e+08/-0.141222/0.17036`; max absolute action average = `9.3698e+08`.
  - seed `20260823`: `-2.21672e+08/0.00191743/-0.344304/-0.379302`; max absolute action average = `2.21672e+08`.
  - seed `20260824`: `-9.06949e+07/-1.16998e+09/-5.11323e+09/-0.31965`; max absolute action average = `5.11323e+09`.
- The retained Qp window has reward-event rows `700, 700, 700`, select rows `1610, 1616, 1613`, and lineage rows `100, 100, 100` (A3 seeds in order); reward rows are not a complete-history export.
- **Located defect candidate (`QP_SELECTION_OR_REWARD`)**: `ZhangBoQpController.reward()` computes `direction=(oldPhi-newPhi)/(abs(oldPhi)+normalizationEpsilon)`; the current personal-archive default epsilon is `1e-12`. The A3 action averages reach `10^8–10^9` (and one EPSILON average reaches about `-5.11e9`), which is numerically pathological if `oldPhi` is near zero. This is a source-level candidate, not yet a proven performance cause.
- Qp event payload is a 4,096-entry rolling window: A3 total exceeds retained in all three runs. Counts, terminal stream hash, action totals/average rewards, Q-table hash, lineage counters, and dual-Q phase counts are available; the complete Qp event sequence is not.

## Five-category gate

The categories are audit labels, not statistical significance claims:

1. `INVALID_EVIDENCE`: scope, budget, initial/evaluation hash, or evidence manifest fails.
2. `LOGGING_INSUFFICIENT`: run is otherwise usable but required run-end count/hash/action fields are absent.
3. `NO_REGRESSION_SIGNAL`: no A3 Qp activity or A3 wins at least two of three objective minima.
4. `REGRESSION_SIGNAL`: Qp is active and A3 loses at least two of three objective minima; this is a paired quality label.
5. `COMPOSITE_BLOCK_UNRESOLVED`: the runs are valid and may show regression, but A3 bundles Qp, personal archive/lineage, and dual-Q changes, or contains an unresolved reward-pathology candidate.

**Paired quality signal: `REGRESSION_SIGNAL`** (`3/3` seed pairs meet the regression rule).
**Final five-category classification: `%s`**. The fixed evidence supports a repeatable quality regression signal, but not an isolated causal attribution; A3 changes Qp plus personal-archive/lineage and the dual-Q phase schedule together. The reward anomaly is a located defect candidate, not proof that it caused the quality loss.

## Continue / stop gate

- Continue only with a single-variable follow-up that keeps the fixed snapshot, seed, budget, population, PDDR semantics, and random stream unchanged; first compare A3 Qp action/reward/table transitions against a Qp-only or archive-only control.
- Stop the current causal claim at `COMPOSITE_BLOCK_UNRESOLVED`; do not call Qp selection/reward the proven root cause while Qp, personal archive/lineage, and dual-Q differences remain bundled.
- Do not collect full Qp history by changing algorithm decisions. If full sequence attribution is required, add a passive unbounded/streaming sink and rerun the same six-cell matrix only after the ON/OFF gate remains green.

## Recompute

```powershell
python scripts/analyze_fixed20_diagnostic.py --audit-dir docs/evidence/V35-A2-A3-CAUSAL-AUDIT
```
