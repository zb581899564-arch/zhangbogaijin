# FIELD_SEMANTICS_AUDIT — V35-FC5-T 100k 字段语义与分母独立审计

> 总控独立复核 · 源码级逐字段核对 · ANALYSIS_CORRECTION_ONLY=true

## 0. 审计范围

- 源码：`java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35Fc5TransferAudit.java` VERSION=FC5_100JOB_TRANSFER_V1
- 旧分析脚本：`second-tier-100k-analysis/analyze_second_tier_100k.py` v1
- 原始日志：`raw/output/100k/100_5_3_1/seed-2026090[1-3]/A2_CFVF,A4_BUDGET_AWARE_CATA/fc5-transfer-*.csv` 等 6条运行
- 字典：`FIELD_DICTIONARY.md`

## 1. 字段真实语义（源码行号）

| 字段 | 源码位置 | 类型 | 粒度 | 语义 | 生命周期 |
|---|---|---|---|---|---|
| `seed` | Representative:c526, Round:c474 | long | 每条Representative / 每轮Round | 主seed | 固定 |
| `cycle` | `recordPddrRound` 参数 cycle | int | 每PDDR轮 | 形式PDDR轮次序号，从1起。A2:3轮，A4:12轮 | 单轮 |
| `fe` | `recordPddrRound` 参数 fe | long | 每轮 | 本轮完整评价数真实FE | 单轮 |
| `representative` | `recordPddrRound:107 labels=E_C/E_E/E_W/E_B` | enum | 每条Representative (=方向标签事件) | 四方向极值标签：E_C(Cmax/obj0), E_E(TEC/obj1), E_W(TWC/obj6), E_B(平衡Phi) | 单轮 |
| `poolIndex` | `winners[index]` | int | 每条 | 该代表在pool内的原始下标 (objectiveWinner/balancedWinner) | 单轮 |
| `source` | `sources.get(poolIndex)` | Source | 每条 | 代表来源PARENT/CATA_APPLY等 | 单轮 |
| `fingerprint` | `ZhangBoQgController.fingerprint:469-470` | String | 每条 | `variables|variablesid|copy(vars)|worker` 四段toString，以`|`分隔，含逗号，唯一标识解的基因型 | 解生命周期 |
| `Cmax/TEC/TWC` | `objectives(pool.get(poolIndex))` | double | 每条 | 三目标值 | 单轮 |
| `pddrScore` | `scores(pool):317-330` `dominatedBy + 1/(dominates+1)` | double | 每条 | PDDR打分，越小越优 | 单轮 |
| `pddrRank` | `scoreOrder:332-342` 稳定排序 | int | 每条 | 按score升序、score相同时按poolIndex升序的排名，1=最好 | 单轮 |
| `poolPresent` | `Representative.csv:244 "true,"` 硬编码 | boolean | 每条 | **恒为true**。观察器只在池内记录代表，不存在pool外代表 | — |
| `pddrSelected` | `selectedSlotByOrder.containsKey(poolIndex)` :110 | boolean | 每条 | 是否被PDDR选中进入下一个工作种群 (rank<=100) | 单轮 |
| `rejectReason` | `:116 SELECTED / PDDR_SCORE_RANK_NOT_SELECTED` | String | 每条 | 未选中时固定为PDDR_SCORE_RANK_NOT_SELECTED | 单轮 |
| `nextPopulationSlot` | `selectedSlotByOrder.get(poolIndex)` :111 | int | 每条 | 若选中，进入下一代物理槽位1..100，未选中为-1 | 单轮 |
| `nextSemanticRole` | `roleForSlot:309-315` | String | 每条 | 槽位对应子群角色 G1_CMAX/G4_BALANCED/G2_TEC/G3_TWC，未选中NONE | 单轮 |
| `qgTeacherUses` / `qpTeacherUses` | `observeTeacherUse:135-136` | long | 每条Representative记录 | 该代表作为Qg/Qp教师被使用的次数。**注意：同一fingerprint可能对应多条Representative记录（不同label或不同cycle的同一解），一次真实教师使用会同时累加到所有仍live的该指纹记录上，导致简单求和会重复计数** | 跨轮累加至观测结束 |
| `teacherUseCycles` | `List<Integer>` | String | 每条 | 被用作教师的cycle列表`;`分隔 | 跨轮 |
| `improvedOffspringCount` | `observeGeneratedOffspring`+`observeEvaluatedCandidate` 延迟结算 :171-189 | long | 每条 | 该代表作为教师产生的、按其标签方向改善(parent→child) 的后代事件数。**是事件数，非去重offspring数** | 跨轮 |
| `retiredAtCycle` | `retireMissing:287-298` | int | 每条 | 该代表从存活工作种群退休的cycle，-1=至观测结束仍存活。机制：每轮用`selectedFingerprints`集合比对liveByFingerprint，不在选中集合中的指纹对应的所有历史记录被置retiredAtCycle=cycle并移出live | 跨轮 |
| `Nmerge/Nunique/Nnd/Roverflow` | `recordPddrRound:86-92` | int/double | 每轮Round | Nmerge=pool.size(), Nunique=目标三元组去重后唯一数, Nnd=严格非支配数([0,1,6]), Roverflow=Nnd/100 | 单轮 |

