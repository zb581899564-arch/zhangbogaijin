# Cmax Audit 小规模诊断

## 技术摘要

本运行只增加旁路观测，不改变算法决策。实例为`20_2_3_1`，seed为`20260808`，种群100，预算20000 FE，每1000 FE保存一次曲线。

- 最终非支配前沿最小Cmax：`201.27874014165087`。
- 产生新的历史Cmax纪录：`11`次。
- PDDR保留：`10`次；进入个人/全局档案：`11`次；下一轮仍存活：`10`次。

## 指标定义

- `BestCmaxGlobal`：截至检查点，全局非支配历史中的最小Cmax。
- `BestCmaxG1`：检查点时当前G1子群的最小Cmax。
- `BestCmaxGenerated`：截至检查点，所有已完整评价候选的历史最小Cmax。
- `BestCmaxSurvived`：截至检查点，至少被PDDR保留一次的纪录最小Cmax。

## 证据文件

- `cmax-curves.csv`：1000 FE粒度曲线。
- `cmax-record-lifecycle.csv`：纪录来源、候选集、PDDR、档案和下一轮存活。
- `mechanism-summary.txt`：机制触发与FE闭合。

## 限制

该结果是单实例、单seed的诊断实验，不构成正式统计或算法优越性结论。
