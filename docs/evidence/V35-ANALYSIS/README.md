# V35 Final 离线分析证据入口

当前目录只保存空白的 `per-run-metrics.csv` schema；没有导入任何历史运行结果，也没有统计结论。

正式矩阵 raw final fronts 与已冻结的 run metadata 齐备后，使用
`tools/v35-analysis/v35_analysis.py` 生成新的、不可与历史诊断混写的输出目录。正式产物必须保留
输入 metadata 和每个 raw front 的 SHA-256，并由人工结合冻结矩阵和停止条件解释；该工具不会生成“优于”结论。
