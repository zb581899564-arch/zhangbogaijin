# V35 Final Experiment Status

最后更新：2026-08-23。状态仅允许：`NOT_STARTED`、`RUNNING`、`BLOCKED`、`READY`、`COMPLETED`、`INVALID`。

| Track | Owner | Status | Dependency | Runs | Evidence | Blocker |
|---|---|---|---|---:|---|---|
| Final Freeze / Campaign Runner | A | READY | current source snapshot + final matrix approval | 0 formal | `docs/evidence/V35-FINAL-FREEZE/` | 现有冻结是脏工作树候选快照；FC-8、主版本、最终 source manifest 和正式矩阵未闭合 |
| Formal V35 vs HMOPSO-QGS-F | B | BLOCKED | final freeze + formal matrix audit | 0 | `docs/evidence/V35-FORMAL-EXPERIMENTS/` | FC-8/EXP-1/FC-9 未闭合；20-seed、算法 roster、45实例×hash 与每 seed 初群双哈希未冻结（fail-closed preflight 已备） |
| A0-A4 semantic audit + smoke | C | READY | semantic smoke + explicit user approval | 0 formal | `docs/evidence/V35-ABLATION/` | 2k 机制与账本预检通过；正式 500k 消融需要 FC-8/正式矩阵闭合及人工批准 |
| Metric / statistics pipeline | D | READY | none | 0 formal | `docs/evidence/V35-ANALYSIS/` | 流水线、模板与数学/小前沿测试已备妥；等冻结的正式 raw fronts 与 metadata 齐备后才可产出最终统计 |
| Paper methods / experiment skeleton | E | COMPLETED | none | 0 formal | `docs/paper/` | 骨架、方法草稿、实验与结果占位契约已建立；正式结果仍等待 raw fronts、PFref、指标/统计冻结 |

## Frozen baseline

```text
mixture=20/40/20/20
FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT; ShiftMode=NONE
GLOBAL_ORIGINAL; CA-TA-Lite -> inherited LS; A4-Pacing; P=5/G=5
rho=0; directionalTeacherPool=false; population=100; MaxFEs=500000
```

DOE-1 confirmation 的 60 / 60 运行已完成；三个候选容量均未通过 `median ΔCmax >= 2%` 的替换门，证据见 `docs/evidence/V35-DOE1-subgroup-mixture/`。
