# FC-5.2：Evaluated-Candidate → Archive → Final-Output Lifecycle Audit 报告（seed 20260822）

> 运行：20_2_3_1 / 500k / population 100 / seed 20260822 / 正式 PACING 配置（β=0.25:0.65）/ `-Djacoco.skip=true` / 训练机同机串行。
> 插桩：`V35EvaluationSourceContext`（评估来源标记）+ `V35Fc52LifecycleAudit`（候选生命周期追踪），全部纯观察旁路；`ZhangBoIncrementalParetoArchive.java` 一行未改。

## 1. Background

FC-5.1 确认：三个 seed 都存在比最终 archive minCmax 更好的 fully-evaluated 解（seed22: 174.44），且不被最终 front 中任何解支配，却不在最终 front 中。FC-5.2 的任务是用事件链确定这些好解在"评估 → admission → archive → final output"的哪一步消失，并在 A（archive bug）/ B（admission/knowledge-loss）/ C（output provenance）/ D（证据不足）中选一。

## 2. FC-5.1 已知事实（本运行复核）

- best fully-evaluated Cmax = **174.43665028596877**（TEC=11123.47，TWC=15044.46），出现于 **FE=288564**（cycle 41，LS 阶段）。
- fc52BestEverEvaluated = 174.43665028596877 @ fe=288564 —— **与 FC-5.1 的 V35CmaxBestEver 记录完全一致**；
  因 fc52 的出生点全部位于算法侧正式评估路径（problem.evaluate 之外、fullEvaluationCount 已累加之后），
  排除 FC-5.1 潜在的"shadow 诊断评估混入"问题——**174.44 是花过正式 FE 的真实解**。
- 144 次同 Cmax 175.5 以下评估（record 中 Cmax≤175 的 144 条）全部集中在 cycle 41/45 —— 174.x 级在 cycle 41 前从未被正式评估过，是 VNS 后期发现的搜索空间。

## 3. Instrumentation points（全部纯观察，不改决策）

- 评估来源 tag（`V35EvaluationSourceContext`，ThreadLocal + try/finally，不调随机数/不改 FE/不改控制流）：
  L585 initial / L656 Q-round CFVF / L736 final evaluateSwarm；单解点 L4541（GLOBAL_CFVF）、
  旧 CA-TA 与 CA-TA-Lite 回调（CATA_TEST/CATA_APPLY 按 decision 区分）、fixed VNS 与 formal VNS 回调
  （INTRA_FACTORY_VNS）、INTER_FACTORY swap/insert（INTER_FACTORY_LS）、shadow 评估（SHADOW，防御隔离）。
- 出生记录（recordEvaluated）：evaluateSwarm 逐解循环 + 全部 8 个单解评估点，条件 A（Cmax<当前 archive 最小）
  / B（刷新正式 best-ever）/ C（进入 evaluated Cmax Top-20）任一命中即建 record（共 1024 条）。
- 事件记录：local accepted/rejected（含 reason）、merge-pool 物化、PDDR round 结算（score 复制
  `authorScores` 逻辑、rank=selected 中位置）、archive.add 旁路（复制 `weaklyDominates` 判定，
  记录 ACCEPT/REJECT(dominator)/DUPLICATE/REMOVED(remover)）、finish（final presence 按 3 目标精确匹配）。

## 4. Behavior-equivalence evidence

- `V35Fc52LifecycleAuditTest`：同 seed 冻结初始种群，audit OFF / ON / ON 三连跑 → front hash 逐字节一致、FE 一致、审计输出非空且结构完整。
- `V35Fc1ModuleTimerTest` / `V35Fc5CmaxLifecycleAuditTest` 回归通过（5/5）。
- 本运行与首跑（runs5）关键数完全一致（tracked=1024、accepted=86、rejected=934、pool=38、survive=18、
  archive accepted=10、final=0、bestEver=174.4366@288564）——插桩多次运行确定性可复现。

## 5. Final front provenance（代码级）

- `getResult()`（ZhangBoMOHPSOQ:8474-8476）直接返回 `globallyOptimalIndividual`（唯一外部 archive，无过滤排序）。
- runner（ZhangBoV35P25EBudgetDiagnosticRunner:113-118）投影 {obj0,obj1,obj6} → `P8MetricCalculator.nondominated` → front.csv。
- 结论：**final front 的每一行都来自外部 archive**；final front ⊆ archive（ND 过滤只删被支配行）。
- 本运行：finalArchiveSize=622（= archive 原始行数），finalFrontSize=622（ND 后一致，archive 自维护为弱非支配）。
- **fc52FinalPresent=0**：1024 个被追踪好解，最终一个也不在 front。

