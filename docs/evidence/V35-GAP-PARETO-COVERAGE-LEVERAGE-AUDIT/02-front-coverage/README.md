# 02-front-coverage — Agent B 口径登记（V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT-V1）

- 日期：2026-08-31。性质：**只读审计**。本目录所有数字由 `analyze_front_coverage.py` 从只读输入生成（stdout 完整日志见 `run-log.txt`），无手工录入。
- 范围：**front 级证据**（H1/H2 裁决门条件 1–3 的输入）。候选级 PDDR 字段（方向代表 lifecycle / working population 保留率）不在本目录范围内。

## 1. 输入（只读）

- 18 条 250k 运行：`16-remote-250k-runs/sync/seed-<S>/results/run-GAPL250K-<A>-<I>-<S>`，
  S∈{20260916,20260917,20260918}，A∈{C0,C2,C3}，I∈{50_2_3_1(正常),100_5_3_1(困难)}。
- 检查点 FE∈{50000,100000,150000,200000} 取 `checkpoints/checkpoint-<FE>-{decision-front,observed-full-front}.csv`；terminal 取 `checkpoint-fronts.csv` 的 `terminal-*-front` 行。terminal FE=250000（`budget-termination.properties`: requestedMaxFE=actualFE=250000, terminationKind=EXACT_MAX_FE，18/18 运行一致）。
- 归一化参考：`17-250k-reference-and-metrics/terminal-reference-fronts/PFref_terminal_<instance>.csv`。
  脚本重建验证：ND(∪arms×seeds 终态 decision) == 文件（50_2_3_1: 756 点，100_5_3_1: 807 点，match=True 逐一比对）。

## 2. 终态两种 front 与指纹来源

- decision（终态决策前沿）= `checkpoint-fronts.csv` 中 `frontType=terminal-decision-front` 行（**含 candidateFingerprint**）。脚本已在全部 18 次运行中验证其三元组集合与 `front.csv` 完全一致（front.csv 本身无指纹列，仅作内容校验基准）；若不一致会回退到 front.csv+指纹解析并打印 WARNING（本次未触发）。
- observed（终态观测前沿）= `checkpoint-fronts.csv` 中 `frontType=terminal-observed-full-front` 行（含指纹；抽查与 `passive-archive.csv` 行数/内容一致，如 C0/100_5_3_1/20260916 均 960 行）。
- 其余检查点：decision/observed 均取 `checkpoints/` 下独立文件（含指纹），与 checkpoint-registry.csv 的 frontSize 一致。

## 3. 指标口径（全部经 fc6_metrics.py frozen 管线）

- 管线 = fc6 `corrected`：raw 去重 → raw 严格 ND → 用**该实例 PFref_terminal 的 ideal/nadir 统一归一化（不 clamp）**→ HV 参考盒 (1.1,1.1,1.1)（fc6.hypervolume，盒外坐标按盒边界收敛，属 HV 定义的一部分）；IGD 在归一化空间、以**归一化后的完整 PFref_terminal** 为参考集（检查点与终态统一同一参考集——此选择在此登记）。
- 检查点与终态、C0/C2/C3、decision/observed 全部使用同一实例级参考，跨检查点/跨 arm 直接可比。归一化不 clamp，故 HV 可 >1（40 万级 TEC 归一化后前沿可越过参考盒）。
- C_observed_vs_decision = fc6.coverage(observed, decision)（decision 被 observed 弱支配/相等的比例）；C_decision_vs_observed 对称。在归一化空间计算（逐目标正仿射变换不改变支配关系，结果与 raw 空间一致，除 1e-12 epsilon 边界）。
- normalizedNearestNeighborDistance：observedOnly 严格 ND 点到 decision 严格 ND 点在归一化空间的欧氏最近邻距离的均值；无 observedOnly 点时记 0。
- minCmax/minTEC/minTWC：取**该行 frontType 对应 front 的严格 ND 集**的最小值（与 fc6.min_objectives 一致）；decision 行与 observed 行各给本 front 的极值。
- 每行同时给出 HV_decision/HV_observed、IGD_decision/IGD_observed 及全部成对集合指标；同一 (instance,seed,arm,checkpoint) 输出 decision-front 与 observed-full-front 两行（frontType 区分；成对指标两行相同）。

