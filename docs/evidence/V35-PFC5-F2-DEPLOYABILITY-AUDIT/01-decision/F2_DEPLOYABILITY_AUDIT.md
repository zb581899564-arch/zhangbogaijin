# V35-PFC5-F2 可部署性审计（0-FE）

审计日期：2026-08-29
性质：**纯 0-FE 审计**。未运行任何算法、未消耗任何 FE、未修改任何算法或遥测代码、未上传任何文件。
审计对象：已封存的诊断 runtime `121FBB49…` 能否支撑 F2（同实例 / 同 seed / 同快照 / A4 / 500k / telemetry ON）。

**结论：`F2=NOT_DEPLOYABLE_FIELDS_INSUFFICIENT`** —— CFVF 字段缺失，触发用户预置的判据 4：不继续反复改遥测工具，将 FC5 记为机制未解析，转入假设驱动的 Teacher Exposure Calibration。

---

## 1. 被审计实体的身份

| 角色 | SHA-256 | 字节 | 位置 |
|---|---|---|---|
| 正式算法 Jar（F1 所用） | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` | 48269638 | `26-final-runtime-jar-validation/formal-algorithm-8DAD8F40.jar`、`V35-FC5-MIDHORIZON-250K/00-preregistration/runtime/` |
| 诊断 base Jar | `723d24ed3021a01facda0231e3b142238e740fb18d025a4341748f2af8d22e2f` | 4091944 | `15-final-pddr-provenance/build/` |
| 诊断 runtime Jar | `121fbb4939258bdc94c297d5f6ce9be0b0bee0271a6e71b89bae8e1486394155` | 47776437 | `26-final-runtime-jar-validation/`、`V35-FC5-MIDHORIZON-250K/00-preregistration/runtime/`（两处 SHA 一致，本次实测复核通过） |

### 判据 2：F2 使用诊断衍生 Jar，正式算法身份仍绑定 `8DAD8F40…`

这一点**成立且必须显式声明**，事实如下：

- F2 若执行，运行时实体是诊断 runtime `121FBB49…`（由诊断 base `723D24ED…` 派生），**不是**正式 Jar `8DAD8F40…`。
- 但 `runtime-provenance.properties` 同时记录 `formalAlgorithmJarSha256=8DAD8F40…` 与 `diagnosticRuntimeJarSha256=121FBB49…`，且 `runtimeJarBindingVerified=true`：诊断 runtime 在启动时把自身 SHA 传入 driver，输出的 `sourceJarSha256` 与之自洽。
- 封板令与 `A4_50K_121_ON_OFF_EQUIVALENCE.csv` 明确 `algorithmDecisionSemanticsChanged=false`、`formalFrozenJarChanged=false`、`pddrDecisionChanged=false`、`formalMatrixRunning=false`。
- 因此**算法决策语义的身份锚点始终是 `8DAD8F40…`**；`121FBB49…` 只是承载观察能力的**部署实体**，其合法性来自 50k ON/OFF 等价性验证（`onOffBehaviorEquivalent=true`）。

**但必须同时记录一项反向事实**：`diagnosticSourceChanged=true`。诊断 runtime 的**源码相对正式 Jar 是改动过的**（这正是它能输出遥测的原因）。所以"正式算法身份仍绑定 8DAD8F40"是**语义层面的绑定 + 50k 等价性支撑的推断**，不是"同一个 Jar 跑了两种模式"。F2 的 ON/OFF 对比因此**只能是行为等价性断言，不能是同一实体的自我对照**。

---

## 2. 判据 1：五个字段域的覆盖情况

明细见 `00-field-coverage/field-coverage-matrix.csv` 与 `telemetry-schema-inventory.csv`。

| 域 | 判定 | 载体 |
|---|---|---|
| **Teacher** | **SUFFICIENT** | `teacher-use-events`（43 列 / 26,300 行 @50k：teacherSource、cacheType、qSystem、qState、qAction、scope、directionalRegret、teacherFingerprint、offspringFingerprint、offspringImproved…）+ `teacher-concentration`（top1Share、top5Share、shannonEntropy、normalizedEntropy、exposuresPerCycle）。`teacherContractPass=true`、`teacherOutcomeLifecycleValidated=true` |
| **PDDR** | **SUFFICIENT** | `pddr-full-ledger`（37 列 / 1,553 行：source、parentSlot、lineageId、physicalSlotBefore、semanticRoleBefore/After、pddrScore、selectedRank、cutoffScore、cutoffMargin、rejectionReason、四个 isXxxRepresentative 标志）+ `pddr-cycle-summary`。`pddrContractPass=true`、`pddrPhysicalLifecycleValidated=true` |
| **CA-TA** | **PARTIAL** | `cata-contribution-events`（41 列 / 1,394 行，覆盖 TEST_APPLY → generated → evaluated → acceptedLocally → enteredMergePool → selectedByPddr → 个人/全局档案 → survivedNextGeneration → teacherUsedLater → improvedOffspringLater 全链路）+ `cata-contribution-summary`。**但契约永久声明 `cataFullLifecycleValidated=false` 与 `cataAllShortGateSourceCoverageValidated=false`，且封板令第 6 条禁止归一化为 true** |
| **checkpoint** | **SUFFICIENT（仅 50k 验证）** | `checkpoint-fronts`（24 列 / 2,013 行：nominalCheckpointFE、actualSnapshotFE、overshootFE、formalOuterCycle、qRound、frontType、atomicBoundary、checkpointKind、checkpointDeltaFE、frontSource、terminationKind）。`checkpointComplete=true`、`terminalCheckpointProtocol=PASSED`、`terminalCheckpointClassification=ACCEPTED` |
| **CFVF** | **INSUFFICIENT（阻断）** | **无。** 见下节 |

### 2.1 CFVF 缺失的实证

1. 冻结的 7 类遥测 schema 中**不存在任何 CFVF 类文件**（既无 `telemetry-cfvf-*.csv`，也无其它等价文件）。
2. 对 ON 运行输出目录全量 `grep -ri cfvf`，**仅命中两处同一标量**：
   `generatedCandidateSourceCounts=INITIAL_POPULATION=100,GLOBAL_CFVF=30000,CATA_TEST=1015,CATA_APPLY=379,INTER_FACTORY_LS=132,INTRA_FACTORY_VNS=16643`
3. `cfvfRepairs` / `cfvfOffspring` / `baselineUpdateEvents` 三个键在诊断 runtime 输出中 **grep -c 均为 0**。
4. `telemetry-pddr-full-ledger.csv` 的 `source` 分布为 `GLOBAL_OFFSPRING=600 / PARENT=600 / CATA_TEST=148 / CATA_APPLY=139 / O1_O9=65 / CRITICAL_INSERT=1`，无 CFVF 来源标记；且 30,000 个 CFVF 候选中仅 600 条出现在 PDDR 台账（PDDR 周期级切片，非全量事件）。
5. `telemetry-cata-contribution-events.csv` 的 `context` 只有 `G1_CMAX/G2_TEC/G3_TWC/G4_BALANCED`（子群），`bottleneck` 恒为 `BAL`，均不区分 CFVF。

### 2.2 诊断 runtime 的 CFVF 能力边界

**诊断 runtime 缺少 CFVF 事件级遥测，仅保留有限聚合计数。**

| 计数 | 正式 `8DAD8F40…` 的 OFF 运行（F1，500k） | 诊断 `121FBB49…` 的 ON 运行（50k） |
|---|---|---|
| `cfvfOffspring` | **310000** | **不产出** |
| `cfvfRepairs` | **0** | **不产出** |
| `baselineUpdateEvents` | **0** | **不产出** |

两者是**不同部署实体**，提供的是不同的观测面，不是同一实体的 ON/OFF 对照，因此不适用"某一方丢失了另一方已有的东西"这一表述：

- 正式 `8DAD8F40…` 的 OFF 运行通过 `status.properties` 的 `mechanismSummary` 提供 CFVF 的**聚合计数**，但没有事件级明细。
- 诊断 `121FBB49…` 的 ON 运行提供 Teacher / PDDR / CA-TA / checkpoint 四域的**事件级遥测**，但不提供 CFVF 事件级遥测，也不产出上表三个 CFVF 聚合计数。

净结果：**CFVF 在事件级完全不可观测**；聚合级仅在正式 Jar 侧可得，且不含修复/替换的过程信息。

CFVF 在 500k 下产生 **310,000 个后代，占全部 500,000 次评价的 62%**，是评价预算的最大单一消耗者，且按 §13.2 冻结边界是 `MANDATORY_FINAL_COMPONENT`。F2 若执行，将在一个**对最大组件只有聚合计数、没有事件级可观测量**的状态下观察机制。这是诊断工具的能力边界，不构成对正式算法本身的否定，也不构成任何组件已被排除或已被证成的结论。

---

## 3. 除字段缺失外的三项独立风险（不支持 F2 直接部署）

即使 CFVF 问题被解决，以下三项仍需在预登记阶段处理：

1. **等价性仅在 50k 验证，500k 是 10 倍外推。**
   ON/OFF 等价性证据为 `actualFE=48269 / maxFEs=50000 / remainingFE=1731`。
   而 `250kStarted=false`、`250kApproved=false` —— 曾被宣告 `250kReadyForPreregistration=true` 的 250k 诊断 campaign **从未执行**。500k 没有任何中间尺度的等价性支撑。

2. **诊断驱动器不绑定快照文件。**
   其命令行为 `V35MidHorizonDiagnosticDriver A4 100_5_3_1.txt 20260901 50000 ON <runId> <jarSha> <outputDir>`，**没有 snapshot 参数**。
   它靠重新生成初群，而初群哈希恰与冻结快照相同（`initialPopulationHash=179a82a3…`，与 F1 及冻结 snapshot 的 `179a82a3…4c2d` 一致）。
   这是**哈希等价**，但 provenance 路径弱于 F1 的 `readSnapshot(...)` 精确读取。F2 合同必须显式声明这一点，不得表述为"同快照"。

3. **遥测体积：500k 预估约 0.9 GB**（teacher-use-events 约 795 MB、cata 约 44 MB、pddr-ledger 约 27 MB、checkpoint 约 10 MB）。训练机 249 GB 可用，不构成阻断，但下载与归档需按此规划。

---

## 4. 判据 3 与判据 4 的适用

- **判据 3（字段足够 → 冻结 F2 合同后再申请运行）：不适用。** CFVF 不足。
- **判据 4（字段不足 → 不继续反复改遥测工具 → FC5 记为机制未解析 → 转入假设驱动的 Teacher Exposure Calibration）：适用。**

封板令第 1 条已禁止增加观察字段（"遥测 schema 冻结为 121 runtime 所产出的 7 类 telemetry CSV 字段集"），第 4 条的升级通道要求"停止运行并重新申请诊断工具版本升级（新预登记 + 用户批准），不得在运行中临时改工具或事后归一化"。

**按用户判据 4 的决定，本次不走升级通道，不再迭代遥测工具。**

---

## 5. 裁定

```ini
F2_DEPLOYABILITY_AUDIT=COMPLETED
F2_FIELD_COVERAGE=INSUFFICIENT
blockingField=CFVF
F2=NOT_DEPLOYABLE_FIELDS_INSUFFICIENT
f2Preregistered=false
f2Started=false
FC5=MECHANISM_UNRESOLVED
nextRoute=HYPOTHESIS_DRIVEN_TEACHER_EXPOSURE_CALIBRATION
calStarted=false
telemetryToolIteration=FORBIDDEN_BY_USER_DECISION
consumedFE=0
changedAlgorithm=false
changedTelemetryTool=false
```

下一步路线建议见同目录 `PFC5-CAL_ROUTE_PROPOSAL.md`。**该建议不构成预登记，也不构成启动。**
