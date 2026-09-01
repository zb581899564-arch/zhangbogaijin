# FIELD_DICTIONARY.md — FC5-T 首档 50k 遥测字段字典

日期：2026-08-25
来源核对：所有字段语义均直接取自 `V35Fc5TransferAudit.java`
（`张博改进\java-jmetal58\jmetal-algorithm\src\main\java\org\uma\jmetal\algorithm\multiobjective\mypso\v35\V35Fc5TransferAudit.java`，
schema `FC5_100JOB_TRANSFER_V1`）与 `ZhangBoV35Fc5TransferRunner.java`，
**不是**按列名猜测。

## 1. fc5-transfer-summary.properties

| 字段 | 源码行 | 含义 |
|---|---|---|
| `schema` | L28 | 遥测版本，固定 `FC5_100JOB_TRANSFER_V1` |
| `seed` | L264 | 运行主种子 |
| `enabled` | L265 | 观察器开关 |
| `pddrRounds` | L265 | 真实 PDDR 轮数（`rounds.size()`），50k 内 A0/A2 每运行 2 轮（个别 A0 运行 3 轮）、A4 每运行 6 轮（24 条运行合计 74 轮） |
| `representativeRecords` | L266 | 方向代表记录数（= 轮数 × 4） |
| `representativesSelected` | L267 | 被 PDDR 选中的代表记录数 |
| `representativesWithImprovedOffspring` | L268 | 产生过方向严格改善子代的代表记录数 |
| `archiveWorkingSnapshots` | L269 | archive-working gap 快照数 |
| `observerErrors` | L270 | 观察器内部异常计数（0=观察未干扰搜索） |

## 2. fc5-transfer-merge-rounds.csv（L210-218 `mergeRoundsCsv`）

每个真实 PDDR 轮一行。**合并池即 `GLOBAL_ORIGINAL` PDDR 实际排名的同一候选池**
（`recordPddrRound(pool, sources, selected, fe, cycle, ...)`，L74-126）。

| 字段 | 含义 |
|---|---|
| `cycle` | PDDR 轮次（从 1 起） |
| `fe` | 该轮已累计的完整评价次数 |
| `Nmerge` | 合并池物理候选数（`pool.size()`） |
| `Nunique` | 按 `(Cmax,TEC,TWC)` 三目标**精确去重**后的点数（`firstObjectiveTriples`，按目标值位模式分组；`uniqueCount`） |
| `Nnd` | Nunique 中**严格三目标 Pareto 非支配**点数（`strictNondominatedCount(pool, unique)`） |
| `Roverflow` | `Nnd / 100`（`TARGET_WORKING_POPULATION=100`） |

严格性要求（预登记 §4）：不把近重复当完全重复；不引入 crowding、NSGA-III 参考向量或新区域定义。

## 3. fc5-transfer-windowed-merge-overflow.csv（L220-235 `windowedMergeCsv`）

按 `WINDOW_FE=50000` 分桶（`end = ((fe-1)/50000+1)*50000`）聚合：

| 字段 | 含义 |
|---|---|
| `windowEndFE` | 窗口结束 FE |
| `rounds` | 窗口内 PDDR 轮数 |
| `meanNmerge / meanNnd / maxNnd` | 窗口内均值/最大 |
| `meanRoverflow / maxRoverflow` | 窗口内 Roverflow 均值/最大 |

**50k 下每条运行恰好一个窗口** → 预登记判据 1 的"连续两个 50k 窗口"不可观测。

## 4. fc5-transfer-directional-representative-lifecycle.csv（L237-246 `representativesCsv`）

每个 PDDR 轮、每个方向代表一行（4 行/轮）。

