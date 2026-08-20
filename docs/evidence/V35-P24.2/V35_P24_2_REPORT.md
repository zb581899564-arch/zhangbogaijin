# V35-P24.2 dualQ P/G-block 参数校准与冻结重建

日期：2026-08-16  
前置：V35-P24.1（冻结修订）、V35-P25E（忠实适配诊断）、V35-P25D 隔离  
状态：`completed`（参数实施、诊断、冻结重建、幂等全部通过）；`gBlockLength` 为候选参数，正式启用待用户批准

## 1. 动机（诊断链）

- P25E 5 seed 50k：A4 相对 QGS 基线 HV +38.6%、IGD −35.7%（忠实口径下成立）。
- 预算节奏单变量（LS 30→2，5 seed 确认）：HV +16~18%、IGD −27~31%——50k 下 A4 落后主要是继承局部搜索吞预算。
- 500k 4 seed 对照：A4 与 QGS 预算结构相同（外层循环 18、local 占比 81% vs 82%），HV 均势（2:2），IGD/TEC 4/4 领先；机制差异定位为 **dualQ 分块冻结削减 Qg 长预算学习**：A4 的 Qg TD=2000（G-block 400 轮），QGS 全程学习 TD=3600。
- 用户批准：不改创新点结构，实施 P/G-block 参数级校准（P24.2 流程）。

## 2. 参数实施（向后兼容，默认行为不变）

| 文件 | 改动 |
|---|---|
| `ZhangBoDualQCoordinationConfiguration` | 新增 `gBlockLength`（默认=blockLength）；`blockFrozen(warmup, pLen, gLen)` 重载；canonicalText 增加一行 |
| `ZhangBoDualQCoordinator` | P/G 块交替支持不等长（每对 P+G 轮次推进）；等长时 blockIndex/offset 与历史公式逐位一致（单元测试钉住） |
| `V35ProductionConfiguration` | 新增可选 `dualQCoordination`（默认 null，canonicalText/哈希不变） |
| `ZhangBoGlobalSearchConfiguration.forV35` | 可选配置优先，否则默认 `blockFrozen()` |

定向测试 17/17 通过（含 3 项新增：不等长块调度、等长复现历史、gBlockLength 拒绝 0）。

## 3. 500k 诊断（20_2_3_1，4 seed，Table 9 正式参数，统一 reference 881 点）

| 配置 | median HV | median IGD | median Cmax | median TEC | Qg TD 学习 |
|---|---:|---:|---:|---:|---:|
| A4-gb5（正式） | 0.8635 | 0.0825 | 191.70 | 8443.78 | 2000 |
| QGS 基线 | 0.8460 | 0.1412 | 189.49 | 8659.02 | 3600 |
| A4-gb10 | 0.8731 | 0.0781 | 189.42 | 8490.38 | 2520 |
| **A4-gb15** | **0.8738** | **0.0756** | **187.94** | **8381.27** | 2800 |

- gb15 相对正式 A4：HV +1.2%、IGD −8.4%、Cmax −2.0%（全场最优）、TEC −0.7%（全场最优）。
- gb15 相对 QGS 基线：HV +3.3%、IGD −46%、Cmax、TEC 均更优——**首个"4 seed 中位数全面优于基线"的 500k 证据**。
- 逐 seed：gb15 对 gb5 的 HV 胜 2/4，对 QGS 胜 3/4；minCmax 在 3/4 seed 优于 gb5。
- 改善幅度温和，未做显著性检验（4 seed 诊断）。

## 4. 冻结重建与回归

- V35-P24 与 V35-P24.1 冻结物按当前源码树重建，`V35P24FreezeCaptureTest` 与 `V35P241FreezeRevisionTest` 磁盘幂等契约通过。
- jmetal-algorithm 全量回归（`--add-opens` 命令）：262 项中与本次改动直接相关的失败为 0。既有失败不变：V35P101 前沿快照不兼容（D-076 登记）、NSGAIIIT/DifferentialEvolutionTestIT（jMetal 上游环境）、Mockito errors（JDK17，`--add-opens` 后消失）。

## 5. 边界与下一步

- `gBlockLength=5`（等长）仍是正式默认；gb10/gb15 为候选参数，**正式启用需用户批准**，批准后更新本冻结物并全量回归。
- 本诊断不改变 `formal_matrix_started=false`，不进入正式 reference。
- 证据：逐 seed 运行见 `../V35-P25E-corrected-comparison/dualq-gblock/`（front.csv + mechanism-summary.txt），指标口径 `P8MetricCalculator` 精确扫描线。
