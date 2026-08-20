# P8 集成、消融与工程验收报告（校正版）

- 语义版本：`p8-ablation-v3`
- 正式标签：34；旧控制点：已归档，不计入
- 标签级运行记录：204；完成：204；失败：0
- 精确配置复用记录：42
- 单次最大完整评价数：2000；最小完整评价数：1942
- 非法解总数：0；异常修复总数：0；局部候选完整评价记录：336450
- 工程参考前沿实例数：2

## 五组正式消融

1. `FV0–FV-Full`（7项）：比较规范资源更新、FA引导、独立资源更新、耦合FMW动作及惯性/探索删项。
2. `FM0–FM3`（4项）：在同一deterministic_canonical骨架上依次比较规范无疲劳、疲劳累积、自然恢复和疲劳感知选工。
3. `QP0–QP6`（7项）：比较单pbest、容量6档案、随机四策略、Q-pbest、同步Qg、分块冻结和完整CFVF。
4. `V0–V-Full`（7项）：比较O1–O9、O10–O13、Need工厂选择、Test-and-Apply、上下文、代价信用和FAT上下文。
5. `B0–FULL`（9项）：从deterministic_canonical规范基线逐层叠加疲劳、CFVF、谱系档案、Q-pbest、双Q、邻域和CA-TA。

`A0_AUTHOR_DIAGNOSTIC`仅用于作者缺陷诊断，不进入矩阵或参考前沿；旧`B0R/B0C/B1Q/B2P`属于P8-v2历史证据。

## 工程验收

- `integration_engineering_validated=true`
- `ablation_engineering_validated=true`
- `sampled_reproduction_accepted=false`
- `full_reproduction_accepted=false`

## 结论边界

本报告只证明校正后的开关、桥接、预算和小规模消融矩阵工程闭合。参考前沿由本轮已完成运行合并后冻结，不是理论真值。B0/FM0使用`deterministic_canonical`、实例SUT、显式第一阶段MA/WA、原Qg/PDDR/O1-O9；作者未控路径仅作A0诊断。本轮未执行500000 FE、显著性检验或P9正式实验，因此不得称为论文完整复现。
