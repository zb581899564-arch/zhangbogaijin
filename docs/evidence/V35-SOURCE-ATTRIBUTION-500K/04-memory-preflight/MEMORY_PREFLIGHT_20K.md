# MEMORY_PREFLIGHT_20K — 真实内存/GC门（流式修正版；分解模型baseline不乘25）

- 日期：2026-09-01；assignedJavaHeap = 4 GiB
- Observer Jar SHA-256：78bf4d3016a612a9f3073ca00abb94181ef4883b2838540ac9776b1eed046565
- 0-FE勘误（2026-09-01）：补齐ON侧heap实测值与GC实测、磁盘账本改为实测口径
  （见 `../EVIDENCE_ERRATA_20260901.md`）；分解模型、硬门与结论不变

## 流式修正

初版`flushedEventLedger`将全部flushed历史事件留在内存（`StringBuilder`持续追加），
导致观察器内存并非有界、初版estimated500kPeak=1.40GB漏掉了完整ledger常驻。已修正：
flush到磁盘临时文件（`File.createTempFile`+`BufferedWriter`），内存仅持有有界未flush缓冲
（≤25000行×1024B=25MB）。closeLedgerWriters先flush残留再关闭，Runner从磁盘复制
source-ledger.csv/pddr-round-ledger.csv到输出目录并从磁盘计数行数。

## 实测

| 字段 | OFF | ON |
|---|---:|---:|
| heapUsedPeak | 945,796,576 B | 940,463,104 B（ON-OFF=-5,333,472 B → observerMeasuredDelta=0） |
| heapCommittedPeak | 1,227,882,496 B | 1,204,813,824 B |
| gcCollectionCount | 46 | 59 |
| gcCollectionTime | 490 ms | 591 ms |

## 分解模型

```ini
baselineAlgorithmPeak = 945796576  (= heapUsedPeak_OFF_20k)
observerMeasuredDelta = 0  (ON ≤ OFF；真流式设计下观察器不抬高峰值)
observerBoundedResidentCap = 1671168  (代码常量)
observerUnflushedBufferCap = 25600000  (25000 rows × 1024 B)
observerTransientDelta = 27271168  (= max(0, 1671168 + 25600000))
safetyMargin = 268435456  (= max(0.20×(baseline+transient), 256 MiB))
estimated500kPeak = 1241503200
estimatedPeakToHeapRatio = 0.2891
```

## 硬门

```ini
estimated500kPeak < 0.60 × assignedJavaHeap
1241503200 < 2576980377.6 → PASS
memoryGatePassed = true
```

磁盘账本（实测口径；该值属于**磁盘容量**，不进入heap估算）：`source-ledger.csv` 真实大小
**4,135,598 B（含表头）/ 15,258 FE**，bytesPerEvaluatedCandidate = (4,135,598 − 307) / 15,258
= **271.02 B/行**（落在预登记估算带 144–354 B/行内，相对登记值 300 B/行偏差约 −9.7%，
未超 20% 阈值，`observer-memory-model.md` §2 系数无需冻结更新）。按线性磁盘估算：
500k ≈ 135,522,283 B ≈ **135.5 MB 磁盘**。该磁盘容量值不进入heap估算——
estimated500kPeak = 1,241,503,200 B 的分解模型不含任何磁盘账本项，勘误前后完全一致。

## 结论

`memoryPreflightExecuted=true`、`memoryGatePassed=true`（真流式设计，观察器内存有界已由
20k实测证明且ledger不驻留）。
