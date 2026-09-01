# A0--A4 消融 reference 与统计契约

每个实例仅在该实例的 100 条 raw runs 全部 `COMPLETED` 且 paired groups 完整后构造：

```text
PFref_ablation(instance) = ND(A0 + A1 + A2 + A3 + A4, all 20 frozen seeds)
```

使用 `tools/v35-analysis/v35_analysis.py` 的 frozen raw-front-only 输入接口，参数为：

```text
--algorithms A0,A1,A2,A3,A4
--control A0
--formal
```

输出必须保存在本目录新建的、带输入 manifest SHA-256 的 analysis 子目录；不得覆盖旧
analysis，也不得在 raw fronts 不全、任一 paired group invalid、或出现不同问题实例时生成。

HV 使用每实例统一归一化边界和 `(1.1,1.1,1.1)`；IGD、Spacing、双向 C-metric、前沿规模、
Cmax/TEC/TWC 极值、运行时间均从同一 raw-front manifest 计算。Wilcoxon 只对同一
`(instance,seed)` 配对；多臂场景才使用 Friedman/Holm。统计输出没有自动论文结论权。
