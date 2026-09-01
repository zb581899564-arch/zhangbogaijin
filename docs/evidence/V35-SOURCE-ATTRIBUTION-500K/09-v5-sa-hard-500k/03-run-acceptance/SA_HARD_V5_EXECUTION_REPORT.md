# SA-HARD V5 500k — 执行报告（V35-SOURCE-ATTRIBUTION-500K / 09-v5-sa-hard-500k）

**性质：** 只读纵向来源观察（OBSERVATIONAL / DIAGNOSTIC_ONLY）。唯一一条获准运行，不启动 SA-NORMAL。
**Observer：** V5（schema `v35-source-attribution-observer-schema-v2`），正式算法 Jar 未重建。

---

## 1. 运行身份（冻结，全部现场重算 SHA-256）

| 字段 | 值 |
|---|---|
| campaign | `V35-SOURCE-ATTRIBUTION-V5-SA-HARD-500K` |
| runKey | `SA-HARD-V5` |
| instance / seed / arm | `100_5_3_1` / `20260901` / `A4` |
| profile | `C0_BETA_MAX_065`（`pddrSelectionMode=GLOBAL_ORIGINAL`，betaMin 0.25 / betaMax 0.65，FM3，ShiftMode=NONE，单族，序列无关 SUT） |
| population / MaxFEs | `100` / `500000` |
| mixture（运行期） | `G1_CMAX=20;G4_BALANCED=40;G2_TEC=20;G3_TWC=20` |
| observerJarSha256 | `1a73e3cf025f7cfdb47bde38a7b34e8f8b0810958f61323a5d3cbc35272c8c9e` |
| formalJarSha256 | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` |
| snapshotSha256 | `84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769` |
| instance / setup / fatigue SHA | `2e88fa97…` / `4b49b780…` / `cf611bfb…` |
| initialPopulationHash(V35 / P8) | `179a82a3…` / `7c6f8b42…`（运行期 `initialPopulationHash` 逐位一致） |
| problemConfigurationSha256 | `892c7c3f…` |

Jar 身份在运行前后未变（`logs/launch-env.properties` 与 `logs/run-closeout.properties` 双点记录）。

## 2. 远程执行

| 字段 | 值 |
|---|---|
| host / 目录 | `aic-inspur-home`（`inspur-NP5570M5`，Linux 5.15.0，32 核）/ `/home/inspur/aicomp/zhangbo-v35-source-attribution-v5-sa-hard-500k-20260901`（全新目录，上传前 `DIR_NOT_EXISTS`） |
| JVM | OpenJDK 11.0.27；单 JVM；`nice -n 10`；`-Xms1g -Xmx4g`（**未扩堆**） |
| classpath 顺序 | `V5 观察器 Jar : 正式 Jar`（观察器在前） |
| 起止 | `2026-09-01T21:39:15+08:00` → `2026-09-01T21:54:26+08:00`（墙钟 15m11s；`algorithmRunNanos=905,529,224,793`） |
| processExitCode | `0` |
| 并发环境 | 上传前 `pgrep java` 无运行进程；Stage2 4500 矩阵暂停标记 `PAUSED_BY_USER.properties` 在位；空闲磁盘 246G |

## 3. 预算与终止（PHASE_CONSISTENT_BUDGET_TERMINATION）

```ini
requestedMaxFE=500000
actualFE=500000
decoderCalls=500000
remainingFE=0                 # = 500000-actualFE，∈ [0,5000)
utilizationRate=1.000000
terminationKind=EXACT_MAX_FE
qPhaseFE=5000
formalOuterCycles=62  formalQgRounds=3100  cfvfOffspring=310000
globalPhaseFE=310100  formalLocalFE=174702  caTaLiteFE=15198  totalLocalFE=189900
localFeShare=0.3798
qgSelections=12400  qpActions=271800
```

机制指纹与冻结 F1(A4) 逐项一致：`pddrEventStreamHash=d698245e…`、`qgTableHash=F0E6D62B…`、`qpTableHash=9328966A…`、`caTaLiteTest=11502`、`formalOuterCycles=62`。
DSCR：`teacherUses=12400`、`dominatedTeacherUses=0`、`DTUR=0.000000`。

## 4. 运行验收（`03-run-acceptance/run-acceptance.csv`，61/61 PASS）

| 类别 | 结果 |
|---|---|
| 进程与状态 | `processExitCode=0`、`status=COMPLETED`、`failures=NONE` ✓ |
| 预算 | `0 < actualFE(=decoderCalls) ≤ 500000`、`remainingFE∈[0,5000)`、`utilizationRate>0.99`、`EXACT_MAX_FE` ✓ |
| 完整性 | `illegalSolutions=0`、`duplicateEvaluations=0`、`abnormalRepairs(cfvfRepairs)=0`、`sourceLoss=0`、`observerExecutionErrors=0`、`telemetryLedgerErrors=0` ✓ |
| 来源账本 | `sourceLedgerRows=500000=actualFE`；UNSET 来源行 `0`；三目标全部有限；`nominalFE=25000×ceil(actualFE/25000)` 违背 `0`；`finalEvaluate` 列在位；一级来源四类分布见 §5 ✓ |
| Observer V5 schema | source-ledger 含 `actualFE,nominalFE,generation,outerCycle,qRound` ✓；lifecycle 十类事件齐全（§6）✓ |
| 检查点 | 19 个配置检查点（25k…475k）+ terminal(500000) + B0(0)；`maxOvershootFE=0` < 一个 5000 FE 原子 phase ✓；全部 decision front 可读、目标有限 ✓ |
| B0 | 独立从 ledger 前 100 条评价重算严格三目标 ND = 11 点，与导出 `checkpoint-0-decision-front.csv` **逐点一致** ✓ |
| 前沿 | 387 点、三目标有限、`frontSha256Raw=f3755d83…1239bdd` ✓ |
| Jar 不变 | 正式 Jar、观察器 Jar 运行前后 SHA 一致 ✓ |
| 证据反向复核 | 运行自身 `evidence-sha256.tsv` 67 项：matched=67、missing=0、mismatch=0 ✓ |

## 5. 来源账本一级分布（描述性预算占比，非根因）

| firstLevelSource | rows | 占比 |
|---|---:|---:|
| GLOBAL_CFVF | 310000 | 62.00% |
| INHERITED_LS | 174702 | 34.94% |
| CATA | 15198 | 3.04% |
| NOT_APPLICABLE（INITIAL_POPULATION → PARENT_CARRYOVER） | 100 | 0.02% |
| 合计 | 500000 | 100% |

> **禁写提示**：该分布与 V4 运行逐值相同，只是**描述性预算占比**。任务书与 Phase A0 合同明确禁止把"CFVF 占 62% 评价量"直接解释为 CFVF 根因。

## 6. 生命周期账本（V5 新增真实事件账本）

`source-lifecycle-events.csv` = 2,430,744 行，来源列无 UNSET：

| eventType | rows |
|---|---:|
| GENERATED | 500000 |
| QP_ACTION | 543600 |
| QP_TEACHER | 543600 |
| DESCENDANT | 493233 |
| PERSONAL_ARCHIVE | 235922 |
| IMPROVING_DESCENDANT | 73874 |
| MERGE_POOL | 15715 |
| QG_TEACHER | 12400 |
| PDDR_SELECTED | 6200 |
| WORKING_POPULATION | 6200 |

十类事件与 Phase A0 合同要求逐项对应（`GENERATED / DESCENDANT / IMPROVING_DESCENDANT / MERGE_POOL / PDDR_SELECTED / WORKING_POPULATION / PERSONAL_ARCHIVE / QG_TEACHER / QP_TEACHER / QP_ACTION`）。
事件 FE 只是观察时间戳（选择类事件携带最近一次成功解码 FE），不构成新增评价。

## 7. 检查点

| 项 | 值 |
|---|---|
| 配置检查点 | 19 个（25000…475000，步长 25000），`overshootFE=0` |
| terminal | `checkpointTargetFE=500000`，`checkpointObservedFE=500000`（terminal decision front = 顶层 `front.csv`，387 点；与 20k 门同一约定） |
| B0 | `checkpointTargetFE=0`，`b0-decision-front` / `b0-observed-full-front` 各 1 条（11 点） |
| 口径 | 19 + 1 = **20 个非 B0 快照**，另有 B0；terminal **不**重复计为第 21 个窗口 |
| 数据完整性 | 全部 decision front 可读、目标有限；`checkpointsUnreadable=0`、`checkpointNonFiniteObjectives=0` |

## 8. 资源开销（实测）

| 项 | 值 |
|---|---|
| heapUsedPeak | 3,566,581,248 B（3.32 GiB） |
| heapCommittedPeak | 4,214,226,944 B（< 4 GB，无 OOM） |
| GC | 1214 次 / 24.604 s |
| 控制台日志 | `logs/SA-HARD-V5.log` 169 B |
| 结果目录 | 707,790,390 B（其中 `source-lifecycle-events.csv` 447,875,375 B、`source-ledger.csv` 164,588,161 B） |

**透明偏差（不推翻验收）**：20k OFF/ON 的分解模型预测 `estimated500kPeak` 比值 0.3221（≈1.29 GB），本次实际峰值 3.57 GB，为预测值的约 2.8 倍——与 V4 500k 同类"模型外推精度"偏差一致（V4 为 2.92 倍）。堆上限按任务书固定为 4 GB 且**未扩堆**，运行正常 `COMPLETED`、exit 0，观察者缓存有界（`ledgerRows=actualFE`、`observerExecutionErrors=0`、无丢弃事件）。该偏差登记为模型外推精度问题，不作为运行失败项。

## 9. 失败类复现与来源分析

- 失败类复现：`04-failure-reproduction/failure-class-reproduction.csv` 与 `frozen-reference-analysis.properties`（gold 自检 1e-12 通过，`verdict=SA_HARD_FAILURE_CLASS_REPRODUCED`）。
- HARD 侧窗口来源证据：`05-hard-source-analysis/`（`HARD_SOURCE_ANALYSIS_REPORT.md`、`source-window-metrics.csv`、`source-lifecycle-summary.csv`、`direction-extreme-contributions.csv`）。
- 裁决与停止边界：`07-decision/SA_HARD_V5_DECISION.md` / `SA_HARD_V5_DECISION.properties`。

## 10. 停止边界（本包结束后保持）

```ini
SA_NORMAL_STARTED=false
SA_A2_CONDITIONAL_STARTED=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
formalJarChanged=false
```
