# P8.1 启动记录

- 启动日期：`2026-08-10`
- 工作包：`P8.1 规范生产基线校正与重新验收`
- 当前状态：`completed`
- 用户决定：正式B0/FM0改为`deterministic_canonical`；`author_actual`移入`A0_AUTHOR_DIAGNOSTIC`，不参加正式消融或参考前沿。
- 运行边界：只运行34标签、2实例、3种子、每次2000 FE的P8-v3工程矩阵；不运行P9或500000 FE。

实施开始时暂时撤回：

```text
ca_ta_engineering_validated=false
ca_ta_scheme_aligned=false
integration_engineering_validated=false
ablation_engineering_validated=false
```

作者四个原文件的冻结SHA-256继续以`docs/evidence/P4.1/SOURCE_FREEZE_BEFORE.csv`和`docs/evidence/P8-v2/author-source-integrity.csv`为准，完成时再次核对。

完成日期：`2026-08-10`。当前正式证据入口为`docs/evidence/P8.1/P8_1_REPORT.md`和`docs/evidence/P8-v3/P8_REPORT.md`。
