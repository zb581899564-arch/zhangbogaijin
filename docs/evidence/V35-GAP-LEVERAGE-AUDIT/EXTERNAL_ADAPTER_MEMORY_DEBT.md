# EXTERNAL_ADAPTER_MEMORY_DEBT（外部基线适配器内存技术债登记）

- 登记日期：2026-08-30
- 性质：技术债登记，本工作包**不实现**修复；只记录事实、根因边界与修复约束
- 影响域：外部公平基线（NSGA-II-F / SPEA2-F）在 100-job 实例上的 500k 级运行

## 1. 事实链（全部为 V35-GAP-PROBE-V2 实际执行记录）

| 实例 | 尝试 | 堆 | 结果 | 证据 |
|---|---|---|---|---|
| 50_2_3_1 ×4 | attempt 1 | -Xmx4g | OOM（4/4） | logs/gap500-{spea2f,nsga2f}-50_2_3_1-*-…（远端） |
| 50_2_3_1 ×4 | attempt 2 | -Xmx12g | OOM（4/4） | 同上 +2 后缀日志 |
| 50_2_3_1 ×4 | attempt 3 | **-Xmx16g** | **成功**（精确 500000 FE） | 500k-runs/run-GAP500-{SPEA2F,NSGA2F}-50_2_3_1-* |
| 100_5_3_1 ×4 | attempt 1 | -Xmx16g | OOM（4/4） | logs/*100_5_3_1*.log（首轮） |
| 100_5_3_1 ×4 | attempt 2 | -Xmx32g | OOM（4/4） | logs/*-attempt2.log |
| 100_5_3_1 ×4 | attempt 3 | -Xmx56g（串行） | OOM（4/4） | logs/*-attempt3.log |
| 100_5_3_1 ×4 | attempt 4 | **-Xmx100g** | **成功**（精确 500000 FE） | 500k-runs/run-GAP500-{SPEA2F,NSGA2F}-100_5_3_1-* |

失败 attempt 的 partial 目录全部保留于远端 `500k-runs/.partial-*`（重试规则合规）；
成功目录与失败 attempt 通过 runId/attemptId 与日志关联。

## 2. 观测根因（边界内）

- OOM 异常点分散于 `ZhangBoEvaluationObservation.afterEvaluation`（追加时）、
  `ZhangBoFatigueEvaluator.candidate`（小对象分配失败=堆已满）等处——
  分配点不固定，指向**持续性保留**而非单次大分配。
- 外部适配器路径（`V35ComparisonProblemAdapter` + `V35ExactEvaluationBudget`）
  的 `lastEvaluated` IdentityHashMap **无界保留**全部已评价解对象
  （键=解对象，值=四向量指纹串）：100-job 下每解约 10–15KB（四向量列表+
  属性副本+指纹串），500k ≈ 5–8GB 起步，叠加 GC 拷贝与属性副本后远超 56g。
- 对照：A0/A4 路径（`V35FairRunner`）无此无界账本（其预算/观测有界），
  同实例同预算 4g 即可完成——外部 OOM 非算法或问题语义所致，纯属适配层保留策略。
- 100-job 的解规模（100 工件 × 4 向量）使保留量约为 50-job 的 2–3 倍，
  与 16g（50-job 过）/56g（100-job 不过）的边界一致。

## 3. 修复约束（未来实现时必须遵守）

1. **只允许修改保留方式**：`V35ExactEvaluationBudget` 的重复评价守卫可改为
   有界近期窗口（与 `ZhangBoEvaluationObservation.beforeEvaluation` 的
   `MAX_RECENT_IDENTITIES` 清空策略同款先例）或等价的按对象标记；
   观测/输出侧同理。**不得改变算法搜索核心、评价计数语义或 FE 合同**。
2. **必须通过 OFF/ON 行为等价**：修复后须重做 20k OFF/ON 等价
   （行为哈希/FE/前沿逐位一致），并补一次 50k 级配对验证。
3. **重复评价检测语义不得弱化为不可用**：同对象未变再评价的拒绝行为
   必须保持（近期窗口内保证覆盖；跨窗口依赖解对象生命周期）。
4. 修复后比较 Jar 使用独立名称与新 SHA，旧 Jar 966DA3D2… 保留不覆盖；
   需更新 artifact binding 并重跑静态扫描与四类单测。
5. 在修复落地前，外部基线的 100-job 500k 级运行必须使用 -Xmx100g
   （或等效大堆）串行执行；本债不阻塞已完成的 16/16 条 Gap Probe 证据。

## 4. 与 Gap Probe V2 证据的关系

16 条 ACCEPTED 运行的外部数据全部来自 Jar 966DA3D2…（搜索语义与冻结语义
完全一致，OOM 与堆大小仅影响能否跑完，不影响任何已产出前沿的行为语义）；
未来若以修复版 Jar 重跑外部基线，属新 evidence 事件，不得覆盖本批数据。