## 4. 集合运算与指纹规则

- observedOnlyNdCount = |fp(observed 严格ND) − fp(decision 严格ND)|；decisionOnlyNdCount 对称。**全部为真实 candidateFingerprint（SHA-256，六向量规范文本）集合运算**，不使用 poolOrdinal/index/文件序号。
- observedOnlyRatio = observedOnlyNdCount / observedNdSize（指纹级）。
- potentialHvRecovery = HV(decision ∪ recovered) − HV(decision)（归一化空间）。candidates 先按**指纹 ∈ decision 指纹集 或 目标三元组与 decision 精确重复**双重排除，再取相对 decision∪candidates 全集严格非支配的点。gapIsMaterial = potentialHvRecovery ≥ 0.02 或 observedOnlyRatio ≥ 0.10（门条件 1 阈值）。
- `checkpointTargetFE=250000` 表示 terminal 行（gap CSV 亦含 terminal 行，供"是否终态事后出现"判定）。

## 5. 数据质量登记（如实）

1. **strictNdSize == observedNdSize 并不成立（89/90 不等）——原因已查明并登记**：fp→triple 严格 1:1（90 个检查点对中 0 违例），但**同一目标三元组可由多个不同候选指纹达成**（六向量不同、目标相同），故指纹级 ND 规模（observedNdSize）≥ 目标级 ND 规模（strictNdSize）。全 90 对共 1150 个 ND 三元组带多指纹，累计超出 2338 个指纹。这是指纹级计数口径的固有结果，非文件损坏；difficult 实例 observedOnly 中约三至六成（部分检查点更高，如 C3@200000 中位数 64/79）是"目标与 decision 前沿重复、仅指纹不同"的候选（逐检查点中位数见 run-log.txt dupObj/objNew 列），因此指纹级 ratio 会**高估**新覆盖，potentialHvRecovery（已双重去重）是更接近真实的 HV 口径。
2. **7/90 检查点的 decision front（原样文件）并非严格 ND**：全部集中在 50_2_3_1 seed20260918 的 C2/C3（C2@50000/100000/200000，C3@50000/100000/150000/200000；最重 C3@50000: 307 行 → 严格 ND 289），无重复行，即文件内含被支配行。所有指标按重算的严格 ND 集计算；困难实例与 C0 无此情况。
3. terminal observed 文件含重复指纹行（同一 fp 最多重复 50 行，960 行 → 520 唯一 fp），去重口径见 §3/§4。

## 6. 与既有证据的一致性校验

- 终态 HV/IGD（decision front，corrected 管线，同一 PFref）与 `17-250k-reference-and-metrics/terminal-metrics.csv` 18/18 完全一致（max abs diff = 0）。

## 7. 产出文件

| 文件 | 行数（不含表头） | 内容 |
|---|---|---|
| front-coverage-timeseries.csv | 180 = 2 实例 × 3 seed × 3 arm × 5 检查点 × 2 frontType | 全部前沿规模/覆盖/HV/IGD 指标 |
| observed-decision-gap.csv | 90 = 2 × 3 × 3 × 5 | observedOnlyNdCount / observedOnlyRatio / potentialHvRecovery / gapIsMaterial |
| c0-vs-c3-hv-curve.csv | 10 = 2 实例 × 5 检查点 | 行=instance×checkpoint，列=3 seed 均值，C0/C2/C3 并排：meanHV_decision/meanHV_observed/meanObservedOnlyRatio/meanPotentialHvRecovery/meanMinCmax_decision/meanMinCmax_observed |
| analyze_front_coverage.py | — | 生成脚本（本身为证据） |
| run-log.txt | — | 脚本完整 stdout（含逐实例 C0 vs C3 对照与最早出现检查点） |

NaN 输出为空串；gapIsMaterial 为 True/False 字面量。
