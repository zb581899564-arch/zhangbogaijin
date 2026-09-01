# SECOND TIER 100K ANALYSIS REPORT — V35-FC5-T 第二档 100k 筛查

> 由 `analyze_second_tier_100k.py` 生成。Luna C 独立分析；只读源数据，不改算法/PDDR。
> 字段语义以源码为准（`FIELD_DICTIONARY.md`）。不读取任何人的文字结论。

------------------------------
## 0. 数据与哈希验收

- 数据就位：`E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-FC5-100JOB-TRANSFER\04-positive-negative-contrast\second-tier-100k-analysis\raw\output\100k`
- 运行验收与 evidence 反向复算：`run-acceptance-recheck.csv`
- 已验收（属预注册 FC5 A2/A4 对照集且 maxFEs==100000）运行数：**6**
- 说明：预注册 A2/A4 100k 运行集，已验收 6 条。

- **初群哈希一致性**：逐运行比对 `initial-population.sha256` 的 v35 与 `status.properties.initialPopulationHash`（见 recheck 列 initialHashMatch）。
- **evidence 反向验证**：对每条运行目录的 `evidence-sha256.tsv` 逐文件重算 SHA-256+字节。

------------------------------
## 1. 字段口径

全量字段语义见 `FIELD_DICTIONARY.md`。关键口径：
- `Nmerge`=池大小；`Nunique`=目标三元组去重后唯一数；`Nnd`=严格非支配数（`[0,1,6]`，同目标不互支配）；`Roverflow=Nnd/100`。
- 四方向代表：`E_C`(Cmax/obj0)、`E_E`(TEC/obj1)、`E_W`(TWC/obj2)、`E_B`(三目标平衡-PDDR)。
- `poolPresent` 恒为 true；`pddrSelected`=选中进下一工作种群；`nextPopulationSlot/nextSemanticRole`=槽位与子群角色；`retiredAtCycle`=退出存活集（-1=至观测结束仍存活）。
- `cmaxGap=workingBestCmax-archiveBestCmax`（>0=工作比档案差）；tecGap/twcGap 同理。

**W1/W2 定义**（筛查侧，与 Java 固定 50k 桶不同）：
- **W1 = [0,50000]**：`fe ∈ (0,50000]`（观察完整）。
- **W2 = [50000, actualFE]**：`fe ∈ (50000, actualFE]`。
- 因 Phase-Consistent Budget Termination，`0<actualFE=decoderCalls<=100000` 且 `0<=remainingFE<qPhaseFE=5000`，故实际 W2 跨度<50000：任一 `actualFE<100000` 的运行 （尤其 A4）W2 一律标记 `PARTIAL_SECOND_WINDOW`，**不冒充完整 50k 窗口**。
- **禁止**把 W1 与 W2 拼成 100k 连续轨迹：100k 是独立预算实验，MaxFEs 影响 A4 预热/Pacing/预算调度。

------------------------------
## 2. W1/W2 窗口结果

**Nnd / Roverflow 按 (arm, window) 聚合**（A2=3 个 PDDR 轮、A4=12 个 PDDR 轮，A2/A4 因正式外循环结构不同，轮数不同，跨臂数字不严格可比）：

| arm | window | rounds | medianNnd | maxNnd | medianRoverflow | maxRoverflow |
| --- | --- | --- | --- | --- | --- | --- |
| A2 | W1 | 3 | 49 | 59 | 0.49 | 0.59 |
| A2 | W2 | 6 | 60.0 | 70 | 0.6 | 0.7 |
| A4 | W1 | 21 | 38 | 65 | 0.38 | 0.65 |
| A4 | W2 | 15 | 71 | 76 | 0.71 | 0.76 |

完整逐轮见 `per-round-overflow.csv`，按窗口见 `windowed-overflow.csv`；所有 W2 均标记 `PARTIAL_SECOND_WINDOW`（actualFE<100000）。

------------------------------
## 3. 四方向代表生命周期

**pool→next 保留率（按 arm/window/方向）**——全部为 1.0（100%），未见代表被挤出：

| arm | window | representative | retentionRate |
| --- | --- | --- | --- |
| A2 | W1 | E_B | 1.0 |
| A2 | W1 | E_C | 1.0 |
| A2 | W1 | E_E | 1.0 |
| A2 | W1 | E_W | 1.0 |
| A2 | W2 | E_B | 1.0 |
| A2 | W2 | E_C | 1.0 |
| A2 | W2 | E_E | 1.0 |
| A2 | W2 | E_W | 1.0 |
| A4 | W1 | E_B | 1.0 |
| A4 | W1 | E_C | 1.0 |
| A4 | W1 | E_E | 1.0 |
| A4 | W1 | E_W | 1.0 |
| A4 | W2 | E_B | 1.0 |
| A4 | W2 | E_C | 1.0 |
| A4 | W2 | E_E | 1.0 |
| A4 | W2 | E_W | 1.0 |

