# AUDIT_PREREGISTRATION — V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT-V1（冻结）

- 日期：2026-08-31
- 性质：**只读审计**。`newFEConsumed=0`、不上传训练机、不实现新算法、不改任何冻结语义。全部结论基于既有证据。
- 审计问题：为什么 V35 在 100-job 困难实例上仍能找到较好的 Cmax 极值，但 HV/IGD 与 Pareto 覆盖质量明显崩塌？是否存在真实、可操作且未被历史实验否决的单变量杠杆？

## 1. 优先级（冻结，不得越级）

```text
H1：FC5候选利用断裂是否解释100-job覆盖崩塌（先查）
H2：PDDR压缩后四方向代表是否无法进入working population（与H1合并裁决）
H3：Teacher集中（仅当H1/H2未确认或字段不足）
H4：CFVF/Qp生成偏置（仅当H1-H3均不能解释）
```

禁止：从"HV差"直接跳到"修改PDDR"；把FC6对三种修法（ORDER_SWAP / BP_RESERVED_LEGACY / REGION_AWARE 15/55/15/15）的否决误写成"利用断裂机制被否决"。

## 2. 数据源与分析口径（冻结）

- 主时序数据：250k LOCAL-FE-PACING 运行（C0=正式语义对照；C2/C3 仅用于理解局部FE重配是否改变利用链，不是候选）。实例 50_2_3_1（正常）/100_5_3_1（困难）；检查点 50000/100000/150000/200000/terminal(250000)。三 seed（20260916-18）。
- 检查点前沿含真实 candidateFingerprint（SHA-256，六向量规范文本），observed-full-front 与 decision-front 的集合差、C-metric、potentialHvRecovery 由此计算——不使用 poolOrdinal/index%4/文件序号冒充候选身份。
- 历史证据：FC5 候选利用断裂审计、FC6A3/FC6A4/FC6B、Stage2 A0–A4 先导、FC5-T 迁移审计、PDDR 完整 ledger（含 telemetry-pddr-full-ledger.csv 候选级遥测）、GAP-PROBE-V2 500k。路径以 rg/文件系统扫描定位，不得凭记忆。
- 指标：decisionFrontSize、observedFullFrontSize、exactDedupSize、strictNdSize、C(observed,decision)、C(decision,observed)、observedOnlyNdCount、decisionOnlyNdCount、observedOnlyRatio、normalizedNearestNeighborDistance、minCmax/minTEC/minTWC、HV、IGD；对 observed-only 严格非支配点计算 potentialHvRecovery（并入决策前沿后的 HV 增量，统一各实例 reference 与归一化）。
- 四方向语义固定：G1_CMAX / G4_BALANCED / G2_TEC / G3_TWC。不引入 NSGA-III 参考向量、crowding 或新区域定义。

## 3. 裁决门（冻结，全部满足才可裁决）

### PDDR_WORKING_POPULATION_COMPRESSION（H1+H2，须全部六条）

1. 困难实例 ≥2/3 seed、连续 ≥2 检查点：observedOnlyRatio ≥ 10% 或 potentialHvRecovery ≥ 2%；
2. 该现象明显早于或同步于 HV/IGD 退化（非终态事后出现）；
3. 困难 vs 正常实例的利用率差 ≥ 10 个百分点；
4. ≥1 个四方向代表满足"pool 存在 → PDDR 后或 working population 丢失"，且保留率比正常实例低 ≥20 个百分点；
5. 丢失点仍严格非支配，且非精确重复/非法解/后续被合法支配删除；
6. 证据来自真实候选指纹/lineage。

缺候选级字段 → `H1_H2=INSUFFICIENT_EVIDENCE`（不得从前沿规模差异直接推断根因）。

### TEACHER_EXPOSURE_CONCENTRATION（H3，须全部五条）

困难实例 ≥2/3 seed、连续 2 检查点集中度明显恶化；top1Share 高 ≥20pp 或归一化熵低 ≥0.20；方向遗憾或后代有效率同步恶化；时序早于崩塌；存在未被历史结构审计否决的真实可达杠杆。不得重提已关闭的 Teacher lambda 锦标赛惩罚方案。

### CFVF_QP_GENERATION_BIAS（H4，须全部四条）

困难实例 ≥2/3 seed 稳定；连续 2 检查点；某来源大量生成但严格 ND/HV 贡献显著下降；存在未被冻结、可达、单变量的调整接口。不得关闭 CFVF/Qp/Dual-Q/CA-TA、不改奖励/动作集合/P-G 时序。

## 4. 最终裁决空间

```text
PDDR_WORKING_POPULATION_COMPRESSION / TEACHER_EXPOSURE_CONCENTRATION /
CFVF_QP_GENERATION_BIAS / NO_ACTIONABLE_LEVER / INSUFFICIENT_EVIDENCE
```

裁决区分 observational mechanism evidence 与 causal confirmation：本轮最多产出 `ROOT_CAUSE_CANDIDATE`（观察性机制证据），不得宣称最终因果根因——因果确认须后续单变量修复实验。

## 5. 若 PDDR 成为候选杠杆

只写"下一实验建议"：GLOBAL_ORIGINAL 为对照、保持四子群语义、只改一个 PDDR 选择环节、不用固定区域配额/15-55-15-15/BP预留/crowding/NSGA-III参考向量/新教师门控、不改档案容量/DOE配比/LS顺序；20k 工程门 → 50k 筛查 → 通过后才申请 250k。找不到真实可达单旋钮 → `NO_ACTIONABLE_LEVER`，不得为继续实验创造新机制。

## 6. 证据目录与产出

```text
01-evidence-registry/  evidence-source-registry.csv, field-availability-matrix.csv, historical-decision-map.csv
02-front-coverage/     front-coverage-timeseries.csv, observed-decision-gap.csv
03-pddr-utilization/   directional-representative-lifecycle.csv, pddr-working-population-utilization.csv, archive-working-gap.csv
04-teacher-analysis/   teacher-concentration-analysis.csv
05-cfvf-qp-analysis/   source-contribution-analysis.csv
06-independent-verification/  主Agent独立复算
07-decision/           hypothesis-decision-matrix.csv, PARETO_COVERAGE_LEVERAGE_AUDIT_REPORT.md, FINAL_DECISION.properties
```

字段不存在时写 NOT_EXPORTED / NOT_OBSERVABLE / EVIDENCE_FIELD_LIMITATION。所有汇总数字由脚本从 CSV 生成。
