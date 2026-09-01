# PFC5 Phase 0 核验结果（12 项）

| # | 核验项 | 结论 | 说明 |
|---|---|---|---|
| 1 | CSV schema (4 registries) | PASS | historical-failure-seed-registry.csv:ok instance-exposure-role-registry.csv:ok baseline-fair-readiness.csv:ok snapshot-identity-audit.csv:ok |
| 2 | seed selection determinism | PASS | in_class=['20260901', '20260904', '20260905'] selected=['20260901'] |
| 3 | instance role mutual exclusion & hard rules | PASS | n=49 unique=True diag=True no_validation_if_exposed=True |
| 4 | snapshot physical/logical hash identity | PASS | sha=84d845233e332a66… |
| 5 | raw front finite/dedup/strict-ND idempotence | PASS | 10 fronts ok |
| 6 | reference input order independence | PASS | canonical=4dc85dd4fa3c7824… |
| 7 | HV/IGD historical gold recalculation | PASS | max abs diff = 1.665e-16 (gate 1e-12) |
| 8 | Step 0 jar SHA reverse verification | PASS | formal=8dad8f40 runtime121=121fbb49 base723d=723d24ed |
| 9 | OFF/ON core behavior field comparison | PASS | 29 fields, unequal=[] |
| 10 | evidence-sha256 per-file reverse recompute | PASS | 139 files verified, mismatches=[] |
| 11 | documentation path existence | PASS |  |
| 12 | AGENTS/ROADMAP frozen-boundary consistency | PASS | roadmap=True agents=True |

总体：12/12 PASS。javaSourceChanged=false; buildNotRequired=true; consumedFE=0
