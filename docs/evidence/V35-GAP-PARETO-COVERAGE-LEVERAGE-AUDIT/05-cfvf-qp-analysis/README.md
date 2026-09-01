# 05-cfvf-qp-analysis — Agent C 产出（字段可用性登记 + 计数级复算，无最终假设裁决）

生成脚本：`generate_05_source_contribution.py`（Python 3.11，只读输入，全部数字由脚本生成）。

## 数据源（绝对路径）

`E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR\16-remote-250k-runs\sync\seed-{20260916,20260917,20260918}\results\run-GAPL250K-{C0,C2,C3}-{50_2_3_1,100_5_3_1}-<seed>\`
的 `status.properties`（mechanismSummary 计数）、`pddr-observation.properties`、`cmax-audit-records.csv`（tracked 候选）。

## 产出：source-contribution-analysis.csv（126 行 = 18 run × 7 来源模块）

- **generationCount（计数级，真实可用）**：CFVF=cfvfOffspring、Qg=qgSelections、Qp=qpActions、CA-TA-Test=caTaLiteTest、CA-TA-Apply=caTaLiteApply（均来自 mechanismSummary）；inheritedLS=inheritedLocalEventOps（pddr-observation.properties，**环形缓冲截断**）；initial=runtimeSubSwarmSizes 之和（100）。C0: cfvf=155000, qp=135100；C2: 170000/150100；C3: 180000/160100（18/18 run 解析成功）。
- **ndContribution_ndWithinTrackedSubset**：仅对 cmax-audit tracked 子集（每 run 34-78 个候选）计算子集内严格非支配比例；tracked 候选 mechanism 仅 {FIXED_VNS→inheritedLS, CFVF→CFVF, INITIAL→initial}，**Qg/Qp/CA-TA-Test/CA-TA-Apply 无任何 tracked 候选 → EVIDENCE_FIELD_LIMITATION**。
- **pddrRetainedRate / nextRoundSurvivalRate / globalArchiveRate**：tracked 子集内计算（nextRoundSurvivalRate 分母仅计 YES/NO，排除 NOT_SELECTED）。
- **offspringImprovedRate = NOT_EXPORTED（全部 A 源行）**：A 源无该字段；仅 B 源 directional-lifecycle（4 方向代表，cohort 口径）与 C 源 23-a4-50k `telemetry-cata-contribution-events.csv`（improvedOffspringLater，右删失）/`telemetry-teacher-use-events.csv`（offspringImproved，50k 单 seed）存在。

## 冻结 Jar 字段限制（H4"生成偏置"门所需字段的可用性矩阵）

| H4 所需 | 可用性 | 依据 |
|---|---|---|
| 某来源"大量生成"（计数） | **可用**（mechanismSummary 计数级，250k×18 run） | status.properties mechanismSummary |
| 某来源"严格 ND 贡献" | **NOT_EXPORTED**（仅 cmax-audit tracked 子集 34-78/run，且子集偏向 G1_CMAX/cmax 审计；Qg/Qp/CA-TA 为 0 tracked） | pddr-observation.properties：`poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR` |
| 某来源"HV 贡献" | **NOT_EXPORTED**（无任何 per-source HV 字段） | 同上 |
| 冻结可达的单变量调整接口 | 未在本目录评估（归主 Agent 裁决） | — |

## 环形缓冲截断（必须随证据引用）

- `ca-ta-lite-events.log` 仅保留最后 ~4094 行，而 caTaTest+caTaApply 实际 = 5668+1865 = 7533 事件 → CA-TA 事件流前段永久丢失，任何基于该 log 的统计只能代表尾部。
- p6 环形缓冲：`p6EventsRetained=4096` / `p6EventsTotal=182923`；inheritedLS 计数（inheritedLocalEventOps）在 9 条 run 中精确等于 4096（截断上限），真实 LS 事件总数 NOT_OBSERVABLE。`formalLocalFE`（87367 等）为 FE 消耗量，不是生成计数。

## 数据级观察（不外推、不裁决）

- C0 臂 tracked 子集 ND 率 3-seed 中位：CFVF 0.0455、inheritedLS(FIXED_VNS) 0.1423、initial 0.0000。**该比例仅描述 tracked 子集内部（其构成偏向 cmax 改善事件），不可外推为各来源对全 pool 的 ND/HV 贡献**——后者在冻结 Jar 中未导出。
- 因此 H4 的"某来源大量生成但严格 ND/HV 贡献显著下降"判据所需的来源级 ND/HV 时序在 250k 数据中**无法直接检验**；最多只能以 tracked 子集比例、pddrRetainedRate、archiveInsertions（3100/3400/3600 by arm）等代理量观察。若主 Agent 需要，FC5-T 50k 单 seed 的 cata-contribution-events（含 enteredMergePool/selectedByPddr/enteredGlobalArchive/improvedOffspringLater）是目前唯一的候选级 CA-TA 生命周期证据。
