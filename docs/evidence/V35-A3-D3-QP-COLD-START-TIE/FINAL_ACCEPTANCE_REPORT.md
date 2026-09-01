# V35-A3-D3：Qp 冷启动贪心并列破平诊断验收报告

日期：2026-08-24  
状态：`COMPLETED_DIAGNOSTIC / NO_PROMOTION`

## 问题与唯一变量

此前 D-100 发现：Q0（Qp 动作与个人档案真实执行、但不进行 TD 学习）在三组 50k 配对运行中有
97.15% 的动作是 `KEEP`。控制器的零表贪心在多个合法动作同分时稳定选取第一个合法动作，因而
`KEEP` 是合理的冷启动风险候选。

本工作包只把该并列规则改为：**当且仅当 `DIRECTIONAL` 合法、且它与当前最大 Q 值并列、且并列
动作数大于一时，优先选取 `DIRECTIONAL`**。其余条件保持 Q0 不变：

- 容量 6 的谱系个人档案、四动作 Qp pbest 与同步 Qg/Qp 仍开启；
- `OBSERVE_ONLY_ALL_CYCLES`：奖励、TD transition 与 Q 表写入必须为零；
- 未启用预热、P/G 冻结、方向教师池、Shift、压力掩码或任何 PDDR 改动；
- `20_2_3_1`、100 粒子、50,000 FE、seed `20260822/23/24` 与同 seed 的 D1/Q0 使用同一显式初始种群。

Q1 只是一项最小因果诊断，不是 DOE、正式消融、论文独立样本或正式机制修复。

## 有效性验收

九条 D1/Q0/Q1 记录均通过各自运行目录的文件级 SHA-256 清单；同一 seed 的三个臂初始种群哈希
一致。每条记录满足：

```text
actualFE = decoderCalls = 50000
front 非空且目标有限
illegalSolutions = duplicateEvaluations = abnormalRepairs = missingSources = 0
```

Q1 的 canonical configuration 已记录 `DIRECTIONAL_IF_TIED`；遥测确认
`trainedTransitions=0`、`rewardSamples=0`，因而本臂没有暗中恢复 TD 学习。

## 动作是否真的改变

Q0 三个 seed 共 30,000 次 Qp 动作中，`KEEP=29,146`（97.15%）；Q1 中 `DIRECTIONAL` 从
237 次增至 2,104 次。说明并列破平确实触发，但仅能在 `DIRECTIONAL` 合法的状态下改变选择，
不能把全部 `KEEP` 动作替换为方向动作。

完整逐 seed 分布见 `03-analysis/q-action-distribution.csv`。

## 预注册判定

稳定改善门为：至少 2/3 seed 同时出现 HV 上升和 IGD 改善，且中位数满足
`ΔHV >= +2%` 或 `ΔIGD <= -10%`。稳定退化门沿用 D-100：至少 2/3 seed 同时 HV 下降且 IGD
变差，且中位数 `ΔHV <= -2%` 或 `ΔIGD >= +10%`。

| 对照 | 同时改善 / 同时退化 | 中位 ΔHV | 中位 ΔIGD | 结论 |
|---|---:|---:|---:|---|
| Q0 → Q1 | 2/3 / 1/3 | +0.8850% | -2.0515% | 未达到稳定改善幅度门 |
| D1 → Q1 | 0/3 / 3/3 | -1.3788% | +16.3038% | Q1 仍稳定劣于固定方向 pbest |

因此正式裁决为：

```text
cold_start_tie_verdict = COLD_START_TIE_BREAK_NOT_CONFIRMED
```

## 科学解释与边界

“零表时第一个合法动作是 KEEP”确实是可观察的实现偏差；定向并列破平也确实减少了这种偏差。
但在本预注册的三组 seed、50k FE 条件下，它没有提供足够强的 HV/IGD 改善，也没有恢复到 D1 的
固定方向 pbest 水平。因此不能将 A2→A3 的退化单独归因于这一条 tie-break，更不能把 Q1 写入
正式 A3/A4、冻结 Jar、DOE 或 4500 正式矩阵。

当前得到的是一个**已否定的最小假设**：冷启动的稳定 KEEP 破平可能参与退化，但不是可被该简单
规则独立修复的充分原因。未裁剪奖励、个人档案-认知领导耦合以及其他已冻结搜索结构仍不能借本
结果被单独修改。

## 证据与后续

- 预登记：[PREREGISTRATION.md](00-preregistration/PREREGISTRATION.md)
- 运行与状态：`02-q1-runs/`
- 全部共同参考指标：[metrics-common-reference.csv](03-analysis/metrics-common-reference.csv)
- 配对判定：[pair-metrics.csv](03-analysis/pair-metrics.csv)
- 机器裁决：[CAUSE_DECISION.md](03-analysis/CAUSE_DECISION.md)
- 顶层文件哈希清单：`evidence-sha256.tsv`

本工作包在此关闭。后续如要研究 Qp，必须另行预注册、明确唯一变量和升级门；不得重跑本 Q1
来挑选更有利的 tie-break 或参数。
