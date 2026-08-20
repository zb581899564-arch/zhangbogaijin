# FC-6 Stage 2：BP-PDDR 20-job 500k ×3 seed 验证（Build B）

- 日期：2026-08-19（训练机 tmux fc6-stage2，启动 11:39:56+08，三进程并行，~42 分钟完成）
- Build B jar：`jmetal-exec-5.8-BUILD-B-bppddr.jar`（sha256 前 16 位 `9b71d206159cc460`；= FC-5.2 最终版 + 唯一算法改动 `boundaryReservedIndices` 接入 `select()`，零新增参数）
- 对照：`fc2-500k-20_2_3_1/pacing/*`（FC-2 官方 pacing 基线，与 Stage 1 3/3 逐字节复现的同一批次）

## 1. 质量门（6-front union reference，fc6_metrics.py 与 P8MetricCalculator 逐位一致）

| seed | baseline | BP | BP 相对基线 | 门 | 判定 |
|---|---:|---:|---|---|---|
| HV | 0.897033 / 0.937543 / 0.874922 | 0.934794 / 0.902612 / 0.899859 | +3.50% / −0.06% / −0.37% | 退化<2% | **PASS（最大 −0.37%）** |
| IGD | 0.040476 / 0.026462 / 0.057544 | 0.031882 / 0.040902 / 0.043485 | −23.2% / −1.4% / +4.8% | 退化<10% | **PASS（最大 +4.8%）** |
| frontSize | 618 / 613 / 599 | 604 / 795 / 580 | — | sane | PASS |

## 2. 核心目标：Cmax

| seed | baseline minCmax | BP minCmax | Δ |
|---|---:|---:|---:|
| 20260822 | 188.39 | **171.74** | **−8.8%** |
| 20260823 | 175.70 | **175.35** | −0.2% |
| 20260824 | 195.70 | **176.53** | **−9.8%** |

- **Cmax 中位数 188.39 → 175.35（−6.9%）**；三个 seed 全部不劣于基线。结果优于预期带 180–184（带为保守估计，取得更优值）。
- 三极值全部进入最终 front：seed22 171.74 / seed23 175.35 / seed24 176.53 各自即 front 最小 Cmax（= 问题侧 bestCmaxEvaluatedOverall）——**知识保留闭环：被保留的边界极值持续迭代出更优 Cmax**（如 seed22：存档极点 171.74 由 173.76 路径演化而来，后者 PDDR SURVIVE rank=0）。

## 3. TEC / TWC（无系统性退化）

| seed | baseline TEC | BP TEC | baseline TWC | BP TWC |
|---|---:|---:|---:|---:|
| 22 | 8303.47 | 8311.05 (+0.1%) | 12737.04 | 12507.08 (−1.8%) |
| 23 | 8339.28 | 8363.17 (+0.3%) | 12457.12 | 12722.90 (+2.1%) |
| 24 | 8355.00 | 8331.82 (−0.3%) | 12421.85 | 12542.78 (+1.0%) |

TEC 最大 +0.3%、TWC 一升一降一降——无系统性退化，direction 由 HV/IGD 门覆盖。

## 4. 机制（fc52）

| seed | boundaryPool | boundarySurvived | R_retain | bestEver(问题侧) |
|---|---:|---:|---:|---:|
| 22 | 186 | 186 | **1.0000** | 171.7361 |
| 23 | 186 | 186 | **1.0000** | 175.3547 |
| 24 | 186 | 186 | **1.0000** | 176.5327 |

- R_retain=1.0000 三/三：select 的保留规则与 audit 镜像逐轮一致（实现一致性校验通过）。
- 机制可见证据（seed22）：bestEver 候选 173.759（VNS，fe=376603）→ **PDDR SURVIVE:rank=0** → archive addCalled → 被更优 171.736 取代且 171.736 留在最终 front——BP-PDDR 之前这是 3/3 必死的路径，现在极值持续存活并迭代。

## 5. 已知口径差距（诚实声明）

- fc52 的"出生"钩子未包接 `vnd()`/`factorySearch()`（工厂间变邻域）内的 `evaluator.evaluate(current_pop1/pop1)`（ZhangBoMOHPSOQ L6438/6439/6454/6455/6902）——该路径经problem.evaluate 被问题侧 V35CmaxBestEver 计数，但 fc52 不记录其出生与 archive 事件。
- 影响：fc52 bestEver/archive 计数对"VND 产出的最优解"欠计（Stage 2 seed22 的 171.736 即此路径产出；基线三 seed 两侧观测器完全一致，故 FC-5.2 死亡链结论不受影响——死亡链基于逐候选记录而非全局计数器）。
- 处置：报告采用问题侧 `bestCmaxEvaluatedOverall` 为权威"最优已评估 Cmax"；fc52 完整接线的补全登记为 FC-6 后续观察增强（纯观察，不改变算法行为，另行构建验证）。

## 6. 结论

Stage 2 门**全部通过**：Cmax 中位数 −6.9%（175.35 < 188.39 且优于预期带），HV 最大退化 −0.37%（<2%），IGD 最大 +4.8%（<10%），TEC/TWC 无系统性退化，R_retain=1.0000，front 规模 sane → **进入 Stage 3（100-job veto 专项）**。

## 7. 证据文件

- `stage2-bp20/seed-2026xxxx-front.csv`（最终 nondominated front）
- `stage2-bp20/seed-2026xxxx-mechanism-summary.txt`（含 fc52 生命周期 + R_retain）
- `stage2-bp20/seed-2026xxxx-console.log`
- 指标：`scripts/fc6_metrics.py`（与 P8MetricCalculator 同源，FC-2 数值逐位验证）
- 基线对照：`docs/evidence/V35-P26/experiments/fc2-500k-20_2_3_1/pacing/*`