## 6. Intended archive semantics

源码与现有文档未明确写明"archive 应观察全部 evaluated candidates 还是仅 selected population"，
按代码事实报告为：
- **INTENDED SEMANTICS NOT EXPLICIT**（文档未声明）；
- 代码事实：archive 唯一增量写入 = `updateParticlesMemory`（L8426-8430），每 cycle（Q 轮 + 尾部）
  把每槽 `tempSwarm.get(k).get(size-1)`（槽位个人历史修剪后的末尾元素）提交给
  `ZhangBoIncrementalParetoArchive.add(globallyOptimalIndividual, ...)`。
  → **archive 只观察（每槽一个的）种群成员及其个人历史末尾解；局部候选若未进入种群则永不被 archive 观察。**

## 7. Actual archive semantics

与上述一致，且判定全程合法：
- 217 次被追踪解的 archive.add 观察：ACCEPT 10、REJECT(DOMINATED) 91、REJECT(DUPLICATE) 116；
- **dominator 合法性全检：0 例不合法**（每个 dominator 均三目标弱支配且至少一维严格小）；
- **remover 合法性全检：0 例不合法**（10 个被移除的 tracked 成员，其 remover 均严格支配它们）；
- 无 archive clear/rebuild/replace 路径（grep 确认仅 3 个写点：初始化、initializeLeader 裸播种、updateParticlesMemory）。
- **结论 A（archive implementation bug）排除。**

## 8. Lifecycle funnel（1024 tracked 候选）

| Stage | Count |
|---|---:|
| Fully evaluated tracked | 1024 |
| Local accepted（ACCEPTED） | 86 |
| Local rejected（NOT_BETTER 924 / NO_RECOVERY_GAIN 10） | 934 |
| Entered merge pool（pendingCaTaLocalCandidates 物化） | 38 |
| Entered next population（PDDR survive） | 18 |
| Archive.add called（tracked 视角） | 217 |
| Archive accepted | 10 |
| Archive rejected (dominated) | 91 |
| Archive rejected (duplicate) | 116 |
| Archive never observed | 1008 |
| Later removed from archive | 10 |
| Present in final output | **0** |

source 分布（1024 条）：INTRA_FACTORY_VNS 967（94.4%）、CATA_TEST 35、CATA_APPLY 18、INTER_FACTORY_LS 4 ——
**优秀 Cmax 解几乎全部由 intra-factory VNS 产生**（与用户规范预测一致）。

## 9. 174.44（best-ever）完整生命周期（record 655）

```
fe=288564  cycle=41  qRound=-1  lineage=1472   [birth]
  source       = INTRA_FACTORY_VNS
  Cmax/TEC/TWC = 174.43665028596877 / 11123.47 / 15044.46
  local        = ACCEPT:ACCEPTED        （G1 组 Cmax 改善判定通过）
  ↓
  mergePool    = yes                    （进入 pendingCaTaLocalCandidates 并在 PDDR 物化）
  ↓
  pddr         = REJECT:score=1         （PDDR 三目标评分排序被挤出前 100）
  ↓
  archive      = never                  （从未被 archive.add 观察）
  ↓
  final        = no
```

**174.44 没死在任何"门"的 bug 上——它在 PDDR 的评分排序中排名靠后而被环境选择淘汰。**

## 10. PDDR 评分机制（决定性证据）

`ZhangBoEvaluatedPddrSelector.authorScores`：`score = dominatedBy + 1/(dominates+1)`，升序取前 100。
**score 越小越好，"能支配最多解的综合均衡点"score 可低至 0.02；而 Cmax 极值解（TEC/TWC 边缘、
不支配任何候选）score = 0 + 1/(0+1) = 1，排名反而靠后。**

概验（审计旁路对同轮全部候选的 score 复算，已用 `V35Fc52PddrSelectProbeTest` 证明 select 对
score=1 解在排入前 100 时必被选中）：
- 174.44 所在 PDDR 轮（fe=291213）：Cmax<200 的 24 个候选中有 21 个 score<1（最小 0.022），全部被选；
  **174.44 score=1（第 24 名）被挤出前 100** —— select 行为与 score 完全自洽，REJECT 是真实的算法选择。
