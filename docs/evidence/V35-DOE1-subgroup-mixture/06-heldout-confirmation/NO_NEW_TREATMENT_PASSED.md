# DOE-1 held-out confirmation: no new treatment passed

确认阶段已经完成：`BASE/T1/T2/T3 × 20_5_4_1/50_5_4_1/100_5_4_1 × 5 seeds` 共 60 条独立 JVM、500000 FE 运行。

三个候选均未达到预注册的 `median ΔCmax >= +2%` 门：T1 为 `+1.7236%`，T2 为
`+1.2324%`，T3 为 `+0.4419%`。因此正式容量不替换，冻结为：

```text
FINAL_SEARCH_MIXTURE = [G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC] = [20,40,20,20]
```

这不是“确认没有开始”的占位说明；完整独立验收见
[`HELDOUT_ACCEPTANCE_REPORT.md`](HELDOUT_ACCEPTANCE_REPORT.md)。
