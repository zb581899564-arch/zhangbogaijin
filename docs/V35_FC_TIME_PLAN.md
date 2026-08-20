# V3.5-FC-TIME：运行时间收口计划（2026-08-18）

> 前置：D-082（V35-FC 流水线）已把 FC-2 pacing 转正。本计划在 FC-4 与 FC-5 之间插入时间收口阶段，核心任务从"再提高 HV"转向**保住 FC-2 质量收益、把几十倍 CPU 时间砍到正式时间门内**。
> 第一原则：**只灭重复计算，不改任何算法决策**——Q/pacing/CA-TA/PDDR 选择结果/随机数调用顺序/FE 数一律不动。

## 0. 为什么需要这个阶段

`20_2_3_1 / 500k` 下，新版 A4+PACING 相对李明哲基线 QGS：HV +10.9%、IGD −73%、TEC/TWC 双优（见 FC_EXPERIMENTS_COMPLETE_DATA.md 第 8.3 节），质量面已成立；但 CPU 时间约 55 倍量级（跨环境：1092s vs 20s），同机同批次下机制栈 ≈6×、pacing ≈8×——**这是当前最大的论文风险**。时间门不通过，FC-8 正式矩阵（45×20）不得启动。

## 1. 探查结论（2026-08-18，代码级证据）

作为 FC-TIME-1 的"嫌疑清单"，全部来自当前源码树（路径相对 `jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/`）：

| # | 事实 | 证据 | 涉及阶段 |
|---|---|---|---|
| 1 | A4 500k 总墙钟 135.8s，其中解码仅 7.1s——**时间大头在机制栈，不在 FM3 解码** | V35FairRunner decoder 快照 `decoderTotalNanos`；500k-pair mechanism-summary | FC-TIME-1 验证 |
| 2 | 四处支配比较"每无向对重复 4 次三目标比较"（双向各比两次，每次 12 次目标读取；只需 2 次） | `ZhangBoEvaluatedPddrSelector.authorScores`:160-175；`PDDRFFselect`（ZhangBoMOHPSOQ）:7722-7731；`select()` 全局锦标赛:1260-1273；`appendAndPrunePersonalHistories`:8088-8097 | FC-TIME-2-A |
| 3 | 已有"单遍双向"参考实现（仅用于被动存档） | `V35PassiveEvaluationArchive.observe`:26-44 | FC-TIME-2-A 复用模式 |
| 4 | 外部 archive 已是增量式（每加一解两次 O(A) 扫描，非 O(M²) 全量重排） | `ZhangBoIncrementalParetoArchive.add`:11-21 | 无需改 |
| 5 | critical DAG/关键结构每次 N3 现算，未进解码结果缓存；生产调用点仅 2 个 | `V35MacroCandidateGateway.prepareWithEvaluation`:259（N3）；`ZhangBoNeighborhoodSuite.criticalBlock`（O10） | FC-TIME-2-B |
| 6 | CA-TA 候选"整解复制"但派生状态引用共享（不深拷贝），仅重建 attributes map + objectives clone | `V35MacroCandidateGateway.copy`→`DhhfspFourVectorSolution`:69-77→`copyMutableValue`:386 | FC-TIME-2-C（按 profiling 决定） |
| 7 | 18→62 轮成因：LS FE 截断（405k→177k）后每轮 Q 相跑满 50 轮（3100=62×50），62 轮 × 整套后处理 | `runFormalHmopsoQgsBaseline`:575-657；`localSearchFeCeiling`:556-573 | FC-TIME-3 候选 |
| 8 | per-cycle 计时钩子位已确定；P6 事件流已存在 | 主循环 :653-654（插入点）；P6 事件 :659-670；`getZhangBoP6EventStreamHash`:8926+ | FC-TIME-1 |
| 9 | 等价性重放基座可复用 | `V35Fc0PrefinalArchiveTest.replayA4FrontHash`（同 seed 重放 front sha256）；Qg/Qp 事件流哈希原语 | 等价性验收 |
| 10 | 三臂 runner 分离：QGS 用 `ZhangBoV35P25ECorrectedComparisonRunner --algorithm HMOPSO_QGS_F`（V35_BASELINE 纯 Qg）；Legacy/Pacing 用 `ZhangBoV35P25EBudgetDiagnosticRunner`（legacy 不传 budget / pacing 传 `--local-fe-budget 0.25:0.65`） | CorrectedComparisonRunner:37-49,96-98；V35FairRunner:171-192；BudgetDiagnosticRunner:184-222 | FC-TIME-0 |