- 全程 910 次 Cmax<200 的 tracked 结算判定：score<1 被选 688 次、被拒 66 次（排在更小 score 之后）；
  **score=1 仅 1 次被选、4 次被拒（含 174.44/175.22/187.51/188.13，全部 Cmax 极值点）**。
- FC-5 漏斗交叉验证：cycle 41 swarm 视角 Q-round 后 bestCmax=194.27 → PDDR 后 nextPop=196.08
  （恶化）——PDDR 淘汰了 194.27 的均衡最优，174.44 从未进 swarm，因此四层漏斗对"未入群好解"不可见。

## 11. Top-20 excellent-Cmax 命运表（evaluated Cmax 前 20）

| # | Cmax | TEC | TWC | source | fe | fate |
|---|---|---:|---:|---|---|---|
| 1 | 174.4367 | 11123.47 | 15044.46 | INTRA_FACTORY_VNS | 288564 | local=ACCEPT, mergePool=yes, **pddr=REJECT:1**, archive=NEVER, final=no |
| 2–15 | 174.4367×14 | 11123.47 等 | 15044.46 等 | INTRA_FACTORY_VNS | 288589–288751 | local=REJECT（NOT_BETTER，同 Cmax 无改善被拒）, archive=NEVER, final=no |
| 16–18 | 174.7591×3 | 11100.75 | 15023.25 | INTRA_FACTORY_VNS | 288600–288663 | local=REJECT, archive=NEVER, final=no |
| 19 | 174.9143 | 11119.68 | 15141.54 | INTRA_FACTORY_VNS | 288553 | local=ACCEPT, archive=NEVER, final=no |
| 20 | 174.9143 | 11100.66 | 15006.30 | INTRA_FACTORY_VNS | 288562 | local=REJECT, archive=NEVER, final=no |

Top-20 无一进入 archive/front。**这是系统性的：所有优良 Cmax 极值解都被同一道 PDDR 排序挡在种群与 archive 之外。**

## 12. Root-cause classification

**结论 B：Admission / knowledge-loss problem（PDDR 环境选择环节）。**

- 非 A：archive.add 及其删除无任何不合法判定（dominator/remover 全检 0 例外）。
- 非 C：final output 直接是 archive 的 ND 投影，archive 内容与输出一致（622=622）。
- 机制：fully-evaluated 的 VNS 好解**已被 local acceptance 接受**（86/97 的 tracked 解），但
  **PDDR 的评分公式 `dominatedBy + 1/(dominates+1)` 天然把"Cmax 极值、TEC/TWC 边缘、不支配他人"的点排在
  "综合均衡、支配大量候选"的点之后**，导致优秀 Cmax 解在"评估结果 → 种群 admission"之间丢失，
  而 archive 又只观察种群成员 → 好解全程 invisible。
- 即用户规范结论 B 的精确情形："**已付 FE 得到的 Pareto knowledge 与 population admission 绑定过紧**"。

## 13. 候选 FC-6 方向（PROPOSED NEXT STEP，本阶段未实现）

**Evaluated → Archive Observe → Population Selection 解耦**：把"已 fully-evaluated 的非支配好解"在
PDDR 之前（评估完成时）直接提交外部 archive 观察——不改 PDDR、不改 local acceptance、不改
`ZhangBoIncrementalParetoArchive.add`、不改种群选择——只是让"已付 FE 的知识"在种群选中之前就被
archive 记住（无论该存档是否影响后续搜索，至少最终输出能保留这些极值点）。
具体实现形态需 FC-5.2 seed22 结论确认后、由用户裁决 FC-6 时再定（如：对 accepted 局部候选在
pendingCaTaLocalCandidates.add 处旁路调用 archive 观察，且该旁路不影响任何现有决策）。

## 14. 声明

FC-5.2 全程未修改任何算法决策：CFVF / VNS / CA-TA / LS / PDDR / local acceptance /
archive.add（连文件都未改）/ population selection / final output / 随机序列 / FE 均与插桩前一致；
行为等价门（front hash / FE / event 类测试 5/5 通过）证明插桩为纯观察旁路。

## 证据文件

- `runs5-final/seed-20260822-mechanism-summary.txt`（最终版，source 精确标定）
- `runs5/seed-20260822/`（首跑，关键数一致）
- `runs5b/fc52-debug.log`（PDDR score 判定旁路复算，910 条）
- `V35Fc52LifecycleAuditTest` / `V35Fc52PddrSelectProbeTest` / `V35Fc5CmaxLifecycleAuditTest`（行为等价）