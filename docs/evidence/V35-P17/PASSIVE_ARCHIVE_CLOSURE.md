# V35-P17 收口报告：Passive Evaluation Archive（只读全评价非支配历史）

生成日期：2026-08-13
验收标准：只读全评价非支配历史。

## 1. 新组件 `V35PassiveEvaluationArchive`（v35 包）

- `observe(已评价解)`：增量 Pareto 维护——被现有成员严格支配的候选不收；新成员支配的旧成员
  即时删除；只存**防御性副本**；
- 只读面：`snapshot()`（不可变列表 + 副本）、`size()`、`getObservedCount()`、`toCsv()`、
  `statistics()`；**没有任何方法能回写算法、消耗随机或参与搜索决策**；
- 挂钩：`ZhangBoMOHPSOQ.setPassiveEvaluationArchive`（与 `setCmaxAudit` 同模式，纯旁路），
  覆盖正式线**全部**评价路径：`evaluateSwarm`（初群 + 每 Q 轮 CFVF/结构化基线全局后代 +
  局部搜索后的群体）、CA-TA 经典、CA-TA-Lite、critical factory swap/insert、O1–O9 固定邻域；
- `V35FairRunner.run(..., boolean attachPassiveArchive)` 重载（旧签名委托 true），
  `writeRecord` 导出 `passive-archive.csv` + `passive-summary.properties`
  （observedCount/archiveSize/retentionRate）。

## 2. 只读隔离证明（硬证据）

同 seed 同初始种群 FULL 20k（20_2_3_1）两臂——挂档案 vs 不挂档案——**front.csv 逐位一致**
（测试断言通过）。档案是纯旁路：不改轨迹、不耗 FE、不产生随机差异。

## 3. 双实例运行时证据（PASSIVE_METRICS.csv）

| 指标 | 20k（20_2_3_1）挂档案 | 20k 不挂档案 | 5k（I1 10_2_2_1）挂档案 |
|---|---|---|---|
| 状态 / FE | COMPLETED / 20000 | COMPLETED / 20000 | COMPLETED / 5000 |
| **observedCount == fullEvaluations** | **20000 == 20000 ✅** | — | **5000 == 5000 ✅** |
| 档案规模 / 保留率 | 381 / 1.905% | — | 98 / 1.96% |
| 档案 pairwise 非支配 | **381/381 ✅** | — | **98/98 ✅** |
| 前沿 / minCmax | 79 / 196.162 | 79 / 196.162（逐位一致） | 64 / 45.667 |

档案规模远小于评价总数（约 2% 保留率）是 Pareto 维护的正常行为：每代 PDDR 候选大多被
已有非支配成员支配，只有真正拓宽前沿的解才入档。

## 4. 执行中的重大发现：审计器（及本档案）原先未观察正式线 CFVF 评估

实现全量喂入时定位到：正式线每 Q 轮的后代评估走 `evaluateSwarm`
（`ZhangBoMOHPSOQ:541`），而审计器的初群钩子带 `evaluationsBefore == 0L` 条件，**只喂了初群**；
20k FULL 里恰好 **5000 次 CFVF 全局后代评估从未进入审计与档案观察流**（observedCount 修复前
15000/20000 证实）。本轮已在 `evaluateSwarm` 对两分支补上非初群的 CFVF/BASELINE_GLOBAL
观察（审计 + 档案同修）。

**对既有结论的更正**（详见 D-069）：
- D-063/P10 与 D-064/P10.1 的"CFVF 零新纪录"是**观察缺口伪影**——CFVF 评估从未被观察，
  "零纪录"并非"CFVF 没有产出纪录"。修复后同臂 20k 实测 `recordSources={INITIAL=4,
  CFVF/CFVF=6, FIXED_VNS/O1_O9=11}`：**CFVF 在 20k 内产出 6 条新 Cmax 纪录**；
- P10.1 报告中"top-k 池不解决 CFVF 零纪录症状"的表述随之失效——该症状本身不存在；
  top-k 池的结论（minCmax −7%、G1 退化缓解、教师滞后未消除）不受影响，因其依据的是
  教师使用与曲线数据，与 CFVF 纪录口径无关。

## 5. 测试钉子

`V35PassiveEvaluationArchiveTest` 4 项（Pareto 维护、防御性副本隔离、快照不可变、
计数与 CSV）；`V35P17P18EvidenceTest`（隔离逐位证明 + 全量观察 + pairwise 非支配）。

## 6. 证据清单

| 文件 | 说明 |
|---|---|
| `runs/full-20k-with/`、`runs/full-20k-without/`、`runs/full-5k-I1-with/` | 三臂运行证据（含 passive-archive.csv/passive-summary.properties） |
| `PASSIVE_METRICS.csv` | 指标汇总 |
| `evidence-sha256.tsv` | SHA-256 清单 |
