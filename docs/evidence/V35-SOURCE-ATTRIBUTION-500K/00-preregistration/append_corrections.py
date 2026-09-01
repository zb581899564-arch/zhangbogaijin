# -*- coding: utf-8 -*-
"""Appends the PHASEA0-CORRECTION-V1 sections to observer-schema.md and
PREREGISTRATION.md (T7 contract-consistency tokens included)."""
import io

SCHEMA_ADDITION = """

---

## PHASEA0-CORRECTION-V1（2026-09-01）——多来源反事实语义修正（阻断问题A）

独立验收退回：初版合同的 FIRST_ADMISSION_WITHIN_WINDOW 归属把多来源重复目标点错误归属给单一来源（最小反例：GLOBAL_CFVF 与 CATA 先后生成完全相同的三元组 p，删除任一来源的事件后另一来源仍保留 p，反事实 WHVG 双方应为 0；初版却把全部贡献归给最先生成者，产生假的 G1/G3 信号）。

### 冻结修正（Observer schema 层）

1. **ledger 必须保留每条已评价候选事件的真实一级来源标签**；Observer 写入阶段**禁止**按目标三元组只保留第一来源（V3 式逐评估账本结构本身满足，本条为显式禁令）。
2. **三元组去重与 producerSet 构造发生在离线分析层**（threshold_recompute.py `canonical_groups` + `producer_set`），不在 Observer 内执行。
3. 反事实语义（与 source-attribution-thresholds.json attributionRule 完全一致）：
   - 归属规则 = `COUNTERFACTUAL_PRODUCER_SET`；
   - `Wt^-s = uniqueObjectiveTriples({e ∈ E_t | e.source != s})`——仅当某三元组的 `producerSet(p) == {s}`（全部事件来自 s）时才从 Wt 中剔除；多来源共享的三元组必须保留；
   - `multiSourceDuplicateRule = SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE`：producerSet 含 ≥2 来源的点不计入任何来源的 ExclusiveND，且单来源反事实中该点不消失；
   - `ExclusiveND`: `p ∈ ND(Fpast∪Wt) ∧ p ∉ Fpast ∧ producerSet(p) == {s}`；
   - `firstAdmissionScope = DESCRIPTIVE_ONLY`：firstProducerSource/ActualFE/CandidateId 仅用于描述性时序报告/候选出生顺序解释/非门控诊断，**严禁**进入 WHVG/WHVGShare/ExclusiveND/ExclusiveNDShare/G1/G3 门控。
4. 一级来源仍固定四类 `GLOBAL_CFVF / CATA / INHERITED_LS / PARENT_CARRYOVER`，不新增第五类。
5. 本修正不改变：nominalFE 派生列与 B_0 定义（上一节 Main-Agent Resolution）、四类映射、内存硬门、wall-clock 审计结论。
"""

PREREG_ADDITION = """

---

## PHASEA0-CORRECTION-V1 修正摘要（2026-09-01，独立验收退回后的重新冻结）

初版 Phase A0 被独立验收退回，两项阻断问题与本修正的关系：

1. **阻断问题A（多来源重复目标点反事实错误）**：初版 §5 的『归属=三元组级first-admission
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
"""

t = io.open("observer-schema.md", encoding="utf-8").read()
t += SCHEMA_ADDITION
io.open("observer-schema.md", "w", encoding="utf-8", newline="\n").write(t)

t = io.open("PREREGISTRATION.md", encoding="utf-8").read()
t += PREREG_ADDITION
io.open("PREREGISTRATION.md", "w", encoding="utf-8", newline="\n").write(t)
print("schema + PREREGISTRATION correction sections appended")
