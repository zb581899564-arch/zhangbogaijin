# -*- coding: utf-8 -*-
"""Applies the memory-model correction (PhaseA0-CORRECTION-V1 blocking issue B)
to observer-memory-model.md and observer-memory-preflight-plan.md: the
deprecated x25 extrapolation is replaced by the decomposed model."""
import io

OLD_FORMULA_MD = """estimated500kPeak = heapUsedPeak_OFF_20k × (500k/20k) + observerStreamingPeak
hardGate: estimated500kPeak < 0.60 × assignedJavaHeap"""
NEW_FORMULA_MD = """# ── PHASEA0-CORRECTION-V1（2026-09-01）：分解模型（阻断问题B修正） ──────────
# 废止：estimated500kPeak = heapUsedPeak_OFF_20k × (500k/20k) + observerStreamingPeak
# 理由：算法种群、档案与已flush缓冲是有界常驻对象，20k基线堆占用不得按FE线性放大25倍。
baselineAlgorithmPeak      = heapUsedPeak_OFF_20k                    # 有界算法基线估计（严禁×25）
observerMeasuredDelta      = max(0, heapUsedPeak_ON_20k - heapUsedPeak_OFF_20k)
observerBoundedResidentCap = ND sample容量上限 + forensic reservoir容量上限
                             + 聚合map最大容量 + writer状态 + fingerprint/lineage缓存上限
observerUnflushedBufferCap = maxRowsBeforeFlush × worstCaseBytesPerRowResident
observerTransientDelta     = max(observerMeasuredDelta,
                                 observerBoundedResidentCap + observerUnflushedBufferCap)
safetyMargin               = max(0.20 × (baselineAlgorithmPeak + observerTransientDelta),
                                 predefinedMinimumSafetyBytes=256 MiB)
estimated500kPeak          = baselineAlgorithmPeak + observerTransientDelta + safetyMargin
hardGate: estimated500kPeak < 0.60 × assignedJavaHeap   # 严格小于；等于即 fail-closed
failClosedAlternative: 若 20k 无法证明算法基线有界 → MEMORY_MODEL_INSUFFICIENT / 500K_NOT_AUTHORIZED
                         （不得编造数值，不得用加堆掩盖观察器存储缺陷）
磁盘口径: ledgerGrowthPer10kFE 仅用于估算磁盘体积，不得计入 heap 常驻占用
参考实现: threshold_recompute.py estimate_500k_peak() + --memory-selftest（T8）"""

OLD_FIELD_MD = "| estimated500kPeak | heapUsedPeak_OFF × 25 + observerStreamingPeak（§3 公式，实测值代入） | 派生 |"
NEW_FIELD_MD = "| estimated500kPeak | 分解模型（§3 修正版公式，实测值代入；baseline 不乘 25） | 派生 |"

OLD_PLAN_14 = "14. `estimated500kPeak = heapUsedPeak_OFF × 25 + observerStreamingPeak`（实测代入）。"
NEW_PLAN_14 = ("14. `estimated500kPeak = baselineAlgorithmPeak + observerTransientDelta + safetyMargin`"
               "（PHASEA0-CORRECTION-V1 分解模型：baselineAlgorithmPeak=heapUsedPeak_OFF_20k 有界基线"
               "**不乘25**；observerTransientDelta=max(observerMeasuredDelta, "
               "observerBoundedResidentCap+observerUnflushedBufferCap)；"
               "safetyMargin=max(0.20×(baseline+transient), 256 MiB)；实测值代入）。")

t = io.open("observer-memory-model.md", encoding="utf-8").read()
assert t.count(OLD_FORMULA_MD) == 1, "model formula anchor %d" % t.count(OLD_FORMULA_MD)
t = t.replace(OLD_FORMULA_MD, NEW_FORMULA_MD)
assert t.count(OLD_FIELD_MD) == 1, "model field anchor %d" % t.count(OLD_FIELD_MD)
t = t.replace(OLD_FIELD_MD, NEW_FIELD_MD)
t = t.replace("# PHASEA0-CORRECTION-V1（2026-09-01）：分解模型（阻断问题B修正） ──────────",
              "# PHASEA0-CORRECTION-V1（2026-09-01）：分解模型（阻断问题B修正） ──────────")
io.open("observer-memory-model.md", "w", encoding="utf-8", newline="\n").write(t)

t = io.open("observer-memory-preflight-plan.md", encoding="utf-8").read()
assert t.count(OLD_PLAN_14) == 1, "plan step14 anchor %d" % t.count(OLD_PLAN_14)
t = t.replace(OLD_PLAN_14, NEW_PLAN_14)
io.open("observer-memory-preflight-plan.md", "w", encoding="utf-8", newline="\n").write(t)
print("memory model files updated (x25 deprecated, decomposed model frozen)")
