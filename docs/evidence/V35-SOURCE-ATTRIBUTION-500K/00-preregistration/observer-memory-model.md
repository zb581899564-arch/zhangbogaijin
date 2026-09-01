# Observer 内存模型与 preflight 硬门（V35 SOURCE-ATTRIBUTION-500K Phase A0）

状态：`DESIGNED_NOT_IMPLEMENTED`（本轮只设计；未来 20k preflight 按 `observer-memory-preflight-plan.md` 执行）
依据：V3 SOURCE-CONTRIBUTION-DIAGNOSTICS-V1 实测账本（同源 Observer 先例）+ 冻结源码结构分析。

## 1. V3 实测基准（全部为已存在文件，可复核）

来源：`docs/evidence/V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1/04-remote-100k/sync/seed-20260919/results/run-GAPLSRC-C0-100_5_3_1-20260919/`

| 项目 | 实测值 |
|---|---|
| source-ledger.csv 行数（含表头） | 96,026（96,025 条已评估候选，actualFE=96025） |
| source-ledger.csv 字节数 | 13,827,526 B（≈14MB 量级，与计划§3.4 引述一致） |
| bytesPerEvaluatedCandidate（磁盘CSV，100-job 100_5_3_1） | 13,827,526 / 96,025 = **144.00 B/行** |
| pddr-round-ledger.csv | 2,989 行 / 378,310 B（126.6 B/行；0.0311 行/FE） |
| front.csv（decision front） | 276 行 / 15,276 B（数百行量级，与计划引述一致） |
| passive-archive.csv（ND 存活集） | 390 行 / 21,551 B |
| 100k 时 V3 账本 JVM 内驻留（StringBuilder，UTF-16） | ≈144 字符/行 × 2 B × 96,025 ≈ 27.7 MB（未流式） |

行结构（V3）：`observedFE,source,candidateFingerprint(64hex),Cmax,TEC,TWC,parentLineageId`。
同目录 50_2_3_1 三 seeds 与 gate20k/gate2k 账本同口径（20k gate：15,258 行 / 2,171,870 B = 142.3 B/行，量级一致）。

## 2. bytesPerEvaluatedCandidate 估算式（新 schema）

V3 基线行 144 B 的构成：指纹64 + source≈16 + fe≈6 + 三目标≈3×19 + parentLineageId≈4 + 分隔7。
新 schema（observer-schema.md §1/§2）增量：

```text
B_row = 144                                  (V3 基线)
      + 64                                   (parentFingerprint，除 INITIAL 外恒有)
      + 12                                   (generation 三元组 cycle/round/qRound)
      + 15                                   (7个生命周期布尔+分隔)
      + 64 × P_teacher                       (teacherFingerprint，有则64hex)
      + 55 × P_GLOBAL_CFVF                   (二级字段：finalEvaluate/role/QgQp动作/两teacherHash(16hex×2)/4changed/4counts)
A4 实测分布（V3 100k 账本）：P_GLOBAL_CFVF = 60000/96025 = 0.625
P_teacher 保守取 0.5

E[B_row] = 144 + 64 + 12 + 15 + 32 + 34.4 ≈ 301 B/行
保守上界（P_teacher=1、所有行带全二级）= 144+64+12+15+64+55 = 354 B/行
登记估算：bytesPerEvaluatedCandidate ≈ 300 B/行（下界144，上界354）
```

注意：144 B/行是 UTF-8 磁盘口径；JVM 内 StringBuilder 为 UTF-16，即 **2 B/字符**，流式 flush 前的驻留 = 2 × 行字符数。

## 3. 外推公式（preflight 后以实测替换）

```text
ledgerRows_500k            ≈ 500,000（账本观察每条被接纳的正式评估；100k 预算实际落账 96,025，500k 取上限）
ledgerGrowthPer10kFE(disk) = 10,000 × bytesPerEvaluatedCandidate
                           ≈ 3.0 MB / 10k FE（V3 旧口径 1.44 MB/10k）
ledgerCsvTotal_500k        ≈ 150 MB（保守 176 MB）——仅磁盘，不驻留内存

observerStreamingPeak      = flushUnitRows × B_row_chars × 2B/char × 2(growth doubling)
                           + observerFixedStructures
flushUnit = min(单outer cycle行数≈8,500, 单25k窗口行数=25,000) → 取 outer cycle ≈ 8,500 行
observerStreamingPeak      ≈ 8,500 × 300 × 4 ≈ 10.2 MB + 固定结构(reservoir 768行×400B + 计数器 + 摘要 ≈ 0.5 MB) ≈ 11 MB

# ── PHASEA0-CORRECTION-V1（2026-09-01）：分解模型（阻断问题B修正） ──────────
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
参考实现: threshold_recompute.py estimate_500k_peak() + --memory-selftest（T8）
```

