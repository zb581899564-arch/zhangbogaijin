# Observer 20k 内存 Preflight 执行步骤清单（未来执行；本轮不执行）

状态：`PLANNED_NOT_EXECUTED`
前置条件：`observer-schema.md` 冻结候选稿生效；`V35_SOURCE_ATTRIBUTION_OBSERVER.jar` 独立构建完成（不得覆盖正式 A4 Jar）；实例/seed/arm 固定为 `100_5_3_1 / 20260901 / A4 / 20k`。

## 阶段 P0：构建与静态自检（0 FE）
1. 按观察者纪律实现 observer（复用 V3 的 11 处 patch 锚点 + 新增只读快照钩子）；断言：无 `new Random()`、无 RNG 消耗、无 `problem.evaluate`、无候选修改、无 System.nanoTime 进入账本行序。
2. diff 校验：observer OFF 路径字节级等于冻结 A4 语义（patch 全部由 `if (observerArmed)` 门控，OFF 时不执行任何观察代码分支）。
3. 记录 `assignedJavaHeap`：与运行器一致 `-Xmx4g`，运行时用 `MemoryMXBean.getHeapMemoryUsage().getMax()` 复核并登记。

## 阶段 P1：20k OFF 基线（1 JVM，0.02M FE）
4. 运行 `100_5_3_1/20260901/A4/20k` observer OFF。
5. 全程采样 used heap（每 ≤100ms 或每 1k FE 一次）+ GarbageCollectorMXBean 累计耗时；落盘 `heap-timeseries-off.csv`。
6. 记录 `heapUsedPeak_OFF`、`gcOverhead_OFF`、actualFE、front.csv SHA-256。

## 阶段 P2：20k ON（1 JVM，0.02M FE）
7. 同 JVM 参数、同 seed、同实例、同机同负载条件运行 observer ON。
8. 账本逐窗流式落盘（flushUnit = min(outer cycle, 25k FE)）；记录每窗行数与字节数。
9. 落盘 `heap-timeseries-on.csv`；记录 `heapUsedPeak_ON`、`gcOverhead_ON`。
10. 计算 `bytesPerEvaluatedCandidate = (Σ窗口字节 − 表头) / 行数`，与估算式（144+增量≈300 B/行，上界354）比对；偏差 > 20% 时冻结更新 `observer-memory-model.md` §2 系数（属 schema 修订→重跑本 preflight）。

## 阶段 P3：OFF/ON 行为等价（与 `observer-equivalence-contract.md` 同门执行）
11. 按 equivalence contract 比较全部字段清单；任一系统性分叉 → `BEHAVIORAL_EQUIVALENCE_FAIL` / `SOURCE_ATTRIBUTION_NOT_AUTHORIZED`，停止并修复观察器（不得改算法）。
12. 记录 `unsetSourceRows`（V3 先例字段）：必须为 0；>0 视为标签缺口，修复后重跑。

## 阶段 P4：内存外推与硬门
13. `observerStreamingPeak` = 实测单窗 flush 峰值（由 heap-timeseries-on 窗口间差分提取）+ 固定结构；与模型公式（≈11 MB）交叉验证，两法差 > 25% 以实测为准。
14. `estimated500kPeak = baselineAlgorithmPeak + observerTransientDelta + safetyMargin`（PHASEA0-CORRECTION-V1 分解模型：baselineAlgorithmPeak=heapUsedPeak_OFF_20k 有界基线**不乘25**；observerTransientDelta=max(observerMeasuredDelta, observerBoundedResidentCap+observerUnflushedBufferCap)；safetyMargin=max(0.20×(baseline+transient), 256 MiB)；实测值代入）。
15. `estimatedPeakToHeapRatio = estimated500kPeak / assignedJavaHeap`；硬门：`< 0.60`（4GiB 时 < 2,576,980,377 B）。
16. `gcOverhead_ON − gcOverhead_OFF ≤ +5 个百分点`（advisory）。
17. 输出 `observer-memory-preflight.csv`：`heapUsedPeak, bytesPerEvaluatedCandidate, ledgerGrowthPer10kFE, estimated500kPeak, assignedJavaHeap, estimatedPeakToHeapRatio, gcOverhead`（OFF/ON 两行 + 汇总行）。
18. 判定：通过 → 连同等价门置 `observerSchemaFrozen=true`、`observerJarFrozen=true`，登记公式最终系数；失败 → `MEMORY_GATE_FAIL/500K_NOT_AUTHORIZED`，只允许优化观察器存储（降 flushUnit、截断 teacherHash 至16hex、reservoir 降容、pddr 账本压缩字段），优化后从 P1 重跑；禁止增大 -Xmx 掩盖设计问题。

## 阶段 P5：归档
19. 证据 SHA-256 登记（timeseries、preflight.csv、ledger 窗口文件、jar）。
20. 汇报 preflight 结果并等待 500k 授权（Phase A 顺序不可越门）。

预算：P1+P2 共 0.04M FE（与计划§5 一致）；预计墙钟 ≈ 2×20k 100k-run 外推（单 JVM，无并发要求——见 timing audit 结论）。
