# V35 A2 与 A4 多实例确认协议

状态：`PRE_REGISTERED_NOT_STARTED`  
版本：`v35-a2-a4-heldout-confirmation-v1`  
日期：2026-08-24

## 1. 目的与边界

本协议只回答一个问题：在未参与此前开发、DOE 或机制裁决的实例上，完整 A4 能否稳定优于 A2。

它不是 Qp 调参、PDDR 改造、档案实验、局部搜索顺序实验、正式消融或论文统计。任何运行开始后均不得按中途结果修改算法、预算、实例、seed、指标口径或裁决门。

两臂定义：

| Arm | 含义 |
|---|---|
| `A2_CFVF` | 公平 HMOPSO-QGS 骨架 + DSCR + CFVF；不含个人档案、Qp、双Q冻结和 CA-TA-Lite |
| `A4_BUDGET_AWARE_CATA` | A2 加个人档案、Qp、10%预热、P=5/G=5双Q冻结及 CA-TA-Lite |

已有 `100_2_3_1 × 12 seed` 结果只构成先导。它支持 A4 的整体信号，但不得替代本协议的多实例结论，也不得用于选择本协议的实例或 seed。

## 2. 冻结搜索语义

所有 60 条运行均使用冻结 Jar：

```text
jarSha256 = 8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

除 A2/A4 合法机制差外，下列字段必须完全相同：

```text
decoder = FM3
familyMode = DEGENERATE_SINGLE_FAMILY
setupMode = SEQUENCE_INDEPENDENT
shiftMode = NONE
subSwarm = [G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC] = [20,40,20,20]
pddrSelectionMode = GLOBAL_ORIGINAL
localSearchOrder = CA-TA-Lite -> inherited LS
warmup = 10 percent
dualQBlock = P=5/G=5
rho = 0
directionalTeacherPool = false
pressureMask = disabled; BAL permits N1-N5
population = 100
requestedMaxFE = 500000
budgetProtocol = PHASE_CONSISTENT_BUDGET_TERMINATION
```

禁止改动 Qp、个人档案、PDDR、子群配比、Pacing、奖励、rho、CA-TA、压力诊断或局部搜索顺序。不得使用诊断工作树重新构建或替换冻结 Jar。

## 3. 已锁定确认设计

确认实例均未出现在当前有效开发/裁决运行总账中：

| 规模 | 实例 |
|---|---|
| 20-job | `20_2_4_1`、`20_5_3_1` |
| 50-job | `50_2_4_1`、`50_5_3_1` |
| 100-job | `100_2_4_1`、`100_5_3_1` |

固定 seed：`20260901`、`20260902`、`20260903`、`20260904`、`20260905`。

```text
6 instances x 5 paired seeds x 2 arms = 60 physical runs
```

每个 `(instance, seed)` 的 A2/A4 必须用同一个新建、独立保存的四向量初始种群快照。该快照只服务本协议，不覆盖 45 × 20 正式快照。两臂的 V35/P8 初群哈希、实例/SUT/疲劳参数 provenance、请求预算必须一致。

## 4. 单运行与配对接收门

每条运行在进入任何指标或参考前沿前必须同时满足：

```text
COMPLETED
0 < actualFE = decoderCalls <= 500000
0 <= remainingFE < 5000
utilizationRate > 0.99
front is non-empty and finite
illegalSolutions = duplicateEvaluations = unexplainedRepairs = missingSources = 0
```

同一 `(instance, seed)` 配对还必须满足：

```text
same instance, seed, snapshotSha256, V35/P8 initialPopulationHash and problem provenance
same frozen fields and only approved arm fields differ
abs(actualFE_A2 - actualFE_A4) < 5000
```

缺失、超预算、前沿非有限、哈希不一致或配对不完整均使该对 `INVALID`；不得补评价、复制结果或以另一条运行替代。

## 5. 指标与参考前沿

所有原始前沿及运行记录先验收，再在每个实例的 10 条有效运行全部完成后一次性冻结：

```text
PFref_confirm(instance) = ND(all A2 and A4 final raw fronts for the 5 seeds)
```

步骤是原始三目标精确去重、严格 Pareto 过滤、统一 ideal/nadir 归一化，并在归一化空间使用 HV reference point `(1.1,1.1,1.1)`。开发参考集、单实例先导参考集和确认参考集绝不混用。

每对定义“正数为 A4 改善”：

```text
deltaCmax = (Cmax_A2 - Cmax_A4) / Cmax_A2
deltaTEC  = (TEC_A2  - TEC_A4)  / TEC_A2
deltaTWC  = (TWC_A2  - TWC_A4)  / TWC_A2
deltaHV   = (HV_A4   - HV_A2)   / HV_A2
deltaIGD  = (IGD_A2  - IGD_A4)  / IGD_A2
```

完整 front 的 HV、IGD、IGD+、Spacing、双向 C-metric、三目标极值、front size、wall-clock 与 FE 利用率均报告；最终晋升只使用 Cmax、TEC、TWC、HV、IGD。Spacing、front size、C-metric 与时间仅作解释，C-metric 必须同时报告 `C(A4,A2)` 与 `C(A2,A4)`。

## 6. 预注册裁决

### A4 晋升为 Final 的必要条件

1. 30 个配对运行全部有效；
2. 全部 30 个配对的中位 `deltaHV > 0`、中位 `deltaIGD > 0`、中位 `deltaCmax >= -2%`；
3. 至少 4/6 个实例的五 seed 中位 `deltaHV >= 0` 且 `deltaIGD >= 0`；
4. 每个规模层级（两实例、10 个配对）的 pooled 中位 `deltaHV >= 0` 且 `deltaIGD >= 0`；
5. 两个 100-job 实例合并的 10 个配对中位 `deltaHV >= 0`、`deltaIGD >= 0`，且不存在单一 100-job 实例同时满足中位 `deltaHV < -5%`、`deltaIGD < -20%`；
6. 不得同时出现 TEC 在六个实例均中位退化超过 2%，且 TWC 在六个实例均中位退化超过 2%。

若所有条件成立：`A4_FINAL_CANDIDATE_CONFIRMED`。这不是论文优越性结论，只授权最终 freeze、生产 preflight 与吞吐 benchmark。

### 否决规则

若任一必要条件失败：`A4_NOT_PROMOTED`。不得救 A4、修改 Qp/PDDR/CA-TA 或在相同确认集上再试参数。A2 成为当前主候选，后续正式 roster、消融解释与外部对照范围必须另行裁决；不得自动恢复旧 A0--A4 的 4500 条矩阵。

## 7. 证据交付

证据根目录：`docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/`。

每次运行至少保存 configuration/provenance、snapshot 与初群哈希、budget-termination、前沿、状态、机制摘要、日志和文件级 SHA-256。汇总必须包括：

```text
preregistration.md
instance-seed-registry.csv
run-records.csv
pair-fairness-audit.csv
reference-fronts/
metrics.csv
paired-deltas.csv
instance-scale-summary.csv
promotion-gate.csv
FINAL_CANDIDATE_DECISION.md
evidence-sha256.tsv
```

任何临时失败、重试或不配对运行只登记，不进入 PFref、统计、绘图或裁决。

