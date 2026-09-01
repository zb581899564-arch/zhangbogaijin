# PREREGISTRATION — V35 SOURCE-ATTRIBUTION-500K Phase A0（0-FE预登记与证据冻结）

- 冻结日期：2026-08-31
- 上游授权：`docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` v1.0（三人共识冻结执行方案，SHA `eac6a458…2606a`）；`PHASE_A_AUTHORIZED_ONLY=true`；`algorithmOptimizationClosed=true`（500k纵向诊断为例外，不重开算法优化）。
- 本阶段性质：**0-FE 预登记**。不实现 Observer、不上传训练机、不消耗任何 FE。产出裁决只能是 `PHASE_A0_PREREGISTRATION_PASSED` 或 `DO_NOT_RUN`。
- 分工与独立性：Agent A（NORMAL解析/reference绑定）、Agent B（阈值/指标合同）、Agent C（Observer静态合同/wall-clock审计）文件所有权互斥；主Agent独立复核全部关键声明（抽查记录见 §7）并裁决跨文件冲突（§6）。

## 1. NORMAL Control 解析（Agent A，主Agent复核通过）

解析输入（真实登记结构）：`docs/evidence/V35-PFC5-PHASE0/02-instance-role-registry/instance-exposure-role-registry.csv`（49实例，sha256 `94f5a38f…`）＋ `docs/PAPER_EVIDENCE_MASTER/run-ledger.csv`（480条，含500k COMPLETED与referenceEligible列）＋ Stage2/V35A2A4 验收材料。

**全部9个100-job候选与逐项淘汰**（详见 `normal-control-resolution.csv`）：

| 实例 | 角色 | 裁决 |
|---|---|---|
| **100_2_3_1** | CONTAMINATED_DEVELOPMENT | **SELECTED**：12条accepted A4 500k（Stage2 seeds 20260808–19，referenceEligible）；A0→A4 ΔHV+25.24%/ΔIGD+19.02%；无Current-A4 veto；raw fronts可绑定；seed 20260901未消耗 |
| 100_2_4_1 | CONTAMINATED_DEVELOPMENT | 并列存活→字典序淘汰（raw fronts不在本地冷归档逐run可绑定态；其seed 20260901已消耗） |
| 100_2_5_1 / 100_8_3_1 | CONTAMINATED_DEVELOPMENT | 淘汰：无accepted A4 500k |
| 100_5_4_1 | CONTAMINATED_DEVELOPMENT | 淘汰：仅DOE1-heldout非A4臂 |
| 100_5_3_1 | CASE_SELECTED_DIAGNOSTIC_ONLY | 淘汰：保留角色＋failure veto＝HARD病例 |
| 100_5_5_1 / 100_8_4_1 / 100_8_5_1 | VALIDATION_RESERVED | 淘汰：保留角色 |

**角色语义裁决（主Agent）**：registry权威角色类 `CONTAMINATED_DEVELOPMENT` 即 §3.1 `role=DEVELOPMENT` 的开发类（registry原文"eligible for development/Race, never validation"）；三类保留角色全部排除。

```ini
NORMAL = 100_2_3_1
normalControlResolved = true
normalSeed = 20260901（与HARD一致，该实例未消耗）
normalArm = A4
normalMaxFEs = 500000
normalReferenceBackfillPolicy = FORBIDDEN
```

## 2. HARD病例绑定（Agent A，全部机器计算，主Agent抽查复核）

```ini
instance = 100_5_3_1
instanceSha256 = 2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf
setupFileSha256 = 4b49b780…
fatigueFileSha256 = cf611bfb…
snapshotSha256 = 84d84523…（docs/evidence/V35-PFC5-PHASE0/fetched-remote/snapshots/100_5_3_1/seed-20260901.fourvec，全库唯一物理副本=Failure Replay所用）
problemConfigurationSha256 = 892c7c3f…（快照头）
initialPopulationHashV35 = 179a82a3… / P8 = 7c6f8b42…
profileCanonicalSha256 = 5b3cc542…（来源run=V35PFC5F1-100_5_3_1-20260901-A4，RUN_ACCEPTANCE=PASS 33/33，FAILURE_CLASS_REPRODUCED）
frozenJarSha256 = 8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
role = CASE_SELECTED_DIAGNOSTIC_ONLY（永久禁止DOE/Race/Validation/Formal/Final）
seed = 20260901
MaxFEs = 500000
```

## 3. Reference合同（不重建，只绑定）

