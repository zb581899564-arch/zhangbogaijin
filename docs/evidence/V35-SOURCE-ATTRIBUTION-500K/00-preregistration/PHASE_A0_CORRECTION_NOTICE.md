# PHASE_A0_CORRECTION_NOTICE — V35-PHASEA0-CORRECTION-V1

- 日期：2026-09-01
- 性质：Phase A0 预登记包被独立验收退回后的修正记录（append-only；原裁决快照保留）

## 1. 退回原因（独立验收结论）

初版 Phase A0（2026-08-31，`phase-a0-decision.pre-correction.properties`）在两项验收判据上被独立复验否决：

- **阻断问题A**：`metricAttributionContractValidated=false`——来源归因采用 FIRST_ADMISSION_WITHIN_WINDOW，多来源重复目标点被错误归因；
- **阻断问题B**：`memoryPreflightModelValidated=false`——内存外推公式 `heapUsedPeak_OFF_20k × 25` 无效。

初版其余部分（20文件清单闭合、12份NORMAL raw fronts冷归档逐份匹配、HARD绑定、fallback阈值来源、wall-clock审计、Observer未实现、零FE零上传）**验证通过，未重做**。

## 2. 阻断问题A：多来源重复目标点反事实错误

### 2.1 最小反例

```text
GLOBAL_CFVF 生成点 p=(0.5,0.5,0.5)
CATA 随后生成完全相同的 p
```

删除 GLOBAL 事件后 CATA 仍保留 p，因此反事实语义下：

```text
WHVG_GLOBAL_CFVF = 0
WHVG_CATA = 0
ExclusiveND_GLOBAL_CFVF = 0
ExclusiveND_CATA = 0
producerSet(p) = {GLOBAL_CFVF, CATA}
```

初版实现（FIRST_ADMISSION_WITHIN_WINDOW）却把全部贡献归给 GLOBAL（最先生成者），产生假的 G1/G3 信号。

### 2.2 影响范围

- 门控指标 WHVG/WHVGShare/ExclusiveND/ExclusiveNDShare 在任何"多来源生成相同三元组"的窗口被系统性歪曲；
- 100-job 困难实例正是多来源候选密集交叉的区域 → 假 G1/G3 风险高；
- Epoch边界与 tie-break 链（nominalFE→actualFE→candidateId→source）不受影响，但仅剩描述性用途。

### 2.3 修正内容

1. `threshold_recompute.py`：`window_metrics()` 重写为反事实 producerSet 语义——
   `Wt^-s = uniqueObjectiveTriples({e ∈ E_t | e.source != s})`，仅 `producerSet(p) == {s}` 的三元组被剔除，共享点保留；`producer_set(group)` 新增；ExclusiveND 按 `producerSet == {s} ∧ p ∉ Fpast ∧ p ∈ ND(Fpast∪Wt)` 计数；
2. `source-attribution-thresholds.json`：`attributionRule.rule = COUNTERFACTUAL_PRODUCER_SET`、`multiSourceDuplicateRule = SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE`、`firstAdmissionScope = DESCRIPTIVE_ONLY`、`firstClassSources` 冻结四类；
3. `observer-schema.md`：ledger 保留事件级真实来源（Observer 写入阶段禁止按三元组折叠）；去重与 producerSet 构造在离线分析层；
4. `PREREGISTRATION.md`：§5 初版归属文字标注废止并指向修正语义。

### 2.4 未改变内容

四类一级来源（GLOBAL_CFVF/CATA/INHERITED_LS/PARENT_CARRYOVER，无第五类）；fallback阈值（2.0pp/10.0pp/连续2窗）；epsilon=1e-12；deficit符号方向；Fpast=B_{t-1} decision front；nominalFE 派生列与 B_0 定义；FIRST_ADMISSION 字段本身（降级为 DESCRIPTIVE_ONLY 保留）。

## 3. 阻断问题B：内存外推公式错误

### 3.1 原错误

`estimated500kPeak = heapUsedPeak_OFF_20k × 25 + observerStreamingPeak`——把有界算法基线堆占用按FE线性放大25倍（算法种群/档案/已flush缓冲是常驻有界对象）。

### 3.2 修正内容（分解模型，详见 memory-model-correction.md）

```text
baselineAlgorithmPeak = heapUsedPeak_OFF_20k（有界算法基线估计，严禁×25）
observerTransientDelta = max(observerMeasuredDelta,
                             observerBoundedResidentCap + observerUnflushedBufferCap)
safetyMargin = max(0.20 × (baselineAlgorithmPeak + observerTransientDelta), 256 MiB)
estimated500kPeak = baselineAlgorithmPeak + observerTransientDelta + safetyMargin
硬门不变：estimated500kPeak < 0.60 × assignedJavaHeap（严格小于；等于即fail-closed）
磁盘账本增长只进磁盘估计；20k无法证明基线有界 → MEMORY_MODEL_INSUFFICIENT/500K_NOT_AUTHORIZED
```

### 3.3 本工作包边界

只修合同：`memoryModelDesigned=true`、`memoryPreflightExecuted=false`、`memoryGatePassed=false`（未执行≠失败）。真实数值只能在未来 Observer 20k OFF/ON preflight 实测。

## 4. NORMAL 文字勘误（不改变 100_2_3_1 选择）

1. **seed表述**：改为"run-ledger中不存在100_2_3_1×20260901的已执行运行；项目文档中可能存在预登记或计划性文字提及"（原文"anywhere in docs"过强）。
2. **100_2_4_1状态统一**：`hasFreezablePreRunReferenceMaterial=partial` 与"仅因字典序淘汰"矛盾 → 统一为 `EXCLUDED_REFERENCE_MATERIAL_PARTIAL`（非完全合格tie survivor；淘汰原因=不完整per-run front绑定 + 字典序）。摘要行 `tiedSurvivorsEliminatedByTiebreak` 同步更正。

## 5. 测试结果（0-FE）

开发者自测（`threshold_recompute.py --selftest/--memory-selftest`）与主Agent独立复核
（`../06-independent-verification/main_agent_correction_verification.py`，期望值由测试合同
显式给定）双路径执行：**T1–T8 全部 PASS**（逐项见 `metric-counterfactual-tests.csv`）。

## 6. 机器状态（修正后）

```ini
phaseA0Decision=PHASE_A0_PREREGISTRATION_PASSED
metricAttributionContractValidated=true
multiSourceCounterfactualSemanticsValidated=true
memoryPreflightModelValidated=true
memoryPreflightExecuted=false
memoryGatePassed=false
observerImplemented=false
newFEConsumed=0
remoteExperimentUploaded=false
sourceAttribution500kStarted=false
```
