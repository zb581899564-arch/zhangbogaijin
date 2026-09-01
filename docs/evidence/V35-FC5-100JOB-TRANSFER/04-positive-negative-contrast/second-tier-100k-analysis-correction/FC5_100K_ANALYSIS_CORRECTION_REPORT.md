# FC5 100K 分析纠错报告 — 总控独立复核签字版

**ANALYSIS_CORRECTION_ONLY=true | newTrainingRuns=0 | algorithmChanged=false | pddrChanged=false**
**PDDR=GLOBAL_ORIGINAL | archive=UNBOUNDED_FULL | mixture=[20,40,20,20] | LS=CA-TA-Lite→inherited LS | FM3 | ShiftMode=NONE**
**日期 2026-08-26 | 总控 复核 | 6条100k运行全部ACCEPTED | evidence-sha256.tsv 反向验证 0失败**

---

## 0. 执行边界

本轮未运行任何新实验，未启动250k，未修改Jar/配置/DOE。仅对 `second-tier-100k-analysis` 的统计口径进行源码级纠错。旧报告 `SECOND_TIER_100K_ANALYSIS_REPORT.md` 保留，增加 `ANALYSIS_CORRECTION_NOTICE.md` 提示。

## 1. 总控独立复核方法（所有汇总数由CSV自动生成，禁止手工抄写）

1. 复算全部关键分子分母（**scope=A4_ONLY** 时 poolPresent=144，pddrSelected=138，rejected=6；**scope=ALL(A2+A4)** 时 pool=180，selected=174，rejected=6）
2. 检查 unique representative 去重（仅1例同fingerprint跨标签，已按 (cycle,fingerprint) 合并）
3. 检查最后一轮right-censor（**A2共12条、A4共10条**已剔除next→nextCycle分母；A4分seed为3/4/3条，见 RIGHT_CENSORING_RULE.md）
4. 检查 teacher exposure 去重（(fingerprint,useCycle) max多重性）
5. 逐条复核6条pddrSelected=false（rejected-representative-events.csv）
6. 反向验证 raw/output 26文件×6的SHA（run-acceptance-recheck.csv 0失败）
7. 抽查seed 20260901 A4 / 20260902 A4 原始csv与FIELD_DICTIONARY行号一致
8. 限制写进本报告 §7

## 2. 纠错后的六段转化链（自动汇总自 directional-retention-corrected.csv）

> **口径声明：** 下表“总体”指 `scope=A4_ONLY, granularity=DIRECTION_LABEL_EVENT, 36个PDDR轮, 144 label events`。如需ALL口径，见括号内。

**总体（A4_ONLY，144 label events，36轮；ALL口径 180 events，45轮，174选中）：**

| 段 | 分子/分母 | 率 | 备注 |
|---|---|---|---|
| pool→PDDR | **138/144** (ALL:174/180) | **95.83%** (ALL:96.67%) | 即pool→PDDR |
| PDDR→next (pddrToNext) | 138/138 | **100%** | 选中即入next |
| pool→next | 138/144 | **95.83%** | 旧报告100%即此分母错 |
| next→nextCycle (eligible, 已剔末轮右删失) | **99/128** (A4_UNIQUE 99/127=77.95%; ALL 115/152=75.66%) | **77.34%** | 原报告69/87不对应任何口径 |
| next→teacher | **117/138** (UNIQUE 116/137=84.67%; ALL 126/174=72.41%, UNIQUE 125/173=72.25%) | **84.78%** | 原报告92/138不对应 |
| teacher→improvement | **77/117** (UNIQUE 76/116=65.52%) | **65.81%** | 原报告54/92不对应 |

**分臂分窗口（UNIQUE维度，自动取CSV）：**

