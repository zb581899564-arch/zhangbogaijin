# checkpoint-reference-contract.md — 检查点 reference 与指标契约（冻结）

## frontType 隔离规则（不得混用）

| frontType | 内容 | 用于 |
|---|---|---|
| terminal-decision-front | run 末 algorithm 决策前沿（front.csv 同源，含指纹） | PFref_terminal、终态 HV/IGD/C/Spacing |
| terminal-observed-full-front | run 末被动档案（passive-archive 同源，含指纹） | 仅诊断，不进终态指标 |
| checkpoint-decision-front | 检查点瞬间决策前沿 | 检查点趋势诊断（辅助） |
| checkpoint-observed-full-front | 前 targetFE 次成功评估的非支配过滤 | **PFref_checkpoint 与检查点指标（主）** |

- PFref_terminal(instance) = ND(∪ arms{C0,C2,C3} × seeds{3} 的 terminal-decision-front)。
- PFref_checkpoint(instance,targetFE) = ND(∪ arms × seeds 的 checkpoint-observed-full-front)，每 (instance,targetFE) 独立构造；禁止复用终态 reference。
- 检查点有效条件：该组全部臂在该 target 均冻结（本设计恒成立，observedFE==target，组内跨度 0<5000）。
- 归一化：每 reference 各自的 ideal/nadir（min/max）；HV 参考 (1.1,1.1,1.1)；objective mapping=[0,1,6]（Cmax,TEC,TWC）。
- 指标：HV、IGD、Spacing、C(PFref,front)、C(front,PFref)、frontSize、minCmax、minTEC、minTWC、runtime（algorithmRunNanos）、actualFE。管线=精确去重→严格 Pareto→归一→HV（fc6_metrics corrected 口径）。

## 检查点分析用途（任务书 §八）

1. 判断候选改善是否只在终态偶然出现（终态获益、检查点不获益 → 预算差异而非机制差异）。
2. 判断 C2/C3 是否存在真实预算敏感性（§九 阈值：≥2 有效检查点、方向反转、HV>2% 或 IGD>10%、≥2/3 seed 一致；否则 MINOR_FLUCTUATION）。
