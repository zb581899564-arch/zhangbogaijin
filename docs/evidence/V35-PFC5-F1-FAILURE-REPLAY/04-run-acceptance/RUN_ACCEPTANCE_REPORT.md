# V35-PFC5-F1 运行验收报告

runId：`V35PFC5F1-100_5_3_1-20260901-A4`
结论：**RUN_ACCEPTANCE = PASS（33/33）**，允许进入冻结契约的 HV/IGD 计算。

---

## 1. 运行终止与预算

| 项 | 值 |
|---|---|
| processExitCode | **0** |
| status | **COMPLETED** |
| mode | `V35_FULL_POOL_OFF` |
| stopReason | `BUDGET_OR_NORMAL_STOP` |
| requestedMaxFE | 500000 |
| actualFE | **500000** |
| decoderCalls | **500000** |
| remainingFE | **0** |
| qPhaseFE | 5000 |
| utilizationRate | **1.000000000000** |
| terminationKind | **EXACT_MAX_FE** |
| phaseBoundAccepted | true |
| phaseBoundFailure | NONE |
| formalOuterCycles | 62 |
| formalQgRounds | 3100 |
| algorithmRunNanos | 904308368069（≈15.07 分钟） |

接收门全部满足：`0 < actualFE = decoderCalls = 500000 <= 500000`，`0 <= remainingFE = 0 < 5000`，`utilizationRate = 1.0 > 0.99`。未补评价、未开 partial Q phase、未改 Q_Times、未改 population。

launcher 内置门输出 `formal-gate.properties`：`status=COMPLETED`、`failures=**NONE**`、`frontSize=387`、`launcherAcceptanceOnly=false`、`includedInFormalStatistics=false`、`includedInReferenceFront=false`。

---

## 2. 硬门逐项结果

完整表格见 `run-acceptance.csv`，共 **33 项，33 项 PASS，0 项 FAIL**。

### 2.1 运行合法性

| ID | 项 | 实测 | 判定 |
|---|---|---|---|
| G01 | processExitCode | 0 | PASS |
| G02 | status | COMPLETED | PASS |
| G03 | front 存在且非空 | 387 点 | PASS |
| G04 | front 三目标全部有限 | true | PASS |
| G05 | actualFE = decoderCalls | 500000 = 500000 | PASS |
| G06 | 0 < actualFE ≤ 500000 | 500000 | PASS |
| G07 | 0 ≤ remainingFE < 5000 | 0 | PASS |
| G08 | utilizationRate > 0.99 | 1.000000000000 | PASS |
| G09 | terminationKind | EXACT_MAX_FE | PASS |
| G10 | illegalSolutions | 0 | PASS |
| G11 | duplicateEvaluations | 0 | PASS |
| G12 | unexplainedRepairs（cfvfRepairs） | 0 | PASS |
| G13 | sourceMissing（passiveObserved == fullEvaluations，且门输出无 sourceObservationLoss） | observedCount=500000，failures=NONE | PASS |
| G14 | launcher 内置门 failures | NONE | PASS |

### 2.2 身份与溯源一致性

| ID | 项 | 实测 | 判定 |
|---|---|---|---|
| G15 | frozenJarSha256 | `8dad8f40…d8b9` | PASS |
| G16 | armProfileSha256 | `5b3cc542…79d1` | PASS |
| G17 | snapshotSha256 | `84d84523…3769` | PASS |
| G18 | initialPopulationHashV35 | `179a82a3…4c2d` | PASS |
| G19 | initialPopulationHashP8 | `7c6f8b42…2d3` | PASS |
| G20 | instanceSha256 | `2e88fa97…35cf` | PASS |
| G21 | setupConfigurationSha256 | `E7E9FF7F…58E1` | PASS |
| G22 | fatigueConfigurationSha256 | `81CAD959…67A1` | PASS |
| G23 | problemConfigurationSha256 | `892c7c3f…79f4` | PASS |

`initial-population.sha256` 独立复核：`V35 = 179a82a3…4c2d`、`P8 = 7c6f8b42…2d3`，与冻结期望一致。
`profile.sha256` 独立复核：`5b3cc542…79d1  profile.txt`。

### 2.3 语义配置

| ID | 项 | 实测 | 判定 |
|---|---|---|---|
| G24 | telemetry | 输出目录无 telemetry/checkpoint 文件，configuration.txt 无 telemetry 键 | PASS |
| G25 | ShiftMode | `shiftMode=NONE` | PASS |
| G25b | 运行时 shift 活动 | leftShiftNanos=0、rightShiftNanos=0、leftRecomputations=0、rightRecomputations=0 | PASS |
| G26 | PDDR | `pddrSelectionMode=GLOBAL_ORIGINAL` | PASS |
| G27 | seed | 20260901 | PASS |
| G28 | population | 100 | PASS |
| G29 | requestedMaxFE | 500000 | PASS |
| G30 | arm | A4 | PASS |
| G31 | runId | `V35PFC5F1-100_5_3_1-20260901-A4` | PASS |
| G32 | 新鲜运行且绑定快照 | 初群哈希等于冻结快照哈希（非重新生成） | PASS |

子群混合 `runtimeSubSwarmSizes=G1_CMAX=20;G4_BALANCED=40;G2_TEC=20;G3_TWC=20` = **20/40/20/20**，符合冻结配置。

---

## 3. 输出完整性

launcher 生成的 `evidence-sha256.tsv` 列出 21 个输出文件，本地反向复算：

```text
total=21 matched=21 missing=0 mismatch=0
```

远端原始数据**未删除**（按要求保留在 `/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829/output/A4`）。

---

## 4. 与历史 A4 500k 运行的确定性对照（记录性发现）

F1 的终态前沿与历史 A4 运行的终态前沿**逐字节相同**：

```text
f3755d83a2acb4280ff8dd566025340c8b64edc71050e05bbd6a3ff4b1239bdd  03-raw-run/remote/front.csv
f3755d83a2acb4280ff8dd566025340c8b64edc71050e05bbd6a3ff4b1239bdd  fetched-remote/.../A4/front.csv
```

机制计数亦逐项一致：`formalOuterCycles=62`、`formalQgRounds=3100`、`qgSelections=12400`、`qpActions=271800`、`qpTransitions=136000`、`cfvfOffspring=310000`、`archiveInsertions=6200`、`caTaLiteTest=11502`、`caTaLiteApply=3696`、`teacherUses=12400`、`validityChecks=24792`、`dominatedTeacherUses=0`、`replacements=238`、`shadowSamples=0`、`shadowEvaluations=0`、`p6EventStreamHash`、`pddrEventStreamHash`、`qgEventStreamHash`、`qpEventStreamHash`、`caTaEventStreamHash` 全部与历史运行相同。

唯一差异是墙钟/纳秒计时（本次 `algorithmRunNanos=904308368069`，历史 `989444694933`），属正常的机器计时差异，不影响搜索语义或终态结果。

**说明**：此对照仅作为确定性证据记录。**F1 的裁决基线固定为历史 A2，不是历史 A4**，本节不构成裁决依据。

---

## 5. 判定

```ini
RUN_ACCEPTANCE=PASS
gatesTotal=33
gatesPassed=33
gatesFailed=0
failedGates=NONE
F1_RUN_INVALID=未触发
下一步=按冻结reference contract计算终态HV/IGD
```