防御性反证（若流式 flush 失效退化为 V3 整账模式）：
`naiveUnbounded500kPeak ≈ 500,000 × 300字符 × 2B × 2(growth) ≈ 600 MB < 0.60 × 4GiB = 2.4 GiB`——
即硬门对实现缺陷仍有 4 倍裕度，但 naive 模式属 schema 违规（observer-schema.md §4.4），不得作为实现依据。

## 4. 二级字段增量成本（相对 V3 每行）

| 增量字段 | 每行成本 | 备注 |
|---|---|---|
| parentFingerprint | +64 B（恒定） | 最大的单项增量 |
| teacherFingerprint | +64 B×出现率 | 账本落账截断16hex则 +16 B×出现率（schema §3 表：账本全64hex，摘要链16hex） |
| 生命周期 7 布尔 | +15 B | 固定 |
| generation 三元组 | +12 B | 固定 |
| GLOBAL_CFVF 二级束 | +55 B×0.625 | 含 2×16hex teacherHash、4 changed、4 counts |
| CPU 侧（非内存） | 四向量逐段 diff O(vectorLen) 每行 | 有界输出 4 布尔+4 long；不落数组 |

## 5. preflight 字段定义（冻结；输出 `observer-memory-preflight.csv`）

| 字段 | 定义 | 来源 |
|---|---|---|
| heapUsedPeak | 20k run 全程 Runtime/内存池采样（≥每100ms 或每1k FE）的 used heap 最大值 | ON 与 OFF 各测一次 |
| bytesPerEvaluatedCandidate | (20k ledger 落盘字节 − 表头) / 行数 | 实测 |
| ledgerGrowthPer10kFE | 10,000 × bytesPerEvaluatedCandidate | 派生 |
| estimated500kPeak | 分解模型（§3 修正版公式，实测值代入；baseline 不乘 25） | 派生 |
| assignedJavaHeap | 启动参数 -Xmx 实际生效值（运行器标准 -Xmx4g = 4,294,967,296 B） | 实测（MXBean max heap） |
| estimatedPeakToHeapRatio | estimated500kPeak / assignedJavaHeap | 派生 |
| gcOverhead | GC 总耗时 / 算法总耗时（GarbageCollectorMXBean 累计），ON−OFF 差值单列 | 实测 |

## 6. 硬门与验证步骤

```ini
MEMORY_HARD_GATE: estimated500kPeak < 0.60 × assignedJavaHeap     # 4GiB 时门限 = 2,576,980,377 B ≈ 2.4 GiB
GC_ADVISORY:      gcOverhead_ON − gcOverhead_OFF ≤ +5 个百分点
FAIL_VERDICT:     MEMORY_GATE_FAIL / 500K_NOT_AUTHORIZED
```

验证路径（计划§3.4 原文：20k ON 实测 → 线性外推 500k → 公式登记）：
1. 同 JVM 参数、同 seed、同实例跑 20k OFF，记录 heapUsedPeak 与 gcOverhead 基线；
2. 跑 20k ON，记录 heapUsedPeak_ON、bytesPerEvaluatedCandidate（实测替换§2 估算）、gcOverhead_ON；
3. 用实测 observer 增量（heapUsedPeak_ON − heapUsedPeak_OFF）与 §3 公式交叉验证：两法估计差 > 25% 时以实测为准并复核公式假设；
4. 线性外推 500k，代入硬门；通过 → `observerSchemaFrozen=true` / `observerJarFrozen=true`（连同等价门）；
5. 失败时只允许优化观察器存储（更小 flushUnit、截断 teacherHash、 reservoir 降容），禁止加大堆掩盖设计问题。
