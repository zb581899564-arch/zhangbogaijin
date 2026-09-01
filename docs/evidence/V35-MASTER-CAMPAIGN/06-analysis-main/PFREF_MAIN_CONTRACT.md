# A0 与 A4 主比较 reference 与统计契约

A0/A4 raw runs **复用** `02-raw-runs/` 的同一 RunKey；本目录不得复制或再执行它们。
每个实例仅在 A0、A4 各 20 条 raw runs 均完成且全配对有效后构造：

```text
PFref_main(instance) = ND(A0 + A4, all 20 frozen seeds)
```

使用同一分析器、同一每实例归一化规则和相同 HV 参考点，参数为：

```text
--algorithms A0,A4
--control A0
--formal
```

`PFref_main` 与 `PFref_ablation` 是不同问题集合的独立 reference。两个指标数值不能混到
同一表，也不能把 A1--A3 的 fronts 静默加入 A0/A4 主比较。将来若用户书面批准外部算法，
必须建立新的 `PFref_formal_all`，并重算相应参与集合内的全部指标。