| arm/window | pool | pddrSelected | pool→PDDR | next→nextCycle | next→teacher | teacher→improvement |
|---|---|---|---|---|---|---|
| A2 W1 | 4 |4 |100%|75%|25%|100%|
| A2 W2 | 8 |8 |100%|50-75% avg|25-37%|33-100%|
| A4 W1 | 27-28 |27-28 |100%|67.9-82.1%|85.2-100%|60.9-73.1%|
| **A4 W2** | **20** | **16-20** | **80% (20260901), 100% (20260902), 90% (20260903)** |84.6-87.5%|65.0-81.3%|46.2-76.9%|

> 关键变化：旧报告 pool→next 报告为 100%（实为pddr→next），纠正后 **A4_ONLY pool→next 95.83% (ALL 96.67%)**，A4 W2 为 **80%/100%/90% (20260901/02/03)**。
> 教师改善率的窗口含义为 **代表出生窗口/cohort window**：`improvedOffspringCount` 只有生命周期累计数，无逐次FE，所谓“W1/W2改善率”指“在W1/W2产生的代表，整个生命周期是否产生过方向改善”，而非“改善事件发生在W1/W2”。

## 3. H1 拆分裁决

### H1a ND候选膨胀（仅限100k预算范围内）

- 标准：`Nnd>100` 才叫overflow（TARGET_WORKING_POPULATION=100）
- 证据：36轮（A4_ONLY）/45轮（ALL）maxNnd=76，无任何轮>100，Roverflow max 0.76
- **H1a = NOT_CONFIRMED_AT_100K**（**仅指在100k范围内未确认**，不能表达为普遍否定候选膨胀机制本身；h1a-h1b-corrected-verdict.csv）

### H1b 代表生存与利用断裂（A4_ONLY口径）

- 存在 **6条 unique directional representatives 被PDDR拒绝**（rejected-representative-events.csv），分布仅A4 W2，非A2（ALL口径同样6条，A2为0）。
- next→nextCycle 损失：**A4_ONLY eligible 128 → survived 99，损失29条 (22.66%)**（A4_UNIQUE 127→99；ALL 152→115）；包含自然退休（fingerprint未被下一轮选中）。
- next→teacher 损失：**A4_ONLY entered 138中 117被用，21条未被用作教师**（UNIQUE 137→116；原报告46条为错误汇总）。
- teacher→improvement 损失：**A4_ONLY teacher 117中 77产生改善，40条未产生**（UNIQUE 116→76）。
- **但**：pddrToNext 100%（选中即入next），teacher曝光未断裂（A4 exposuresPerCycle 192-205），Top5集中但未垄断全部。
- 缺少：正例-退化例同预算500k对照的时序对应、checkpoint HV/IGD生命周期、2/3 seed一致的阈值（仅1/3 seed出现80%低值）。

**H1b = LOCAL_FAILURE_EVENTS_OBSERVED_TRANSFER_UNRESOLVED**

- 有局部拒绝事件，不满足 FC5_TRANSFER_CONFIRMED 要求的：正例对照+指标退化前时序+2/3 seed一致+完整前沿生命周期。
- 也不满足 NOT_CONFIRMED（因为拒绝确实存在）。

### FC5 总体

**FC5_TRANSFER = INCONCLUSIVE_AFTER_DENOMINATOR_CORRECTION**（H1a未确认，H1b局部可观但无法与500k退化时序挂钩，因果Claim不允许）

## 4. Checkpoint HV/IGD 可计算性

`checkpoint-metric-availability.csv`：**NOT_COMPUTABLE_FROM_CURRENT_100K_ARTIFACTS**

- 现有工件：每run仅1个 `front.csv`（最终前沿）、`cmax-audit-curves.csv`（Cmax标量）、4代表、archiveWorkingGap极值。
- 无每PDDR cycle的完整三目标前沿向量 → 无法用固定500k reference（100_5_3_1.csv, 757点, ideal/nadir已计算）重算生命周期HV/IGD。
- 不得以4代表或Cmax曲线近似。

## 5. 250k 是否批准