- **HARD**：既有Failure Replay Reference Contract（`V35-PFC5-PHASE0/04-reference-contract/reference-contract.properties`，sha `ecdc5589…`=F1冻结登记值；PFref `pfref-100_5_3_1.csv` 757点 sha `4dc85dd4…`；ideal/nadir、HV/IGD实现 `analyze_confirmation.py` sha `13692c03…`）。GAP-PROBE同名pfref（146点）已登记为另一合同、未绑定。
- **NORMAL（100_2_3_1）**：12条accepted A4 500k raw fronts位于冷归档 `G:/ResearchArchive/ZhangBo-V35-Paper-Evidence-20260823/remote-campaigns/zhangbo-v35-stage2-master-v2-20260823.tar.gz`（整包sha `0202356f…`），逐条流式哈希 **12/12 与 run-ledger frontHash 逐位一致**；另绑定本地聚合 `reference-front.csv`（1979点）。`normalReferenceBackfillPolicy=FORBIDDEN`（新诊断run不得回灌）。
- 两者ideal/nadir、HV/IGD实现相互独立（分属各自合同），Phase A分析时各自使用各自reference——不得交叉。

## 4. 来源分类冻结（Agent C，四类，运行后不得增加第五类）

| 一级 | 评估侧枚举 | 选择器侧 | 备注 |
|---|---|---|---|
| GLOBAL_CFVF | GLOBAL_CFVF（L682批/L4759单）＋**FINAL_EVALUATE（L788，并入+二级finalEvaluate=true）** | GLOBAL_OFFSPRING | V3实测FINAL_EVALUATE=0行为护栏 |
| CATA | CATA_TEST/CATA_APPLY（L5103-5105/L4895-4897） | CATA_TEST/CATA_APPLY | |
| INHERITED_LS | INTER_FACTORY_LS（L5358/5400）＋INTRA_FACTORY_VNS（L5462/5278） | CRITICAL_SWAP/CRITICAL_INSERT/O1_O9 | |
| PARENT_CARRYOVER | INITIAL_POPULATION（L606）＋PDDR parent行（N_eval=0） | PARENT | 生存/利用层 |

SHADOW路径（零FE）显式排除。二级字段（subSwarmRole/QgAction/Qg·QpTeacherHash/QpAction/四向量changed与计数）只能解释G1，不得改变一级裁决。全部调用点逐行核实（`source-call-chain.csv`）；4类字段需新增只读钩子（teacher指纹/PA进入/descendant派生），池级pddrScore维持 NOT_EXPORTED_AT_POOL_LEVEL。

## 5. 阈值与指标合同冻结（Agent B，recompute脚本自检PASS）

**可比性判定**：FC5-250K（250k+原始telemetry仅远端）、50k gates（PDDR池子集+1375/1553行FINGERPRINT_ONLY）、V3 100k（预算/seed/jar不同）均 **NOT_COMPARABLE** → `matchedWindowFluctuationAvailable=false`，**fallback冻结**：

```ini
whvgShareDeficitPp = 2.0        （deficit = normal − hard，正值=hard更差）
exclusiveNdShareDeficitPp = 10.0
consecutiveWindows = 2          （同一指标同一来源连续两个25k窗口）
epsilon = 1e-12；比较容差 1e-9（仅吸收1-ulp）
```

**t_div（解释性，非因果onset）**：`historicalSufficiency=false`（500k材料仅终态front，无checkpoints）→ fallback：HV progress deficit≥1.0pp **AND** IGD relative-improvement deficit≥10.0pp，连续2个25k checkpoint的最早点；NOT_REACHED亦为登记结果。

关键冻结选择：Fpast=B_{t-1} decision-front snapshot；HV锚=每窗口ND(Fpast∪Wt) ideal/nadir（不clamp，参考点1.1³）；归属=三元组级first-admission（tie-break nominalFE→actualFE→candidateId→source）；ExclusiveND取归属划分读法（share和=100%）；nominal/actual对齐见§6。指标计算直接import `scripts/fc6_metrics.py` corrected管线，不重建。

## 6. 主Agent跨文件裁决（Agent B标记的缺口）

1. **`nominalFE`**：派生列 `nominalFE = 25000 × ceil(actualFE / 25000)`，Observer写账本行时直接计算（已附录冻结进 `observer-schema.md`）；窗口切分一律用该列。
2. **`B_0`**：= ND(账本 source=INITIAL_POPULATION 全部行)，账本离线精确重构（前100行），无需新钩子；B_t（t≥1）=25k checkpoint decision-front snapshot（既有能力）。
3. 该裁决不改四类分类与行为语义；Observer实现缺 `nominalFE` 列即schema违规。

## 7. 主Agent独立复核记录（抽查）

- HARD instanceSha256 重算=注册值 ✓；快照文件存在且sha前缀=84d84523 ✓；pfref 758行 ✓
- NORMAL：run-ledger 100_2_3_1 500k COMPLETED共123行（**A4=12**），referenceEligible=56 ✓；冷归档tar存在且路径=注册值 ✓
- 两JSON解析通过、必需字段在位 ✓；`threshold_recompute.py --audit` PASS、`--selftest` PASS ✓
- `mixCaTaSeed`（631字符区段）与 `settleQp`（3022字符区段）无 nanoTime/currentTimeMillis/Random ✓（与Agent C审计一致）
- 正式Jar `8dad8f40…bad8b9` 未动 ✓

