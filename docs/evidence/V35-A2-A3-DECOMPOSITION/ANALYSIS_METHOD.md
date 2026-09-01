# V35-A3-D 离线指标与裁决方法

## 输入边界

本分析只读取 `04-runs/` 内 12 条已完成的诊断运行：4 个固定臂 ×
3 个固定 seed。不会调用 Java 搜索代码、不会重算解码、不会修改任何
运行目录中的前沿、状态或事件日志。

运行前先验证：

- `status=COMPLETED`；
- `fullEvaluations=decoderCalls=50000`；
- `illegalSolutions=0` 与 `duplicateEvaluations=0`；
- 同一个 seed 的四臂 `initialPopulationHash` 完全相同。

## 共同参考前沿与主指标

全部 12 个 `front.csv` 合并后，先按三目标 `(Cmax, TEC, TWC)` 精确去重，
再执行严格最小化 Pareto 过滤，得到唯一共同工程参考前沿。归一化边界来自
所有 12 个原始最终前沿点的逐目标最小/最大值；退化范围使用 `1e-12`。

对每条运行计算：

- HV：归一化空间的精确三维并集体积，参考点固定为 `(1.1,1.1,1.1)`；
- IGD：共同参考前沿到该运行最终前沿的平均最近归一化欧氏距离；
- Spacing、前沿规模和三目标极值：仅解释性输出。

所有目标按最小化处理。主因果判断只看 HV、IGD 及预注册的配对门；不从
Spacing、运行时间或点数推断机制优劣。

## 相邻对照与敏感性

相邻关系为 `D0→D1`、`D1→D2`、`D2→D3`。报告中的相对变化定义为：

```text
ΔCmax = (Cmax_old - Cmax_new) / Cmax_old
ΔTEC  = (TEC_old  - TEC_new)  / TEC_old
ΔTWC  = (TWC_old  - TWC_new)  / TWC_old
ΔHV   = (HV_new - HV_old) / HV_old
ΔIGD  = (IGD_new - IGD_old) / IGD_old
```

所以 `ΔCmax/ΔTEC/ΔTWC/ΔHV > 0` 表示改善，而 `ΔIGD < 0` 表示改善。

为检查共同参考前沿的影响，另对每一个相邻关系的六个前沿独立构造参考集，
重算 HV、IGD，写入 `05-analysis/independent-reference-sensitivity.csv`。这份表
是敏感性分析，不替代共同参考前沿下的主诊断。

## 稳定退化门与裁决

某一步只有同时满足以下条件才叫“稳定退化”：

```text
至少 2/3 配对 seed 满足 HV 下降且 IGD 变差
且 median(ΔHV) <= -2% 或 median(ΔIGD) >= +10%
```

归因规则严格遵循预注册：

- D0→D1 达门，且事件证明档案空、严重 fallback 或无有效个人领导，才可写
  `PERSONAL_ARCHIVE_COLLAPSE`；
- D1→D2 达门且 D0→D1 未达门，才可写 `QP_SELECTION_OR_REWARD`；
- D2→D3 达门且 D1→D2 未达门，才可写 `DUAL_Q_PHASE_SKEW`；
- 其他所有情况均为 `COMPOSITE_BLOCK_UNRESOLVED`。

因此本分析不会因某个单步有负向信号，就把责任归给一个尚未独立拆开的机制。

## 可复算性

运行 `analyze_decomposition.py` 可重建 `05-analysis/` 的 CSV、Markdown 与哈希
清单。脚本只使用 Python 标准库；每个运行目录另保有运行时生成的
`evidence-sha256.tsv`，分析前会逐文件反向复算这12份清单并写入
`05-analysis/run-evidence-verification.csv`；根目录 `evidence-sha256.tsv` 连接本工作包全部文件。
