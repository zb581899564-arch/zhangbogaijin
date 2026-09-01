# PFC5-2 诊断工具身份审计报告（Step 0 复用裁决）

- 生成时间：2026-08-29
- 生成工具：`docs/evidence/V35-PFC5-PHASE0/tools/build_diagnostic_audit.py`
- 审计对象：`V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation/` 的
  121 runtime 最终复验对（A4 / 100_5_3_1 / seed 20260901 / 50k / OFF+ON）及
  `25-v31-final-decision/` 判定文书
- 输出：`diagnostic-artifact-registry.csv`、`step0-contract-comparison.csv`、
  `diagnostic-freeze.properties`、`DIAGNOSTIC_TOOLING_FREEZE.md`
- 消耗FE：0（`newStep0Runs=0`）；改变算法：否；重跑：无

## 裁决

```ini
STEP0=SATISFIED_AFTER_OFFLINE_RECONSTRUCTION
newStep0Runs=0
diagnosticToolingValidated=true
diagnosticToolingFrozen=true
```

依据工作包 §十：现有证据缺少的只是**非关键报告字段**（working population 独立哈希键、
sorted-front 独立哈希键），原始数据足以离线重建；无任何影响身份或行为等价判断的
关键字段缺失，故不判 BLOCKED、不补跑。

## 1. Jar 身份（全部实体重算，无一凭登记值采信）

| 角色 | 登记 SHA（前16位） | 实测副本 | 结果 |
|---|---|---|---|
| formal `8dad8f40…d8b9` | 8dad8f40266feeaa | 26/ 副本、250K/00-preregistration/runtime/ 副本、隔离冻结目录 jmetal-exec target（三处一致） | MATCH ×3 |
| diagnostic runtime `121fbb49…4155` | 121fbb4939258bdc | 26/ 副本、250K/00-preregistration/runtime/ 副本 | MATCH ×2 |
| diagnostic base `723d24ed…2e2f` | 723d24ed3021a01f | 15-final-pddr-provenance/build/ thin jar（全项目唯一实体） | MATCH ×1 |

**工作树漂移（登记为发现，不阻断）**：当前 `java-jmetal58/jmetal-algorithm/target/…jar`
实测 = `a0a1e74d…`（即 250k 诊断所用重建版），`jmetal-exec/target/…jar` = `e5969803…`，
均不等于三个登记身份。封板后工作树被再次构建；**权威实体以封存副本为准**，
本项目内不得再无登记重建。`diagnostic-artifact-registry.csv` 将两处工作树条目标记为
`DRIFT_EXPECTED`。

## 2. OFF/ON 行为等价（独立复核，不信赖历史等价表）

对 29 个核心行为字段（FE/decoderCalls/相位/非法解/初群/评价轨迹/双Q表/双Q事件流/
PDDR 事件流/canonical front/结构计数/停机语义/terminal 门/cata 四项/候选来源计数）
逐一从两侧 `behavior-summary.properties` 原始键值重比：

```text
核心域全部 EQUAL（generatedCandidateSourceCounts 按设计 OFF 侧不适用）
canonical-front.csv OFF/ON 实测 SHA 一致（ae6e28af…3204，14507 字节 ×263 行）
结论：onOffBehaviorEquivalent=true 独立成立
```

与历史 `A4_50K_121_ON_OFF_EQUIVALENCE.csv`（20 行全 equal）交叉一致。

## 3. Step 0 合同逐项对照（`step0-contract-comparison.csv`）

```text
actualFE=48269 = decoderCalls = lastCompletedAtomicBoundaryFE；remainingFE=1731 (<5000)
requestedMaxFE=50000；terminalCheckpointKind=PHASE_CONSISTENT_TERMINAL；ACCEPTED
真实 RNG 消费哈希（ON）D5EE9E7D…，audit count 2,149,377
候选序列哈希（ON）584423BF…，48,269 候选，来源计数闭合
  （100+30000+1015+379+132+16643=48269，candidateCountClosed=true）
Qg/Qp 表哈希 + 事件流哈希一致；Q 动作逐事件 trace 在 ON 侧 teacher-use-events（26,300 行）
CFVF：GLOBAL_CFVF=30000 来源计数（无独立 CFVF 事件流哈希——登记为已知口径）
PDDR：事件流哈希 + 1,553 行全 ledger + contract 全 true
CA-TA：Test/Apply 计数 + 1,394 行事件；summary 闭包 INCOMPLETE（右删失，登记在案）
种群/前沿：initialPopulationHash、canonicalFrontHash、三类终端前沿签名
观察者：observerErrors=0；unobservableCheckpointCount=0
```

## 4. 离线重建（本阶段新增，不改任何历史文件）

```text
workingPopulation（终止时刻 72 解）：自 ON 侧 telemetry-checkpoint-fronts.csv 的
  PHASE_CONSISTENT_TERMINAL × workingPopulation 行重建，
  canonical SHA-256 = 8a9fd41902b225f1eb18d644f2dc2b0f7ba809cb132590b75a36fe837760e43c
  （行数 72 与 TERMINAL_FRONT_VALIDATION_121.csv 的 workingPopulationND=72 一致）
sorted-front.csv 实测 SHA = 与 canonical 同值（ae6e28af…，两文件逐字节相同）
```

## 5. 计时域隔离论证（主计划 §7 要求）

- `wallNanos` 只出现在 behavior-summary，不参与任何行为哈希（11-real-overhead 证据链
  明文声明；本轮 OFF 27.10s / ON 31.42s，+15.9%——超过 20k 标定的 15% 开销门，
  因 wallNanos 不进行为域故不影响等价，登记为口径备注）。
- CA-TA credit 以**调用/FE 计数**计量（caTaTestCalls=1015、CATA_TEST/CATA_APPLY
  来源计数），证据中不存在任何"墙钟进入 credit"的键值或代码路径记录。
- 决定性证明：若遥测开销影响了任何行为域，OFF/ON 的 RNG/候选/双Q/PDDR/前沿哈希
  不可能逐位一致——实测全部一致。

## 6. 诚实保留与限制

```ini
cataFullLifecycleValidated=false            （原始输出，禁止事后归一化为 true）
cataAllShortGateSourceCoverageValidated=false
FC5=INCONCLUSIVE（历史口径）
```

- CFVF 无独立事件流哈希（以来源计数+事件上下文字段为证）——登记为口径限制；
  若 F2 分析需要更细 CFVF 轨迹，应走"诊断工具版本升级申请"，禁止运行中临时改工具。
- 250k 诊断实际使用其后继重建 A0A1E74D runtime jar；本封板绑定 121FBB49（被 50k
  OFF/ON 复验者）。两者源差异未审计——若 F2 选择改用 A0A1E74D，须先补源级 diff
  审计并重新申请。
