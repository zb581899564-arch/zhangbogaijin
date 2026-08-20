# V35-P19 收口报告：Cmax 生命周期审计（Generation→Admission→Survival→Exploitation）

生成日期：2026-08-13
验收标准：四阶段生命周期审计闭合。范围决定（用户批准方案 A）：**纪录级索引 +
finish() PENDING 收尾扫描**；全候选指纹索引**不实施**（理由见 §4）。
前置阶段产物（保留，不覆盖）：同目录 `CMAX_LIFECYCLE_AUDIT.md`。

## 1. 实现改动（唯一生产改动，纯旁路）

`ZhangBoCmaxAudit.finish()` 新增 `resolvePendingSurvival()` 收尾扫描（`ZhangBoCmaxAudit.java`）：

- `survival==PENDING && !pddrRetained` → `NOT_SELECTED`（进入候选集但从未被 PDDR 选中）；
- `survival==PENDING && pddrRetained`（末轮选中、无下一轮可解）→ 指纹在最终种群 → `YES`，否则 `NO`；
- `summaryText()` 增 `resolvedPendingByFinish=` 计数；`getResolvedPendingByFinish()` 暴露计数。

审计器仍为只读旁路：不复制解、不消耗随机、不参与任何决策；front 与算法行为逐位不受影响。

## 2. 四阶段漏斗验收（20k FULL，20_2_3_1，LIFECYCLE_METRICS.csv）

| 阶段 | 列 | 结果 |
|---|---|---|
| Generation | `generated` | 19/19 全部 true |
| Admission | `admitted` == `enteredCandidateSet` | 19/19 一致（17 true / 2 false） |
| Survival | `nextRoundSurvival` | **0 PENDING**：YES=2、NO=0、NOT_SELECTED=17（收尾解析 17 条） |
| Exploitation | 教师使用列 | 列全填充；本臂末纪录从未被教学（见 §3） |

- `pddrRetained`=2、`personalArchive`=3、`globalArchive`=5、`resolvedPendingByFinish`=17；
- 纪录来源：INITIAL=4、`CA_TA_LITE/N4`=2、FIXED_VNS/O1_O9=13；
- 末纪录：FE 6260、Cmax 196.162（与前沿极值一致）。

## 3. 6750 类教师生命周期复核（新语义）

历史 6750 FE 现象（D-057/P9.1）：20k FULL 的最后 Cmax 纪录只做个人老师、从未做社会老师。
新语义复核结论：

- **100k 主臂**（V35-P10.1 证据）：末纪录 rec56（FE 62657 生成、FE 84304 首教、滞后
  21647 FE）被教 100 社会粒子次/1 代 + 5 个人粒子次/1 代——新语义下现象复现且教师滞后
  量化存在（已在 V35-P10.1 报告登记，top-k 池未消除滞后，如实保留）；
- **本臂 20k**（池开、当前代码）：末纪录（FE 6260）social=0、personal=0、
  `firstTeacherFE=-1`——**从未被任何教师消费**。与历史现象同向：最新纪录难以进入 G1
  社会/个人引导，符合 P10/P10.1 已登记的"Cmax 弱点主因"结论，本报告不新增机制判断。

## 4. 范围决定与残余登记（用户批准）

- **全候选指纹索引不实施**：审计器用途是新全局 Cmax 纪录的生命周期追踪（`observeGenerated`
  只在严格新纪录时建 Record）；对全部评估候选建索引会把 CSV/内存按候选数膨胀（20k 约
  2 万行级），且不增加纪录级四阶段结论的信息量。此决定取代 D-061"全候选索引属 P19"的
  待办口径，正式闭环。
- **legacy 运行路径未挂审计**（`ZhangBoMOHPSOQ` 工厂搜索/VNS 等历史方法）：V35 正式线
  绑定 Table-9 配置恒走 `runFormalHmopsoQgsBaseline`，该路径在 V35 下不可达；登记为
  排除项而非缺陷。
- **枚举清理不做**：`Mechanism.N1_N5` 未使用（N1–N5 以 `CA_TA_LITE/N1..N5` 输出）、
  `Operator.O10-O13` 仅历史邻域可达——保留不动，避免无谓改动。

## 5. 测试钉子（本轮新增，ZhangBoCmaxAuditTest 6 项）

`pddrSelectionResolvesYesNoAndNeverSelectedNotSelected`、`finishResolvesPendingRecordsAgainstFinalPopulation`、
`lineageArchivesMatchSha256PersonalArchive`、`enteredCandidateSetFalseIsImmediatelyNotSelected`、
`teacherUseOnNonRecordLeadersIsIgnored`、`finishDedupesFinalCheckpoint`；集成证据由
`V35P19LifecycleEvidenceTest` 承担（无 PENDING 硬断言 + 漏斗各阶段断言）。

## 6. 证据清单

| 文件 | 说明 |
|---|---|
| `runs/full-20k-20_2_3_1/` | 20k FULL 生命周期母表（cmax-audit-records/curves/summary 等） |
| `LIFECYCLE_METRICS.csv` | 漏斗指标汇总 |
| `evidence-sha256.tsv` | SHA-256 清单 |
| `CMAX_LIFECYCLE_AUDIT.md` | 前置工程诊断（保留） |
