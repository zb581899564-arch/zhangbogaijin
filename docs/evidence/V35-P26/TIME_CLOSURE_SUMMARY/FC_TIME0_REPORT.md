# FC-TIME-0 正式计时报告（2026-08-18）

> 协议：20_2_3_1 / 500k FE / seed 20260822 / population=100 / 同机（zbdepc，JDK17）单 JVM 串行 / 每臂 warm-up 1 次 + 正式 3 次取中位。
> 臂与 runner：QGS=ZhangBoV35P25ECorrectedComparisonRunner（HMOPSO_QGS_F，V35_BASELINE）；Legacy/Pacing=ZhangBoV35P25EBudgetDiagnosticRunner（legacy 空 budget，pacing `--local-fe-budget 0.25:0.65`）。时间口径均为 algorithm.run() 内 wall-clock（runNanos）。

## 1. 逐次运行（秒）

| 臂 | run0（warm-up） | run1 | run2 | run3 | 正式中位 |
|---|---:|---:|---:|---:|---:|
| QGS | 22.5 | 23.6 | 23.6 | 22.6 | **23.6** |
| Legacy | 40.8 | 40.3 | 39.4 | 38.1 | **39.4** |
| Pacing | 147.8 | 146.0 | 144.6 | 146.3 | **146.0** |

## 2. 比值与时间门

```
R1 = Legacy/QGS    = 1.67×   （三创新机制栈的真实代价）
R2 = Pacing/Legacy = 3.71×   （pacing 预算调度的真实代价）
R  = Pacing/QGS    = 6.20×   （新版 vs 李明哲基线的最终比值）
```

时间门（D-083）：红线 >10×；可接受 5–8×；理想 3–5×。**R=6.20× → 可接受区间 → 时间门通过（≤8×），FC-8 正式矩阵可启动；无需 FC-TIME-3（βmin 拐点）。**

## 3. 与优化前对比（语义等价优化 A1-A3 的收益）

| 臂 | 优化前（fc2-500k-local，zbdepc 串行） | 优化后 | 降幅 |
|---|---:|---:|---:|
| Legacy | 99–143s | 38–41s | **−65%** |
| Pacing | 976–1131s | 145–148s | **−85%** |
| QGS | （P25E 服务器 20s） | 23.6s | — |

- 收益在 500k 比 50k（−48%）更大：DSCR 的 A×T 过滤随 archive 增长是超线性放大，A1（fingerprint hoist）的收益正比于该放大量。
- 优化是全家族语义等价的（legacy 也受益），front/Q/事件流逐位一致（FC-TIME-2 验收报告）。
- 此前"55 倍"为跨机粗估（1092s 桌面 vs 20s 服务器），同机口径下真实值为 **6.2×**。

## 4. 裁决

1. **时间门通过（6.20× ∈ 可接受区间）**；允许进入 FC-5 → FC-6 → FC-7 → FC-8。
2. FC-TIME-3（βmin 0.25/0.30/0.35 质量-时间拐点）**不触发**，作为可选后续（若想冲 3–5× 理想区间可另行批准）。
3. 论文计时口径建议：以本报告同机串行数值为准（23.6 / 39.4 / 146.0s）；正式矩阵的时间图待 FC-8 统一口径后冻结。

## 5. 证据

- 逐次运行：`fc-time0/{qgs,legacy,pacing}/run{0..3}/`（QGS 输出在 `runs/seed-20260822/HMOPSO_QGS_F/`）
- 汇总脚本：`scripts/fc-time0-summary.py`；批次脚本：`scripts/run-fc-time0-20260818.sh`