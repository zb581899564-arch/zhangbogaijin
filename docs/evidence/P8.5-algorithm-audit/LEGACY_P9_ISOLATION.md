# 旧P9证据隔离说明

2026-08-10形成的单seed和六seed 500000 FE结果同时早于以下当前语义：

1. `formal-hmopso-qgs-v1`运行时参数闭环；
2. 严格PDDR重复目标处理；
3. 正式CFVF认知/社会系数0.6；
4. `fatigue-shift-v1/LEFT_RIGHT`共享解码；
5. P8.5语义标签与影子档案隔离修复。

因此这些结果统一标记为：

```text
legacy_pre_full_algorithm_audit=true
eligible_for_current_reference=false
eligible_for_paper_superiority_claim=false
```

它们仍可用于追溯早期正向信号、reference自贡献问题和性能热点，但不得与当前20k烟测或未来正式矩阵合并计算HV/IGD/C-metric。