链路：池存在→PDDR 选中→next 种群保留→槽位/语义角色→Qg/Qp 教师使用→方向改善后代；逐轮明细见 `directional-lifecycle.csv`。所有选中代表均 `retainedIntoNext=True`。

------------------------------
## 4. archive-working gap

**cmaxGap 时序与里程碑（按 seed/arm）**：

| seed | arm | firstRepLossFE | firstCmaxGapExpandFE | cmaxGapMax | cmaxGapMaxFE | tecGapMax | twcGapMax | firstNndGE90FE | firstNndGT100FE |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 20260901 | A2 | 64495 |  | 0.0 | 32295 | 706.0190766979649 | 0.0 |  |  |
| 20260901 | A4 | 13491 | 56932 | 5.93612298365224 | 65313 | 2697.984101158174 | 5149.6283752491 |  |  |
| 20260902 | A2 | 64472 |  | 0.0 | 32276 | 0.0 | 0.0 |  |  |
| 20260902 | A4 | 13491 | 20281 | 3.650358667049886 | 34238 | 964.7487964173051 | 6876.0082791202 |  |  |
| 20260903 | A2 | 64493 |  | 0.0 | 32295 | 295.6075022977311 | 0.0 |  |  |
| 20260903 | A4 | 13491 | 13491 | 5.570080503054214 | 96025 | 1226.6171757262782 | 8487.949670540576 |  |  |

- A4：`cmaxGap` 多数轮次为 0，但各 seed 在 fe≈13491–56932 首次出现正向扩大，并随后达到峰值（3.65 / 5.94 / 5.57，见 cmaxGapMaxFE）；代表损失均在 fe=13491，故 gap 于代表损失后扩大；`firstNndGE90FE`/`firstNndGT100FE` 为空 = Nnd 全程未达 90/100。
- A2：`cmaxGap` 全程 0（工作不劣于档案）；代表损失发生在 fe≈64472–64495（近末期）。
- 完整时序见 `archive-working-gap.csv`。

------------------------------
## 5. 教师利用

**教师曝光（= qg+qp）与改善后代（按 arm/window，total 粒度）**：

| arm | window | granularity | teacherExposure | improvedOffspring |
| --- | --- | --- | --- | --- |
| A2 | W1 | total | 68 | 115 |
| A2 | W2 | total | 42 | 368 |
| A4 | W1 | total | 23806 | 7588 |
| A4 | W2 | total | 3693 | 1245 |

- A4：教师曝光与改善后代在 W2 明显低于 W1（如 seed 20260901 E_C 3000+→1062、improved 1146→328；E_W 4945→48）。注意 W2 实际轮数更少、且 W1 创建的代表存活更久（使用按创建窗口归属），会放大 W1/W2 差距；此为归属口径提示，非严格因果。
- A2：教师使用以 Qg 为主（A2 无 Qp/CA-TA），曝光较低；部分方向在 W2 出现较大改善后代 （如 seed 20260901 E_C improved=275）。
- 完整见 `teacher-utilization.csv`。

------------------------------
## 6. 三 seed 一致性

**同 seed 的 A2 vs A4 配对**（实例 100_5_3_1，seed 20260901/02/03）：

| seed | a2MaxNnd | a4MaxNnd | a2RoverflowW2 | a4RoverflowW2 | a4RetentionW2 | a4MaxCmaxGap | a4FirstLossFE |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 20260901 | 59 | 73 | 0.505 | 0.704 | 1.0 | 5.93612298365224 | 13491 |
| 20260902 | 70 | 76 | 0.67 | 0.658 | 1.0 | 3.650358667049886 | 13491 |
| 20260903 | 61 | 73 | 0.57 | 0.668 | 1.0 | 5.570080503054214 | 13491 |

- 三 seed 均 **未出现 Nnd>90**（A4 W2 maxNnd = 73 / 76 / 73），Roverflow W2 中位升高（0.66–0.70）但仍在 1 以内；A4 保留率三 seed 均 100%。
- 三 seed 一致：A4 代表损失发生在 fe≈13491（早），A2 在 fe≈64472–64495（晚）；A4 cmaxGap 三 seed 均在后半段转正（3.65 / 5.94 / 5.57），A2 全程 0。
- 完整见 `seed-paired-contrast.csv`。