| 字段 | 含义 |
|---|---|
| `representative` | `E_C`=argmin(Cmax)（L103-106 按 objectives 0/1/6 与 balancedWinner）；`E_E`=argmin(TEC)；`E_W`=argmin(TWC)；`E_B`=当前 G4 归一化 Chebyshev φ 最小代表（`balancedWinner(pool,min,max)`） |
| `poolIndex` | 代表在合并池中的物理索引 |
| `source` | 该候选来源（`ZhangBoEvaluatedPddrSelector.Source`，如 `O1_O9`/`PARENT`/`GLOBAL_OFFSPRING` 等） |
| `fingerprint` | 四向量指纹（`ZhangBoQgController.fingerprint`），**含逗号的未加引号 Java List 文本，CSV 需特殊解析** |
| `Cmax/TEC/TWC` | 代表三目标值 |
| `pddrScore` | 审计器独立复算的 `dominatedBy + 1/(dominates+1)` 得分（L317-327；仅展示，不代表选择） |
| `pddrRank` | 按 pddrScore 升序的排名（1=最好） |
| `poolPresent` | 恒为 true（代表来自池） |
| `pddrSelected` | 该代表是否被**真实** PDDR 选中（`selectedSlotByOrder.containsKey(poolIndex)`） |
| `rejectReason` | 选中=`SELECTED`，否则=`PDDR_SCORE_RANK_NOT_SELECTED` |
| `nextPopulationSlot` | 选中后的物理槽位（1-based），未选中=-1 |
| `nextSemanticRole` | 槽位语义角色（`roleForSlot`：G1_CMAX/G4_BALANCED/G2_TEC/G3_TWC），未选中=NONE |
| `qgTeacherUses / qpTeacherUses` | 该代表指纹作为 QG/QP 教师被真实使用的次数（`observeTeacherUse`，L128-144） |
| `teacherUseCycles` | 教师使用发生的轮次列表（分号分隔） |
| `improvedOffspringCount` | 由该代表派生、经真实评价后在该代表方向**严格改善**的子代数（`observeEvaluatedCandidate` + `improvesRepresentativeDirection`，L171-194） |
| `lastImprovementFE / TeacherKind / RequestingRole` | 最近一次方向改善的 FE/教师种类/请求角色 |
| `lastTeacherFE / lastTeacherRole` | 最近一次作为教师使用的 FE/请求角色 |
| `retiredAtCycle` | 该代表**指纹**在下一轮 PDDR 选中集合中不再出现而退休的轮次（`retireMissing`，L287-298）；-1=存活。**注意：代表是每轮重选的稳定代表，retire 表示该指纹个体未被下一轮保留，不等于方向代表"丢失"** |

## 5. fc5-transfer-archive-working-gap.csv（L248-254 `archiveWorkingGapCsv`）

每个 PDDR 轮后、档案刷新后采样（`observeArchiveWorkingGap`，L196-208）：

| 字段 | 含义 |
|---|---|
| `workingBestCmax / archiveBestCmax / cmaxGap` | working population 与全局 archive 的最佳 Cmax 及差值（working − archive） |
| `workingBestTEC / archiveBestTEC / tecGap` | 同上（TEC，目标槽 1） |
| `workingBestTWC / archiveBestTWC / twcGap` | 同上（TWC，目标槽 6） |
| `workingSize / archiveSize` | 两群体大小 |

## 6. cmax-audit-curves.csv / cmax-audit-records.csv

- `cmax-audit-curves.csv`：每 1000 FE 采样 `bestCmaxGlobal/bestCmaxG1/bestCmaxGenerated/bestCmaxSurvived` 等（观察旁路，L451 硬门；审计不得改变行为哈希/FE/随机流）。本分析只用 `bestCmaxGlobal` 作性能轨迹。
- `cmax-audit-records.csv`：候选生命周期记录（candidateId/parentId/lineageId/generated/admitted/evaluation/.../pddrRetained/.../fingerprintSha256）。

## 7. status.properties / configuration.txt / initial-population.sha256 / evidence-sha256.tsv

- `status.properties`：`status=COMPLETED`、`decoderCalls`、`fullEvaluations`、`illegalSolutions`、`duplicateEvaluations`。
- `configuration.txt`：runner 版本、`diagnosticKind=FC5_100JOB_TRANSFER_OBSERVER_ONLY`、`preRegistered=true`、instance/seed/arm/population/maxFEs。
- `initial-population.sha256`：`v35`/`p8`/`snapshot` 三段初始种群哈希（同 instance×seed 两臂必须一致）。
- `evidence-sha256.tsv`：运行内每个证据文件的行级 SHA-256 与字节数（`sha256\tbytes\tpath`），用于反向验证。

## 8. 解析注意事项（本分析实现）

1. `evidence-sha256.tsv` 是**制表符**分隔。
2. `directional-representative-lifecycle.csv` 的 `fingerprint` 列是**未加引号且含逗号**的 Java 列表文本 → 不能用普通 CSV 解析；本分析按固定布局解析：前 6 列 + 可变宽 fingerprint + 后 20 列（header 共 27 列，含 `lastImprovementRequestingRole`）。
3. `Nunique` 使用三目标精确去重（目标值位模式分组），与审计器 `firstObjectiveTriples` 一致；`Nnd` 使用严格三目标 Pareto 支配，与 `strictNondominatedCount` 一致。