## 2. 阶段定义

### FC-TIME-0：正式计时（先测准，不改算法）

- 固定 `20_2_3_1 / 500k FE / population=100 / seed=20260822`。
- 同一台机器、同一 JDK、同一 JVM 内存（-Xmx6g）、**单 JVM 串行**。
- 三臂：QGS（CorrectedComparisonRunner）、A4-Legacy（BudgetDiagnosticRunner 空 budget）、A4-Pacing（+`--local-fe-budget 0.25:0.65`）。
- 每臂 warm-up 1 次（不计）+ 正式 3 次，取中位；输出 R1=T_Legacy/T_QGS、R2=T_Pacing/T_Legacy、R=T_Pacing/T_QGS。
- 脚本：`scripts/run-fc-time0-20260818.sh`（本地与训练机两版，同脚本换 WORK_ROOT）。
- 验收：三臂时间中位数、比值 R1/R2/R 入 `docs/evidence/V35-P26/experiments/` 计时报告，注明环境。

### FC-TIME-1：模块耗时账（先查账，后动刀）

- 新增纯旁路计时聚合器 `V35ModuleTimer`：每模块 `calls / totalNanos / avgNanos / percentage`，`System.nanoTime`，不进任何决策路径（与 GIR audit 同源的观察旁路原则）。
- 模块：FM3 full decode / FM3 derived-critical / Qp / Qg / DSCR / CFVF / Inter-factory LS / CA-TA Test / CA-TA Apply / PDDR-FF / Archive / Dominance / Solution copy-repair / Audit-logging / Other。
- 主要插入点：`runFormalHmopsoQgsBaseline` 相位边界（:594 起每步）、`evaluateSwarm`（decode）、`settleOriginalQg`/`settleQp`、`applyV35Dscr`、`updatePositionWithCfvf`、`runFormalInheritedLocalSearch`、`runV35CaTaLiteLocalSearch`、`applyEvaluatedPddr`、`ZhangBoIncrementalParetoArchive.add`、`appendAndPrunePersonalHistories`、`Solution.copy`。
- **per-cycle 记录**（:653-654 输出）：cycleId / FE / archiveSize / frontSize / PDDRTime / ArchiveTime / DominanceComparisons / FM3PreviewCount / CriticalBuildCount / SolutionCloneCount / CycleTime——用于判断"XX 成本随 archive/front 规模增长"类曲线。
- mechanism-summary.txt 追加模块耗时块 + per-cycle 尾部摘要。
- 验收：新测试 `V35Fc1ModuleTimerTest`——∑各模块 ≥95% 总时间；插桩与未插桩同 seed 同 FE 行为哈希一致（纯旁路）。

### FC-TIME-2：语义等价优化（结果完全不变）

硬规则：不改 Q、不改 pacing、不改 CA-TA、不少 Test、不减 FE、不改 PDDR 选择结果、不改随机数调用顺序。只消灭重复计算。

- **A（最高优先）四热点支配去重**：
  - `authorScores`、`PDDRFFselect`、`select()`（:1257-1275）、`appendAndPrunePersonalHistories` 改为"单遍双向 + j 从 i+1 起扫"：每次三目标比较同时结算两个方向（谁支配谁 / 弱支配方向），一次更新双方计数；预计支配计算 −4 倍/−2 倍。
  - `PDDRFFselect` 与 `authorScores` 共用同一打分 `count2 + 1/(count1+1)`，提取共享小工具，保证两边数字一致。
