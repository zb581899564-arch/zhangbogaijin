# 03-pddr-utilization — Agent C 产出（只读审计，无最终假设裁决）

生成脚本：`generate_03_pddr_utilization.py`（Python 3.11，只读输入，全部数字由脚本生成）。

## 数据源（绝对路径，均经只读扫描确认）

- **B 源**：`E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-FC5-MIDHORIZON-250K\01-root-cause-analysis\remote-results\`
  （archive-working-gap-events/summary.csv、directional-lifecycle-events/summary.csv；实例 100_2_4_1 + 100_5_3_1，seed 20260901-03，arm A2/A4，budget=250000）
- **C 源**：`E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-FC5-MIDHORIZON-DIAGNOSTICS\`
  各 accepted run 的 `telemetry-pddr-full-ledger.csv`（2k/20k/50k，A2/A4；含 09-real-2k、10-real-20k、18-final-*-final、23-a4-50k、26-final-runtime-jar-validation）。

## 产出与口径

### archive-working-gap.csv（234 行）
B 源事件级复算：源 234 事件行 + `budget=250000` + `sourceCsv` 列。源汇总文件一致性核验：**60/60 组复算中位/最大 gap 与源 summary 最大绝对差 = 0.0**。数据级事实：cmaxGap 中位在多数窗口为 0，非零 gap 集中于晚期窗口（如 100_5_3_1 A2 窗口 250000：中位 6.10、最大 85.00；100_2_4_1 A4 窗口 200000：中位 4.53）。

### pddr-working-population-utilization.csv（64 行 = 16 ledger run × 4 方向）
- `poolEnteredCount` = ledger 中 `semanticRoleBefore==role` 的行数（每行 = 一个进入当轮 PDDR merge pool 的真实候选）。
- `selectedByPddrCount` = `selectedByPddr==true`（旧 schema 世代为 `selected==true`）。
- `enteredWorkingPopulationCount` = selectedByPddr=true 数：该 ledger 语义下 selectedByPddr=true 即被 PDDR 选入下一 working population（核验：每个 run 的 runLevelConsistencyCheck=OK，即 selectedTotal == 100×cyclesObserved，100=working population 规模）。
- **`semanticRoleAfter` 在全部 16 个 run 中恒为 NOT_APPLICABLE，不能作为 working-population 字段使用**。
- 身份：真实 `candidateFingerprint`（新 schema）/`stableFingerprint`（旧 schema），不使用 poolOrdinal/文件序号。
- `hardVsNormalComparison` = **EVIDENCE_FIELD_LIMITATION**：全部 C 源候选级 run 均为 100-job 实例（100_5_3_1 / 100_2_4_1），**50_2_3_1（正常实例）候选级数据在本审计可及的全部数据源中不存在**（对 V35-FC5-MIDHORIZON-DIAGNOSTICS 与 V35-FC5-MIDHORIZON-250K 全目录 grep `50_2_3_1` 零命中）。
- 数据级事实（16 run 汇总）：pool 9534 → selected 3997（41.92%）；G1_CMAX 保留率最低（A4 各预算 0.42 左右），G4_BALANCED 在 50k A4 为 0.3376。

### directional-representative-lifecycle.csv（549 行 = 549 个唯一代表指纹）
B 源 directional-lifecycle-events 按 `fingerprintSha256` 聚合（真实指纹身份）：firstObservedFE/lastSeenFE 取事件 FE 极值；enteredMergePool/selectedByPddr/enteredWorkingPopulation/teacherUsed/offspringImproved 为任一事件为 1；`improvedOffspringCount/teacherExposure` 为 cohort（代表出生窗口）口径，跨窗口取 max 不求和（避免重复计数）。全部字段为源文件真实导出，无 NOT_EXPORTED 字段。
- 数据级事实：A2/100_5_3_1 pool→working 0.8806（同臂 100_2_4_1 为 1.0000）；A4/100_5_3_1 0.9384 vs 100_2_4_1 0.9673；549 代表中 teacherUsed 385、offspringImproved 295。两实例均为 100-job，无正常实例对照。

## 候选级字段可用性矩阵（H1/H2 门条件 3-6 相关）

| 字段 | B 源(250k) | C 源 pddr-full-ledger(2k/20k/50k) | C 源 cata-contribution-events(50k,A4) | 结论 |
|---|---|---|---|---|
| enteredMergePool | **真实**（poolPresent，仅四方向代表） | **真实**（行存在即 pool 成员，全候选） | **真实**（仅 CA-TA 候选） | 真实存在 |
| selectedByPddr | **真实**（pddrSelected） | **真实**（selectedByPddr/selected） | **真实** | 真实存在 |
| enteredWorkingPopulation | **真实**（enteredNextPopulation，仅方向代表） | **真实但语义等价于 selectedByPddr=true**（semanticRoleAfter 恒 NOT_APPLICABLE） | 部分（survivedNextGeneration，仅 CA-TA） | 存在；per-run 全候选口径以 selectedByPddr 为准 |
| offspringImproved | 真实（improvedOffspring，cohort 口径） | 不存在 | 真实（improvedOffspringLater，RIGHT_CENSORED_RUN_END 右删失） | 部分 |
| teacherFingerprint/qState/qAction/requesterRole | 不存在 | 不存在 | **真实**（telemetry-teacher-use-events.csv，23/18-final 50k run） | 仅 50k |

## 限制（必须随证据引用）

1. **正常实例候选级缺失**：50_2_3_1 无任何候选级数据（grep 全库核实）→ 困难 vs 正常利用率对比列一律 EVIDENCE_FIELD_LIMITATION。
2. **环形缓冲截断**：ca-ta-lite-events.log 保留 ~4094 行（caTaTest+caTaApply 实际 5668+1865=7533）；p6 环形缓冲 p6EventsRetained=4096 / p6EventsTotal=182923。凡涉及这两路事件流的字段均受截断影响。
3. **排除项**：`02-implementation/pddr-full-ledger.csv` 为合成测试数据（fingerprint=FP_A4_001）非真实运行，已排除；18-final 的 -v1/-v2/-v3/-v4/-defaultheap 探索变体已排除；`14-final-sequence-audit` 单行文件已排除。**A4-50k-ON 同一配置在 18-final、23-a4-50k、26-final-runtime-jar-validation 三处出现（换 jar 重验），行级内容对应同一次语义运行，引用时不得三倍计数。**
4. B 源历史裁决上下文：FC5_250K_ROOT_CAUSE_REPORT.md 已按其自身预注册判 H1.1-H1.4 全部 FAIL（pool→next 保留率困难实例未低 20pp、Nnd>100 为 0）。本目录只复算与登记数据，H1/H2 最终裁决归主 Agent。
5. 汇总核对：pddr-observation.properties 显式声明 `poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR`（冻结 Jar 不导出 pool 级归因）。
