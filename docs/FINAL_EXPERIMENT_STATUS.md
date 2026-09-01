# V35 Final Experiment Status

> **当前覆盖状态（2026-08-30 / D-110）**：当前不存在已批准Final；旧Final Source Freeze仅是
> `A4_LEGACY`历史构建物，不得解释为当前Final。新的活动路线见
> [`V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md`](V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md)。
> 当前阶段为`V35_GAP_PROBE_P0`，仅允许0-FE预登记；`gapProbeStarted=false`、
> `validationStarted=false`、`FinalCandidateApproved=false`、`formalMatrixRunning=false`。

最后更新：2026-08-25。Stage2 用户决策已将历史 FC-8/FC-9 前置改标为
`SUPERSEDED_BY_FC6_AND_DOE1_EVIDENCE`；它们不再阻断本阶段。状态仅允许：
`NOT_STARTED`、`RUNNING`、`BLOCKED`、`READY`、`COMPLETED`、`INVALID`。

| Track | Owner | Status | Runs | Evidence | 当前结论 / 阻断 |
|---|---|---|---:|---|---|
| Historical A4 Source Freeze | A | COMPLETED | 0 formal | `docs/evidence/V35-FINAL-FREEZE/` | 旧tag与Jar保持可复现，但D-110后只称`A4_LEGACY`，不代表当前Final |
| Formal Manifest / Fairness | B | COMPLETED | 0 formal | `docs/evidence/V35-FORMAL-MANIFEST/` | 45 instances、20 seeds、900 份共享四向量 snapshot，45×20×5 覆盖；manifest hash 零漂移 |
| A0–A4 Historical Semantics | D | COMPLETED | 0 formal | `docs/evidence/V35-A0-A4-FINAL-SEMANTICS/` | A0=规范公平适配 HMOPSO-QGS-F；A4为`A4_LEGACY`；身份审计有效但不构成Final晋升 |
| Production Preflight / Throughput | C | COMPLETED | 5 Gate3 + 40 throughput | `docs/evidence/V35-PHASE-BUDGET-PROTOCOL/` | A0--A4/50k phase-bound Gate3 与 4/8/12/16 JVM 均通过；`FORMAL_MAX_PARALLEL=16` |
| A2 vs A4 Multi-instance confirmation | D/E | COMPLETED | 60 accepted / 60 planned | `docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/` | 六实例、五seed均完成；30/30有效配对，但100-job否决门触发，裁决`A4_NOT_PROMOTED` |
| A0 vs A2 Final-candidate confirmation | D/E | COMPLETED | 60 accepted / 60 planned | `docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/` | 30/30有效配对；总体HV/IGD为正但Cmax门、4/6实例门、20-job一致性门及`100_8_3_1`否决门失败，裁决`A2_NOT_PROMOTED` |
| Master A0–A4 Raw Campaign | E | PAUSED | 60 paired pilot / 4500 conditional | `docs/evidence/V35-STAGE2-MASTER-V2/` | 外置五臂Runner和Master v2已验收并曾启动；12个完整组仅为先导；D-103确认、Final freeze和用户新批准前禁止恢复 |
| Metric / statistics pipeline | D/E | READY | 60 paired pilot only | `docs/evidence/V35-ANALYSIS/`、`docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/` | 已生成单实例先导reference；正式PFref和论文统计未生成 |
| Paper methods / experiment skeleton | E | COMPLETED | 0 formal | `docs/paper/` | 骨架/占位契约已建立；没有正式数值或优越性结论 |

## Frozen baseline

```text
mixture=20/40/20/20
FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT; ShiftMode=NONE
GLOBAL_ORIGINAL; CA-TA-Lite -> inherited LS; A4-Pacing; P=5/G=5
rho=0; directionalTeacherPool=false; population=100; MaxFEs=500000
```

DOE-1 confirmation 的 60 / 60 运行已完成；三个候选容量均未通过 `median ΔCmax >= 2%` 的替换门。
Stage2 的 strict-exact Gate3 已由用户明确替换为方案 C：`MaxFEs` 是最大允许完整评价次数，
完整 Q phase 不可拆分。当前只接受 `0 < actualFE=decoderCalls<=MaxFEs` 且
`0<=remainingFE<5000` 的运行；五臂实际 FE 范围必须小于 5000。算法 jar 和终端搜索语义
不变。Gate3、吞吐门、正式 A0--A4 launcher 与 Master renderer 已闭合。Master曾启动，因此
`formal_matrix_started=true`仅作为历史事实；当前必须同时记录
`formal_matrix_running=false`、`formal_matrix_paused=true`。A3/Qp个人档案与PDDR生命周期审计已完成；
2026-08-25：D-103确认已完成，A4未通过100-job门；后续独立A0/A2确认也已完成，A2同样未通过
跨尺度Final晋升门。因此当前不存在已晋升的V35 Final候选；不得将任一确认集用于调参、救A2/A4或
自动恢复旧4500矩阵。

唯一当前状态入口：`docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`。

论文证据总账、训练机目录地图、G盘冷归档和可恢复清理已于2026-08-24验收完成；该治理工作不
改变上表任何科学状态，也不授权恢复Master。