------------------------------
## 7. 情形 A/B/C 判定

**预注册判据（H1-100k）**：
- **情形 A** `FC5_TRANSFER_STRONG_SIGNAL_AT_100K`：五条**同时**满足。① ≥2/3 seed 在 W2 出现 `Nnd>100`；② 中位 Roverflow 继续明显上升（W2−W1≥`0.05`）；③ ≥一种方向代表 `pool→next` 保留率相对 W1 下降≥`20`pp；④ gap 在代表损失后扩大；⑤ 教师曝光或改善后代同步下降。
- **情形 B** `FC5_TRANSFER_NOT_CONFIRMED_THROUGH_100K`：全部运行满足 `maxNnd<90` 且 `pool→next` 保留率≥95% 且 `cmaxGap≈0` 无扩大 且教师链路未断裂。
- **情形 C** `FC5_TRANSFER_100K_INCONCLUSIVE`：其余情况，含 `90≤Nnd≤100`、仅 `1/3` seed 超 100、或 Nnd 增加但代表仍被保留利用。

**本批判定**：
当前判定：**FC5_TRANSFER_100K_INCONCLUSIVE**（已验收 6 条 / 3 seed）
逐条条件：A_c1_ge2of3_nnd_gt100=False；A_c2_rov_rise=True；A_c3_retention_drop_20pp=False；A_c4_gap_widen_after_loss=True；A_c5_teacher_drop=True；B_all_max_nnd_lt90=True；B_all_retention_ge95=True；B_all_cmax_gap_approx0=False；B_all_teacher_link_intact=True

判定理由：情形 C：既未同时满足情形 A 五条件，也未满足情形 B 全条件。包括：90<=Nnd<=100 / 仅1/3 seed 超100 / Nnd增加但代表仍被保留利用。注意：Nnd 增加但代表仍被保留、教师仍正常利用时，说明候选多本身不是根因，禁止据此修改 PDDR。

各条件布尔与每 seed 信号见 `h1-100k-screening-verdict.csv`。

> **★ 结论边界 ★**：`Nnd 增加但代表仍被保留、教师仍正常利用时，说明候选多本身不是根因，禁止据此修改 PDDR。` 100k 为独立预算实验；W2 部分窗口不冒充完整窗口；W1+W2 不拼连续轨迹。

------------------------------
## 8. 能说什么 / 不能说什么

- **能说**：在本批已验收的 100k A4 vs A2 运行中，FC5-T 观察器给出明确的工程/诊断信号：Nnd 在后半段（W2）较 W1 明显上升（A4 中位 0.41–0.46→0.66–0.70，max 76）但**未超过 90**；四方向代表 pool→next 保留率 100%，无被挤出；A4 教师曝光与改善后代在 W2 明显回落；A4 工作种群在 fe≈56932 后相对档案出现 cmaxGap 转正（峰值 3.65–5.94），而 A2 全程 cmaxGap≈0。这些是**非统计性的单实例/单实例种子工程信号**，不是论文级证据。
- **不能说**：不得据此断言 FULL/A4 算法统计优越；不得据此修改 PDDR、教师池、子群配比、布局预算、局部搜索顺序或任何冻结参数；不得把 100k 当作 250k/500k 或正式矩阵的替代证据。
- **明确不支撑的假设**：`Nnd>100`（候选过量）在本批未出现；因此“候选多导致代表被挤出/教师链断裂”这一 PDDR 根因假设在本批**未得到支持**。

------------------------------
## 9. 是否需要 250k

- run250k = INCONCLUSIVE_SEE_MORE（100k 不确定；需要更多 seed 或更长预算来区分，但需独立预注册。）
- run250k_auto = NO（本脚本/分析绝不自动启动 250k，README 缺省保持不运行。）
- verdict = FC5_TRANSFER_100K_INCONCLUSIVE（情形 C：既未同时满足情形 A 五条件，也未满足情形 B 全条件。包括：90<=Nnd<=100 / 仅1/3 seed 超100 / Nnd增加但代表仍被保留利用。注意：Nnd 增加但代表仍被保留、教师仍正常利用时，说明候选多本身不是根因，禁止据此修改 PDDR。）
- acceptedRuns = 6（已验收且属预注册对照集的运行数。）

---
_脚本版本：analyze_second_tier_100k v1；字段字典：`FIELD_DICTIONARY.md`。_