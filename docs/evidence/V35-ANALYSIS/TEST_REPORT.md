# V35 Analysis Pipeline Test Report

日期：2026-08-22  
范围：离线指标/统计工具自身；不读取、不修改、不解释任何历史或正式运行结果。

## 执行结果

```text
python -B -m unittest discover -s tools/v35-analysis -p "test_*.py" -v

Ran 7 tests
OK
```

覆盖：

- 三目标严格 Pareto、P8 兼容的 `1e-12` 去重口径；
- 与 `P8MetricCalculator` 一致的 identity front（IGD=0、双向 C=1）与 `(1.1,1.1,1.1)` HV；
- 手工单点前沿：`HV=1.1^3=1.331`；
- exact Wilcoxon 符号秩和、`A12` 与 paired Cliff delta 的“正值更好”方向；
- raw final front + metadata 的临时小前沿端到端输出，且状态强制为 `NON_FORMAL_DIAGNOSTIC_ONLY`；
- 初始种群哈希不一致 fail-closed；
- `--formal` 对不足 20 个成对 seed 的矩阵 fail-closed。

## 结论边界

本报告只验证分析工具。`per-run-metrics.csv` 仍是空 header 模板；没有正式 raw fronts，因此没有 HV/IGD/C-metric/统计数值，更没有论文结论。
