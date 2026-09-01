# memory-model-correction.md — 内存外推公式修正说明（PhaseA0-CORRECTION-V1 阻断问题B）

- 日期：2026-09-01
- 性质：合同修正（0-FE；Observer 未实现；正式Jar未动）

## 1. 原错误

初版公式：

```text
estimated500kPeak = heapUsedPeak_OFF_20k × (500k/20k) + observerStreamingPeak
                  = heapUsedPeak_OFF_20k × 25 + observerStreamingPeak
```

错误原因：把 20k OFF 运行的**整个堆占用**当作随 FE 线性放大的量。实际上 20k 堆占用中
绝大部分是**有界常驻对象**（算法种群 100 个体、增量 ND 档案、已 flush 的缓冲、JVM/框架
基线），它们在 500k 时不会增长 25 倍；随 FE 增长的只有观察器的有界驻留结构、未 flush
缓冲与瞬态分配。×25 会把 20k 基线放大到约 50 GB，在任何合理堆配置下必然误报
MEMORY_GATE_FAIL，或反过来诱使实现者用"加堆"掩盖真实的观察器存储缺陷。

## 2. 修正后的分解模型（冻结）

```text
baselineAlgorithmPeak      = heapUsedPeak_OFF_20k            # 有界算法基线估计（严禁×25）
observerMeasuredDelta      = max(0, heapUsedPeak_ON_20k - heapUsedPeak_OFF_20k)
observerBoundedResidentCap = ND sample容量上限 + forensic reservoir容量上限
                             + 聚合map最大容量 + writer状态 + fingerprint/lineage缓存上限
observerUnflushedBufferCap = maxRowsBeforeFlush × worstCaseBytesPerRowResident
observerTransientDelta     = max(observerMeasuredDelta,
                                 observerBoundedResidentCap + observerUnflushedBufferCap)
safetyMargin               = max(0.20 × (baselineAlgorithmPeak + observerTransientDelta),
                                 predefinedMinimumSafetyBytes = 256 MiB)
estimated500kPeak          = baselineAlgorithmPeak + observerTransientDelta + safetyMargin
hardGate                   = estimated500kPeak < 0.60 × assignedJavaHeap   # 严格小于；等于即fail-closed
```

要点：

1. `baselineAlgorithmPeak` 是**有界算法基线估计**——结合"历史正式 A4/500k 能在相同或更小
   assigned heap 完成"的事实，说明算法常驻对象在 500k 内有界；注释登记为估计，不得乘 25。
2. 观察器侧按**容量上限**计（ND sample / forensic reservoir / 聚合 map / writer / 缓存各自
   的硬上限相加），与实测 delta 取 max——双保险，任一口径失守都能被另一口径捕获。
3. `safetyMargin` = max(20% 相对裕量, 256 MiB 绝对下界)。
4. `ledgerGrowthPer10kFE` 只用于估算磁盘体积（约 3.0 MB/10k FE → 500k ≈ 150–176 MB 磁盘），
   **不得计入 heap 常驻**。
5. fail-closed 出口：若 20k 无法证明算法基线有界 → `MEMORY_MODEL_INSUFFICIENT /
   500K_NOT_AUTHORIZED`，不得编造数值。

## 3. T8 单元检查（`threshold_recompute.py --memory-selftest`，全部 PASS）

| 检查 | 内容 | 结果 |
|---|---|---|
| T8.1 | baseline-only 估计 = B + 20%·B（非 25×B）；确定性 | PASS |
| T8.2 | buffer cap 增大 ⇒ 估计单调不减 | PASS |
| T8.3 | 函数签名无 disk/ledger 参数（磁盘增长不影响 heap 估计） | PASS |
| T8.4 | 硬门边界：<0.60 PASS；==0.60 FAIL；>0.60 FAIL（fail-closed） | PASS |
| T8.5 | 端到端示例：pass-case（1.5GiB 基线/6GiB 堆，ratio=0.306 PASS）与 fail-case（2GiB 基线/4GiB 堆，ratio=0.609 FAIL→MEMORY_MODEL_INSUFFICIENT 路径） | PASS |

（示例数值均为合同演示值，非实测。）

## 4. 未改变内容

硬门阈值 0.60、严格小于语义、preflight 字段定义、`memoryPreflightExecuted=false`/
`memoryGatePassed=false`（本工作包只修合同，真实数值待未来 Observer 20k OFF/ON 实测）。