## 2. 核心审计结论：旧“100%保留率”的分母错误

### 2.1 旧脚本公式（analyze_second_tier_100k.py:544-553, 544-546摘录）

```python
selected = [r for r in subset if r["pddrSelected"]]
retained = [r for r in selected if r["retainedIntoNext"]]  # retainedIntoNext = slot>0 且 retired未立即
rate = len(retained)/len(selected)  # 分母 = pddrSelected数
```

旧报告将此 `P(next | PDDR selected)` 标记为 `pool→next 保留率`，并对W1/W2各标签分别计算，得到 16格全1.0。

### 2.2 正确语义

源码中 `poolPresent` 恒true，意味着**每轮必有4个MergePool代表**。正确分母应为：

- `poolToPddrRate = P(PDDR selected | MergePool representative)` = `pddrSelected / poolPresent(=4 per cycle)`
- `poolToNextRate = P(next | MergePool representative)` = `enteredNext / poolPresent`
- `pddrToNextRate = P(next | PDDR selected)` = `enteredNext / pddrSelected` （旧报告实际计算的就是这个）

旧报告把 `pddrToNextRate` 冒充为 `poolToNextRate`，在 `pddrSelected<poolPresent` 时掩盖了拒绝事件。纠正后：

| 集合 | 旧口径 `pddr→next` | 新口径 `pool→PDDR` | 新口径 `pool→next` |
|---|---|---|---|
| 总体144 label events | 1.0 (138/138) | 0.958 (138/144) | 0.958 (138/144) |
| A4 W2_50K_100K (60 events) | 1.0 (54/54) | 0.900 (54/60) | 0.900 (54/60) |
| seed20260901 A4 W2 | 1.0 (16/16) | 0.800 (16/20) | 0.800 (16/20) |

### 2.3 UNIQUE去重差异

- `DIRECTION_LABEL_EVENT`: 每轮4条，若同一指纹同时是E_C和E_B，则计2次事件。
- `UNIQUE_DIRECTIONAL_REPRESENTATIVE`: 按 `(cycle,fingerprint)` 去重，本次100k中仅1例重复（seed20260901 A4 W1 27 vs 28），故两粒度差异<1条，但为严谨性双粒度并列报告。

### 2.4 Teacher重复累计

`observeTeacherUse` 对 `liveByFingerprint.get(fingerprint)` 的所有历史Representative记录同时 `qg/qpTeacherUses++`。若指纹X在cycle 1和cycle 5各作为E_C被选中且均未退休，则cycle 6的一次真实教师使用会在两条记录上各+1，求和得到2次曝光。纠正脚本采用 `(fingerprint,useCycle)` 去重，取 `max` 多重性作为真实曝光的下界估计。

## 3. 分母契约

见 `metric-denominator-contract.csv`。