## 8. 未来运行注册表（本轮只生成RunKey，不运行）

```text
执行顺序（冻结，不可越门）：
20k Observer OFF/ON工程门 → Observer schema/Jar冻结 → SA-HARD
→ failure-class复现门 → SA-NORMAL → G1/G3分析
→ 仅G1成立时SA-A2-CONDITIONAL → G1/G2/G3/G4 → 强制停止
禁止：第三实例、第二diagnostic seed、其他arm
```

| RunKey | arm | instance | seed | MaxFEs | observer | 条件 |
|---|---|---|---|---|---|---|
| SA-HARD | A4 | 100_5_3_1 | 20260901 | 500000 | ON | 无（首跑） |
| SA-NORMAL | A4 | 100_2_3_1 | 20260901 | 500000 | ON | SA-HARD通过failure-class复现门 |
| SA-A2-CONDITIONAL | A2 | 100_5_3_1 | 20260901 | 500000 | ON | onlyIf=G1_TRIGGERED |

## 9. 停止条件复核（§十二）

逐项检查：NORMAL唯一解析 ✓；reference哈希闭合 ✓；阈值全部可运行前确定（fallback路径，无需看新结果）✓；source标签全部映射到真实事件（4类字段新钩子观测点已核实）✓；wall-clock已确定（false，A4/A2语义内）✓；无需修改正式算法语义即可观察（V3影子先例+4个新增只读钩子均为纯观察）✓；证据清单完整且哈希闭合 ✓。**无DO_NOT_RUN触发项。**

## 10. 最终机器状态

```ini
phaseA0Decision = PHASE_A0_PREREGISTRATION_PASSED
normalControlResolved = true（NORMAL=100_2_3_1）
hardReferenceFrozen = true
normalReferenceFrozen = true
sourceTaxonomyFrozen = true
sourceThresholdsFrozen = true
performanceThresholdsFrozen = true
observerSchemaDesigned = true
observerImplemented = false
wallClockAuditResolved = true（wallClockInfluencesSearch=false for A4/A2 arms）
runRegistryFrozen = true
newFEConsumed = 0
remoteExperimentUploaded = false
sourceAttribution500kStarted = false
DO_NOT_RUN触发项 = 无
```


---

## PHASEA0-CORRECTION-V1 修正摘要（2026-09-01，独立验收退回后的重新冻结）

初版 Phase A0 被独立验收退回，两项阻断问题与本修正的关系：

1. **阻断问题A（多来源重复目标点反事实错误）**：初版 §5 的『归属=三元组级first-admission（FIRST_ADMISSION_WITHIN_WINDOW，已废止）
   （tie-break nominalFE→actualFE→candidateId→source）』被否决。修正后归属规则 =
   `COUNTERFACTUAL_PRODUCER_SET`：ledger 保留事件级真实来源；`Wt^-s =
   uniqueObjectiveTriples({e ∈ E_t | e.source != s})`（仅 producerSet=={s} 的三元组被剔除，
   共享点对任何单来源的反事实贡献为0）；`multiSourceDuplicateRule =
   SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE`；ExclusiveND = `p ∈ ND(Fpast∪Wt) ∧
   p ∉ Fpast ∧ producerSet(p) == {s}`；first-admission 字段降级为 DESCRIPTIVE_ONLY
   （严禁进入任何门控）。测试 T1–T7 见 `metric-counterfactual-tests.csv`。
2. **阻断问题B（内存外推公式）**：初版 `estimated500kPeak = heapUsedPeak_OFF_20k × 25 +
   observerStreamingPeak` 废止（有界算法基线不得按 FE 线性放大）。修正后分解模型：
   `estimated500kPeak = baselineAlgorithmPeak + observerTransientDelta + safetyMargin`，
   `observerTransientDelta = max(observerMeasuredDelta, observerBoundedResidentCap +
   observerUnflushedBufferCap)`，`safetyMargin = max(0.20 × (baselineAlgorithmPeak +
   observerTransientDelta), predefinedMinimumSafetyBytes)`；硬门不变
   （`< 0.60 × assignedJavaHeap`，等于即 fail-closed）；磁盘账本增长只进磁盘估计。
   本工作包只修合同：`memoryModelDesigned=true`、`memoryPreflightExecuted=false`、
   `memoryGatePassed=false`（未执行≠失败）。T8 见 `metric-counterfactual-tests.csv`。
3. **NORMAL 文字勘误**（不改变 100_2_3_1 选择）：seed 表述修正为『run-ledger 中不存在
   100_2_3_1×20260901 的已执行运行；项目文档中可能存在预登记或计划性文字提及』；
   100_2_4_1 状态统一为 `REFERENCE_MATERIAL_PARTIAL`（非完全合格 tie survivor）。
4. 其余全部维持：NORMAL=100_2_3_1、HARD 绑定、双 reference 合同、四类来源分类、
   fallback 阈值、wall-clock 审计、RunKey 注册表。
