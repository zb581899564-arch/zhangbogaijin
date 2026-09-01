# V35 MASTER FORMAL RAW-RUN CAMPAIGN STATUS

最后更新：2026-08-25  
状态：`PAUSED_NO_PROMOTED_V35_FINAL_CANDIDATE`

## Gate 状态

| Gate | 状态 | 说明 |
|---|---|---|
| `FINAL_SOURCE_FREEZE` | `ACCEPTED` | 隔离 clean tag `v35-final-doe1-frozen` → `2b3316b...`；Jar `8dad8f...ad8b9` |
| `FORMAL_MANIFEST_FREEZE` | `ACCEPTED` | 45 instances、20 seeds、900 shared snapshots；证据 manifest SHA 已复核 |
| `A0_A4_PRODUCTION_PREFLIGHT` | `ACCEPTED` | A0--A4/50k 全部通过 phase-bound、共享初群和机制门；A4为合法尾停 48269 FE |
| `FORMAL_MAX_PARALLEL` | `ACCEPTED=16` | 冻结 jar 的 4/8/12/16 JVM benchmark 全通过；16 JVM 无 swap / OOM / 超订阅 |
| `A0_A4_FINAL_SEMANTICS` | `ACCEPTED` | A0 公平适配、A4 frozen Final、A0--A4 语义阶梯审计 |

## Campaign 计数

| 项目 | 数量 |
|---|---:|
| 已冻结 instance × seed 起点 | 45 × 20 = 900 |
| 批准 roster | A0、A1、A2、A3、A4 |
| 预期唯一物理 RunKey | 5 × 45 × 20 = 4500 |
| 已物化 master RunKey | 4500（Master v2清单；不得因物化而自动运行） |
| 已接受完整五臂组 | 12 / 900 |
| 已接受配对运行 | 60 / 4500 |
| 排除的孤立完成运行 | 8 |
| 排除的partial attempt | 7 |
| 运行中 | 0 |
| 待决策运行 | 4440（计划量；不得调度） |

## Evidence / analysis 状态

| 项目 | 状态 |
|---|---|
| Master run registry、atomic completion、retry/invalid paired-group 规则 | READY |
| `PFref_ablation=ND(A0..A4, all 20 seeds)` 入口 | READY，等待有效 raw fronts |
| `PFref_main=ND(A0,A4, all 20 seeds)` 入口 | READY，复用同一 raw runs，等待有效 raw fronts |
| 论文结果接线 | READY，仅保留占位，不含正式数值 |
| 正式性能/统计/优越性结论 | NOT_GENERATED |

## 阻断与自动启动条件

正式矩阵曾启动但现已暂停。方案 C 已把 `MaxFEs` 定义为最大允许完整评价次数；Master 必须为每条
运行写入 `budget-termination.properties`，仅在 `0 < actualFE=decoderCalls<=MaxFEs` 且
`remainingFE<5000` 时接受。任何实际 FE 仍必须如实报告；不得补评价、修改算法、局部预算
或 Q/LS 参数去填满预算。

版本化 A0--A4 snapshot-bound formal arm launcher 与 Master renderer v2已经交付并通过验收；
历史 `FORMAL_MATRIX_BLOCKER.md` 只保留为已关闭技术债证据。当前停止条件不再是launcher缺失，也不再是
A3/PDDR纯观察审计；两者均已有结论。D-103已把下一门定义为A2/A4跨规模多实例确认，详见
`docs/V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md`。

完整启动所需条件为：

```text
FINAL_SOURCE_FREEZE = ACCEPTED
FORMAL_MANIFEST_FREEZE = ACCEPTED
A0_A4_PRODUCTION_PREFLIGHT = ACCEPTED
FORMAL_MAX_PARALLEL = ACCEPTED
A0_A4_FINAL_SEMANTICS = ACCEPTED
```

当前状态保持`PAUSED`。未经D-103确认完成、Final候选裁决、必要的freeze/preflight/吞吐复核及用户
新批准，不得恢复Master或调度剩余运行。唯一当前状态入口为
`docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`。

2026-08-25补充：D-103之后的独立A0/A2 Final-candidate confirmation 已完成。60/60运行、30/30配对
均有效，但A2未通过总体Cmax、跨实例/规模和`100_8_3_1`否决门，裁决`A2_NOT_PROMOTED`。当前没有
可作为本Master最终算法的晋升候选；其输出只保留在
`docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/`，不得进入任何正式PFref或统计。

证据归档和安全清理已完成；Stage2 master目录、12个完整五臂组和暂停标记均保持原状。
