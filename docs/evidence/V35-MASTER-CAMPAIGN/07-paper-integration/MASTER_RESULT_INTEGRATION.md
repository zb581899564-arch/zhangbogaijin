# V35 Master Campaign 与论文结果的接线

本文件只定义数据流；当前不含任何正式数值、排名、显著性或优越性结论。

```text
accepted raw runs
  -> raw-run acceptance
  -> PFref_ablation / PFref_main (separate)
  -> frozen metric master tables
  -> paired statistics and effect sizes
  -> SVG/PDF/PNG figures from frozen tables
  -> traceable paper tables and sentences
```

论文文件应继续遵守 `docs/paper/RESULTS_PLACEHOLDER_CONTRACT.md`。在上游任一项未完成时，
结果位置必须保留 `[PENDING_FORMAL_EVIDENCE]`。

## 允许接线

- 消融章节：只读取 `05-analysis-ablation` 中 A0--A4 pooled reference 的冻结母表；
- 主比较章节：只读取 `06-analysis-main` 中 A0/A4 pooled reference 的冻结母表；
- 方法和实验协议：可引用当前已冻结的 Final 语义、45 实例/20 seed、FM3、公平初群和 FE
  规则，但不能以此推断算法效果；
- 图表：只从有 input/output SHA-256 的分析母表生成，不能手工改数。

## 明确不得接线

不得使用 DOE-1、FC-6、100k/50k preflight、历史 Shift、旧压力语义、A5 或任何未批准外部
算法的结果填充 V35 Final 的 A0--A4 正式比较图表。
