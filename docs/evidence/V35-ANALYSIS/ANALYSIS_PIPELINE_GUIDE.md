# V35 统一指标与统计流水线

状态：`PIPELINE_READY_NO_FORMAL_RAW_RESULTS`。本指南定义离线复算工具，**不代表**正式矩阵已启动、抽样复现已验收或存在任何算法优劣结论。

## 1. 固定口径

输入仅能是冻结后的 `raw final front CSV` 和其运行元数据 CSV。工具不读取 runner、算法实现、历史指标表或手工汇总数字。一个物理运行的唯一键是：

```text
RunKey = algorithm + config_hash + instance + seed + budget
```

只接收 `status=COMPLETED` 的行，并逐块验证同一 `(instance, seed)` 的所有参与算法具有相同：初始种群哈希、实例/扩展哈希、疲劳清单哈希、预算与正式语义：

```text
FM3; DEGENERATE_SINGLE_FAMILY; SEQUENCE_INDEPENDENT; ShiftMode=NONE; objectives=0|1|6
```

运行中止、失败、历史 Shift-on、`A0_AUTHOR_DIAGNOSTIC`，或任何未经冻结的算法均不可填入参与算法列表。参与算法由命令行 `--algorithms` 显式冻结；该列表和原始 metadata SHA-256 会写进 `analysis-status.json`。

每个实例仅在全部已冻结算法和 seed 都齐全后构造一次：

```text
PFref(i) = ND(union of all selected raw final fronts for i)
```

`ND` 是三目标最小化的严格 Pareto 过滤，容差为 `1e-12`，随后精确近重复去重并稳定排序。归一化上下界来自同一个实例的 `PFref`；零范围分母固定为 `1e-12`。这与 `P8MetricCalculator` 的“reference 决定共享归一化”实现一致。HV 使用归一化参考点 `(1.1, 1.1, 1.1)`。

## 2. 输入 schema

从 [per-run-metadata.template.csv](../../../tools/v35-analysis/per-run-metadata.template.csv) 复制出一次实际 metadata 文件。所有列都必须存在；`wall_clock_ms` 和 `cpu_time_ms` 在诊断模式可以留空，在 `--formal` 下必须为非负有限数。

```text
run_id,algorithm,instance,seed,status,config_hash,budget,
initial_population_hash,instance_sha256,instance_extension_sha256,
fatigue_manifest_sha256,decoder_mode,family_mode,setup_mode,shift_mode,
objectives,front_path,wall_clock_ms,cpu_time_ms
```

`front_path` 相对 metadata 文件所在目录解析。front CSV 至少有 `Cmax,TEC,TWC` 三列（大小写不敏感）；其他列可以保留但不会进入指标。缺列、空 front、非有限目标、重复 `run_id`、重复 `RunKey`、算法×seed 不成矩形，或同 seed 初始种群不一致，均会 fail-closed。

## 3. 运行

先运行隔离的数学与手工小前沿测试：

```powershell
python -m unittest discover -s tools/v35-analysis -p "test_*.py" -v
```

为正式母表创建一个**新的、空的**输出目录。工具拒绝覆盖已存在的非空证据目录：

```powershell
python tools/v35-analysis/v35_analysis.py `
  --manifest docs/evidence/V35-FORMAL-EXPERIMENTS/08_reference_and_statistics/per-run-metadata.csv `
  --output-dir docs/evidence/V35-FORMAL-EXPERIMENTS/08_reference_and_statistics/analysis-v1 `
  --algorithms A0,V35_MAIN `
  --control A0 `
  --formal
```

`--formal` 强制每个实例恰为 20 个完整、成对 seed，并要求两个运行时间字段齐备。没有正式 raw fronts 时不应执行该命令；空 metadata 会直接拒绝，绝不创建伪造指标或统计结果。为验证流水线本身而使用合成/小样本输入时，不带 `--formal`，其 `analysis-status.json` 会明确标注 `NON_FORMAL_DIAGNOSTIC_ONLY`。

## 4. 生成物与指标含义

| 文件 | 内容 |
|---|---|
| `reference-fronts/<instance>.csv/.json` | 单次 PFref、归一化边界、HV 参考点与 SHA-256 |
| `per-run-metrics.csv` | HV、IGD、Spacing、相对 PFref 的双向 C、frontSize、Cmax/TEC/TWC 最小和最大值、wall/CPU time |
| `pairwise-coverage.csv` | 同 `(instance,seed)` 的算法—算法双向 C-metric 与差值 |
| `statistics-pairwise.csv` | 每实例、每个候选对 control 的 Wilcoxon、Holm、胜平负、`A12`、paired Cliff delta |
| `statistics-friedman.csv` | 所有 `(instance,seed)` block 的 Friedman 平均排名、卡方近似 p 值 |
| `analysis-status.json` | 输入哈希、冻结算法集、reference 元数据、状态与无自动结论承诺 |

`per-run-metrics.csv` 的 C 字段遵循 P8 约定：`c_forward=C(run, PFref)`，`c_reverse=C(PFref, run)`。论文两算法覆盖分析必须看同 seed 的 `pairwise-coverage.csv`，而不是把 PFref C 字段错当成算法间覆盖。

## 5. 统计与“正值更好”约定

两个算法在相同 seed 和相同初始种群下成对比较。对每个候选 `T` 与 control `C`，送入 Wilcoxon signed-rank 的值统一是：

```text
HV, frontSize:          T - C
IGD, Spacing:           C - T
Cmax/TEC/TWC minimum:   C - T
wall/CPU time:          C - T
C-metric advantage:     C(T,C) - C(C,T)
```

因此所有测试、胜平负、`A12` 和 paired Cliff delta 的正数都只表示“候选在该指标方向更好”。Wilcoxon 使用带平均秩的 exact sign-sum 动态规划；零差值不进入其秩和，但仍进入胜/平/负和效应量。`A12=(wins+0.5*ties)/n`，paired `Cliff delta=(wins-losses)/n`，两者均显式面向配对改善方向。

Friedman 在每个 `(instance,seed)` block 上对同方向质量做降序排名（1 是最好），输出全实例平均秩。Holm 的 family 是“同一 metric 下全部实例和候选对 control 的假设”；工具只输出校正后的 p 值，不以 p 值自动写出“显著优于”。显著性阈值保持路线图规定的 `alpha=0.05`，但最终解释仍必须由人工核查冻结矩阵、失败运行隔离和效应量。

## 6. 已验证边界

`test_v35_analysis.py` 覆盖严格 Pareto+去重、P8 identity 指标约定、手工单点 `HV=1.1^3=1.331`、exact Wilcoxon 与 effect direction、raw-front 端到端输出和初始种群哈希 mismatch fail-closed。测试使用临时合成前沿，不产出也不暗示正式结果。

当前 [per-run-metrics.csv](per-run-metrics.csv) 只有 header，特意不填任何数值。等正式 raw fronts 齐备后，必须在新目录运行本工具并将该输出作为唯一统计母表来源；增加或替换参与算法后须重新构造 `PFref` 并重算全部指标。