**250kApproved = false**（默认）

不满足批准5条件：
1. H1b未出现跨seed一致早期信号（仅1/3 seed 80%）
2. 现有日志已能回答“是否ND>100”（已否定）
3. 250k要回答的二元问题未写死（缺少“next→nextCycle在退化形成区间的行为”字段定义）
4. 所需字段（完整checkpoint前沿、完整MergePool分数表）尚未确认可落盘
5. 运行数与升级条件未预注册

应先提交 `250K_VALUE_OF_INFORMATION.md`（见下节），待字段补齐后再预注册最小250k。

## 6. 证据完整性与停止条件

- 6条运行全部ACCEPTED，同seed A2/A4初群一致（run-acceptance-recheck.csv）
- 日志字段与源码语义一致（FIELD_SEMANTICS_AUDIT.md）
- 分母可唯一确定（metric-denominator-contract.csv）
- teacher已去重
- 未用近似前沿算HV/IGD
- 无修改PDDR/算法诉求 → 未触发停止

## 7. 文件清单（evidence-sha256.tsv，17项）

见 `evidence-sha256.tsv`（**17项数据文件清单全部哈希匹配**（目录共18文件含清单自身），逐项SHA匹配，反向验证0失败，由CSV自动生成禁止手工抄写）。

---

## 8. 12问回答（普通人语言）

1. **原来“100%保留”错在哪里？** 把“选中才算保留”当成了“池子里就有就保留”。分母少算了被PDDR直接拒绝的6个（A4_ONLY 144→138，ALL 180→174）。
2. **真实pool→PDDR保留率？** **A4_ONLY 95.83% (138/144)**（ALL 96.67% 174/180）；A4 W2平均90%（80%/100%/90% per seed）。
3. **有多少被PDDR拒绝？** 6个unique方向代表，全部A4 W2，A2为0（DIRECTION_LABEL_EVENT同样6条）。
4. **有多少入下一代后又在下一轮消失？** **A4_ONLY eligible 128中 29条未存活 (77.34%)**（UNIQUE 127→99=77.95%；ALL 152→115=75.66%；原报告69/87=79.3%不对应任何口径）。
5. **有多少真正被当教师？** **A4_ONLY 入next 138中有117被用 (84.78%)**（UNIQUE 137→116=84.67%；原报告92/138=66.7%不对应）。
6. **教师是否被垄断？** 全部Qg教师Top1 3-16%不垄断；但方向代表教师高度集中：20260901 W1单解73.8%、20260903 W2单解93.9%垄断（去重后真实暴露）。
7. **被拒代表是否仍有等价解保留？** 日志未持久化完整选中集，无法判定，标记 UNAVAILABLE_NOT_PERSISTED。不等于archive极值仍在。
8. **100k是否ND>100？** 否，max 76，0轮>100。
9. **能否重算HV/IGD生命周期？** 不能，无checkpoint完整前沿，`NOT_COMPUTABLE_FROM_CURRENT_100K_ARTIFACTS`。
10. **FC-5现在确认/否定/未解决？** H1a=NOT_CONFIRMED_AT_100K（仅100k范围内未确认），H1b=LOCAL_FAILURE_TRANSFER_UNRESOLVED，总体INCONCLUSIVE_AFTER_DENOMINATOR_CORRECTION。
11. **现在值得跑250k吗？** 不值得，先补字段（`250kApproved=false` 维持）。
12. **下一步最小可反驳实验是什么？** **不跑500k影子**：用 **2k/20k 轻量验证**确认 `fullMergePoolScoreLedger + checkpointFullFrontVectors` 可落盘且行为等价（front/Q/事件流一致），再预注册 **A2+A4配对**的最小250k对照（含正例实例对照，因单A4无法证明迁移），明确二元问题“pool→PDDR是否早于gap扩大且与A2对照分离”。当前 `nextExperiment=NOT_YET_PREREGISTERED`。