- **B：FM3 critical DAG 缓存**：在 `ZhangBoFatigueEvaluationResult`（解码结果 attribute）上挂"关键结构 memo"；N3 预览与 O10 在同一父解且四向量未修改时复用 analyze 结果；JS/FA/MA/WA 任一修改即失效。N4 不建 DAG 无需改。行为不变（同轮同父解结果确定）。
- **C（按 FC-TIME-1 profiling 决定）**：`Solution.copy()` 的 attributes map 与 objectives clone 精简 / scratch buffer 复用。仅当 profiling 证明占比显著才做。

### 等价性验收（每项优化后必过）

- 复用 `V35Fc0PrefinalArchiveTest` 模式：同 seed（20260822）同 20000 FE 重放，断言：
  - front.csv sha256 逐位一致（`[0,1,6]` 三目标非支配集序列化）；
  - 外层/Qg/Qp 事件流哈希一致（`getZhangBoP6EventStreamHash` / `getQgEventStreamHash` / `getQpEventStreamHash`）；
  - FE 计数一致。
- FC-TIME-2 落点加 500k 级逐位 front 比对（本地单条约 15 分钟）。
- 系列测试：`V35FcTime2DominanceTest`（逐位一致 + 支配比较次数断言下降）、`V35FcTime2Fm3DagCacheTest`、等价性重放测试。

### FC-TIME-3：Quality–Time 拐点（条件触发，仅当代码优化后仍 >10×）

- 只调 `βmin∈{0.25, 0.30, 0.35}`（βmax=0.65 不动），`20_2_3_1/500k × 3 seed`，对比 HV/IGD/Cmax/Runtime 拐点；仍单变量。
- 目的：外层循环 62→45→35 可能把时间近乎减半，代价是 HV 或 IGD 的少量回吐；选拐点而非极值。

## 3. 时间门（研发冻结标准，非学术标准）

| 档位 | 判据（同机 Final/QGS） | 动作 |
|---|---|---|
| 红线 | >10× | 不启动 45×20；继续瘦身（FC-TIME-3 或更多等价优化） |
| 可接受 | 5–8× | 若保持 HV≈+10%、IGD 大幅下降、TEC/TWC 优势，可进 FC-5 |
| 理想 | 3–5× | 论文最舒服区间 |

## 4. 悬置事项处置

- **Soft-Freeze（FC-4）**：默认 `ρ=0`（正式候选不含软冻结）。已在跑的 100-job 结果出后仅作旁证，不阻塞 TIME 阶段；除非 100-job 给出强证据（无 diversity collapse 且 IGD 不退化），否则维持 ρ=0。
- **Cheap-Test（FC-3）**：永久封禁，不再碰。教训写入纪律：CA-TA Test 不是纯开销，它本身在贡献搜索——**不要少 Test，要让 Test 算得更便宜**。

## 5. 时间收口后恢复原路线

```text
当前 A4+PACING
 └─ FC-TIME-0 同机正式计时（R1/R2/R）
 └─ FC-TIME-1 模块耗时账（profilig + per-cycle）
 └─ FC-TIME-2 语义等价优化（A 支配去重 → B FM3 DAG 缓存 → C 按需）
 └─ 等价性验收（front/Q/archive/事件流逐位一致）
 └─ Runtime Gate（>10× → FC-TIME-3；≤8× → 放行）
 └─ FC-5 GIR + Cmax Audit
 └─ FC-6 Cmax 修复（只修一处病因）
 └─ FC-7 最终消融
 └─ FC-8 四规模 Champion Gate（正式对比 9 算法）
```

## 6. 禁止清单（本期新增，叠加 D-082 禁区）

1. 不得为省时间改动任何算法决策项（Q/pacing/CA-TA/PDDR 选择结果/随机序/FE 数）。
2. 不得退回 PT0 proxy 或关闭 FM3 关键结构（FC-1 验收门不变）。
3. 不得在本阶段重开 cheap-test 或 soft-freeze 主线。
4. 等价优化每一项必须过逐位等价验收；"结果一致但没测"不算数。
5. 时间门未过（>10×）不得宣称"时间已收口"或占位论文运行时间图。