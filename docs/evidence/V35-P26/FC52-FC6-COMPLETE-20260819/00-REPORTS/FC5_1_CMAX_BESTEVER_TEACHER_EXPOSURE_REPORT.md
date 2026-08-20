# FC-5.1：Cmax Best-Ever Consistency + Teacher Exposure 审计报告（2026-08-18）

> 实验：20_2_3_1 / 500k / seed 20260822,23,24 / PACING 正式配置 / 本地串行。纯观察旁路（problem evaluate hook + archive-best lineage 计数，行为不变；V35Fc5CmaxLifecycleAuditTest 保持通过）。
> 目的：回答两个问题——(1) seed24 到底有没有 fully-evaluated 的 Cmax<195.70 的解？(2) 每个 seed 的 archive-best Cmax 被 G1/CFVF 实际学习过多少次？

## 1. 两个问题的直接答案

**问题 1：有——三个 seed 都存在 fully-evaluated 且不被最终 front 支配的好 Cmax 解，但它们从未进入最终 archive/front。**

| seed | evaluated 全局最好 Cmax | 该解 TEC/TWC | 出现时机（第 N 次评估 / 50 万） | archive 最好 | 最终 front 是否有解支配它 |
|---|---:|---:|---:|---:|---|
| 22 | **174.44** | 11123.5 / 15044.5 | 288,564（中后期） | 188.39 | **否（0 个支配者）** |
| 23 | **169.63** | 11214.8 / 16192.7 | 219,476（中段） | 175.70 | **否（0 个支配者）** |
| 24 | **191.21** | 10924.7 / 17749.8 | **496,557（运行末期 99%）** | 195.70 | **否（0 个支配者）** |

- seed24 的 191.21 解在第 496,557 次评估（几乎最后一轮）产生：Cmax 比 archive 好 4.5、TEC/TWC 只差约 1%，**且不被最终 front 任何点支配——它本应成为 front 的 Cmax 极端点，却不在 front 里**。
- seed22/23 的好解（174.44/169.63）同样：非支配、不在 front。

**问题 2：exposure 不低，反而极高——"exposure 低"假设不成立。**

| seed | archive-best Cmax（lineage） | 被学习总次数 | 其中 G1 组 | 被选 Qg teacher 次数 |
|---|---:|---:|---:|---:|
| 22 | 188.39（l1863） | 1,290 | 1,075 | 60 |
| 23 | 175.70（l1611） | 23,294 | 20,882 | 1,107 |
| 24 | 195.70（l676） | **34,435** | **33,643** | 1,703 |

seed24 的 archive-best 解被 G1 学习了 33,643 次、被选为 Qg teacher 1,703 次——**曝光充分，但种群仍每轮 257–274、离 195.70 差 60+ 单位**。注意：被学习的 archive-best 是 195.70，而全局真正最好的 191.21 从未进 archive（从未有机会被学习）。

## 2. 对 FC-6 预案的裁决

原预案（G1 Archive-Anchor Teacher Exposure）的两个前提：
1. "好解真实存在于 archive" —— **不成立**：好解（非支配、169–191 级）只存在于"已评估"层面，不在 archive。
2. "exposure 很低" —— **不成立**：archive-best 被学 1.3 万–3.4 万次。

**结论：不批准按原理由实施 G1 Archive-Anchor Teacher Exposure。** 即使把 archive-best 当锚点强化学（exposure 已经极高且无效），也解决不了"更好的 191.21 没进 archive"这个更上游的问题。

## 3. 新病因指向：好解在"评估 → 归档"链路上丢失

已排除：
- Generation（能产生 169–191 级好解）；
- Exploitation/exposure（3.4 万次学习无效）；
- front 支配修剪（最终 front 无人支配这些好解）。

剩余两种可能（下一步区分）：
- **A. Archive 准入/修剪链路**：好解评估后未被插入外部 archive（或插入后被删除）——检查 `ZhangBoIncrementalParetoArchive.add` 与 :1124 的归档选择路径（tempSwarm 平均以上才归档）。
- **B. 局部候选入群失败（Admission）**：好解出自 LS/CA-TA 局部候选，评估后多目标得分竞争失败（PDDR 拒收），从未进入 swarm/archive。

区分方法（FC-5.2，小插桩）：在 evaluated-best 记录时标注评估来源（Q 轮 swarm 评估 vs LS/CA-TA 局部候选评估），并记录 archive.add 的接受/拒绝原因（被谁支配 / 从未提交）。

## 4. 证据

- 运行：`fc5-cmax-audit/runs4/seed-{20260822,20260823,20260824}/`（mechanism-summary.txt 含 fc51 块）
- 代码：`V35CmaxBestEver`（problem 模块静态旁路）、`V35CmaxLifecycleAudit.fc51SummaryText`、problem evaluate hook、archive-best lineage 学习计数