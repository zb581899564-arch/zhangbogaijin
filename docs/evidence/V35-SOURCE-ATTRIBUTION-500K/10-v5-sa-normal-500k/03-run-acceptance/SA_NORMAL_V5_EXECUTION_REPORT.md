# SA-NORMAL V5 500k 执行报告（V35-SOURCE-ATTRIBUTION-500K / 10-v5-sa-normal-500k）

**性质：** 只读纵向来源观察（OBSERVATIONAL / DIAGNOSTIC_ONLY）。唯一一条 SA-NORMAL 500k + HARD–NORMAL 分析与裁决。不重跑 SA-HARD；不自动启动 A2。

## 1. 运行身份（冻结，2026-09-02 现场重算）

| 字段 | 值 |
|---|---|
| campaign / runKey | `V35-SOURCE-ATTRIBUTION-V5-SA-NORMAL-500K` / `SA-NORMAL-V5` |
| instance / seed / arm | `100_2_3_1` / `20260901` / `A4` |
| profile | `C0_BETA_MAX_065`（GLOBAL_ORIGINAL，betaMax 0.65，FM3，ShiftMode=NONE，单族，序列无关SUT） |
| observerJarSha256 | `1a73e3cf…72c8c9e` |
| formalJarSha256 | `8dad8f40…bad8b9`（未改，运行前后一致） |
| snapshotSha256 | `ea19f691…3a1842`（0-FE 物化，生成器同源性证明：同一生成器复现 HARD 快照逐字节一致） |
| initialPopulationHashV35 / P8 | `1fdf0820…` / `fa5f2a5c…`（运行期报告一致） |
| instance / setup / fatigue SHA | `4fb1fad5…` / `617d92be…` / `61c712f9…`（与正式 manifest 一致） |

## 2. 远程执行

| 字段 | 值 |
|---|---|
| host / 目录 | `aic-inspur-home`（`inspur-NP5570M5`，Linux 5.15.0，32 核）/ `/home/inspur/aicomp/zhangbo-v35-source-attribution-v5-sa-normal-500k-20260902`（全新目录） |
| JVM | OpenJDK 11.0.27；单 JVM；`nice -n 10`；`-Xms1g -Xmx4g`（**未扩堆**） |
| classpath 顺序 | `V5 观察器 Jar : 正式 Jar`（观察器在前） |
| 起止 | `2026-09-02T09:07:30+08:00` → `2026-09-02T09:18:17+08:00`（墙钟 10m47s） |
| processExitCode | `0` |
| attempt1（失败，0 FE） | 漏传 `bindings/100_2_3_1.binding.properties` → 秒退 exit=1（IllegalArgumentException，评价前）；日志归档 `02-remote-run/logs-attempt1/`；补齐后 attempt2 成功 |

## 3. 预算与终止

```ini
requestedMaxFE=500000  actualFE=500000  decoderCalls=500000
remainingFE=0  utilizationRate=1.000000  terminationKind=EXACT_MAX_FE
formalOuterCycles=62  cfvfOffspring=310000  qgSelections=12400  qpActions=271800
formalLocalFE=178733  caTaLiteFE=11167  totalLocalFE=189900  localFeShare=0.3798
```

机制指纹与冻结A4调度逐项一致（62外循环/310000 CFVF子代/12400 Qg/271800 Qp；HARD 同值——预算调度是实例无关的）。
`formalLocalFE=178733`（HARD=174702）与 `caTaLiteFE=11167`（HARD=15198）为实例相关值。

## 4. 验收（56/56 PASS）

| 类别 | 结果 |
|---|---|
| 进程与状态 | `exit=0`、`COMPLETED`、`failures=NONE` ✓ |
| 预算 | `0<actualFE(=decoderCalls)≤500000`、`remainingFE=0`、`utilizationRate>0.99`、`EXACT_MAX_FE` ✓ |
| 完整性 | `illegalSolutions=0`、`duplicateEvaluations=0`、`abnormalRepairs=0`、`sourceLoss=0`、`observerExecutionErrors=0`、`telemetryLedgerErrors=0` ✓ |
| 来源账本 | `sourceLedgerRows=500000=actualFE`；UNSET=0；三目标有限；nominal 窗口规则违背 0；`finalEvaluate` 列在位 ✓ |
| V5 schema | ledger 五列齐全；lifecycle 十类事件（2,488,377行）齐全 ✓ |
| B0 | 独立重算 5/5 逐点一致 ✓ |
| 检查点 | 19 配置 + terminal(500000) + B0(0)；overshoot=0 ✓ |
| snapshot 身份链 | staging 快照 SHA + 运行期 `initialPopulationHash` 一致 ✓ |
| Jar 不变 | 正式/观察器 Jar 运行前后 SHA 一致 ✓ |
| 证据反向复核 | 运行自身 `evidence-sha256.tsv` 67 项 0 缺 0 mismatch ✓ |

## 5. 来源一级分布（描述性，非根因）

| firstLevelSource | rows | 占比 |
|---|---:|---:|
| GLOBAL_CFVF | 310000 | 62.00% |
| INHERITED_LS | 178733 | 35.75% |
| CATA | 11167 | 2.23% |
| NOT_APPLICABLE (INITIAL_POPULATION→PARENT_CARRYOVER) | 100 | 0.02% |

## 6. 资源开销

| 项 | 值 |
|---|---|
| heapUsedPeak | 3,202,396,152 B（2.98 GB）< 4 GB，无 OOM |
| heapCommittedPeak | 4,214,226,944 B |
| front | 518 点，sha `bd4464f3…`（新前沿，无历史参照） |
| 结果目录 | 720,302,821 B（lifecycle 460MB + ledger 164MB + 其余） |

## 7. HARD–NORMAL 分析与裁决

见 `04-hard-normal-analysis/HARD_NORMAL_ANALYSIS_REPORT.md`、四份CSV与 `hard-normal-decision.properties`。

```ini
t_div=NOT_REACHED
G1_GLOBAL_CFVF=INSUFFICIENT  (WHVG deficit windows 1-2; ExND deficit windows 17-18; timing fails)
G3_CATA=NOT_TRIGGERED
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
SOURCE_LEVER_CANDIDATE=NONE
SA_A2_CONDITIONAL_ELIGIBLE=false
SA_A2_CONDITIONAL_STARTED=false
```

## 8. 停止边界（保持）

```ini
SA_A2_CONDITIONAL_STARTED=false
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
DOE_AUTHORIZED=false
QP_V2_AUTHORIZED=false
CONFIG_RACE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false
formalMatrixRunning=false
PDDRChanged=false; CFVFChanged=false; DualQChanged=false; CaTaChanged=false; formalJarChanged=false
```

Phase A 结束（G4出口）。Phase B 及任何修复/DOE/Validation/Final/正式矩阵须新的明确授权。
