# PDDR Evidence Resolution（V35-P26）

## 1. 裁决摘要

本文件是对既有 PDDR 证据链的只读整合，不构成新的实验结果，也不改变算法实现。

当前裁决：

- `KEEP_GLOBAL_ORIGINAL`：继续使用当前冻结的 `GLOBAL_ORIGINAL` 环境选择。
- `FC6B_FINAL_VETO`：不采用 `REGION_AWARE`；FC6B 的 20-job/100-job 直接质量比较已经否决该分支。
- `PDDR_CURRENT_EVIDENCE_CLOSED`：P6.1.1 已证明工程实现，FC5/FC6 已完成候选生命周期与替代选择器审计；现有 Stage2 字段限制只约束更细的离线分类，不再把 PDDR 描述为“尚未审计”。
- Stage2 12 组/60 条先导继续作为诊断数据；正式矩阵不恢复，冻结 JAR 不修改，除非完成候选级审计并取得新的决策。

本文件中的“当前冻结语义”指 v3.5 主线：`ShiftMode=NONE`、`GLOBAL_ORIGINAL`、严格三目标 `[0,1,6]`、`FM3`、单族/顺序无关语义及已冻结的局部搜索顺序。旧 Shift 运行不得与该主线合并解释。

## 2. 证据分类总表

| 证据 | 语义分类 | 可支持的结论 | 不能支持的结论 |
|---|---|---|---|
| P6.1.1 | v3.5 前工程验证；未显式标记为 Shift-on | `EVALUATED_PDDR` 的评估顺序、FE 记账和 offspring-first 替换链路正确 | 当前 v3.5 的质量结论、PDDR 必须修改 |
| P9.1 | **旧 Shift 语义** | 旧运行中 Cmax 记录的候选/教师/存档生命周期诊断 | 当前 `ShiftMode=NONE` 下的 PDDR 结论 |
| FC5 / FC5.2 | 当前无 Shift、纯观察 | 当前主线存在 Cmax 记录准入后被 PDDR/后续链路丢失的具体实例 | 选择器已经构成 bug、应立即改 PDDR |
| FC6A1 | 当前无 Shift、纯观察 | `GLOBAL_ORIGINAL` 的全局 score 对方向边界的结构性拥挤 | 任何替代选择器已经通过质量门 |
| FC6A2 | 当前无 Shift、区域反事实观察 | Region 假设具有可解释性；174.44 在反事实区域分配下可保留 | `REGION_AWARE` 已被正式接受 |
| FC6A3 | 当前实现审计 | PDDR 候选账本、输入构成和观测开关的工程不变量 | 选择器质量优于其他方案 |
| FC6A4 | 当前无 Shift、局部搜索诊断 | 保留 `CA-TA-Lite → inherited` 顺序 | PDDR 应改变 |
| FC6B | 当前无 Shift、直接质量比较 | `REGION_AWARE` 在正式比较中不可接受，尤其被 100-job veto | Region 反事实的解释本身错误 |
| Stage2 12×5 | 当前主线先导；聚合诊断 | A4 的总体 pilot 信号和 PDDR/Cmax 丢失信号 | 候选级 PDDR 缺陷、正式统计结论 |

## 3. P6.1.1：工程链路成立，但不是当前性能证据

主要文件：

- `docs/evidence/P6.1.1/P6_1_1_REPORT.md`
- `docs/evidence/P6.1.1/CONFIG_PDDR.txt`
- `docs/evidence/P6.1.1/FINAL_GATE_SUMMARY.txt`
- `docs/evidence/P6.1.1/TEST_PDDR_INTEGRATION_2000FE.log`

该批次（2026-08-09）使用 `fatigue_improved`、B2P、`EVALUATED_PDDR`，在 2000 FE 下完成初始 100 个个体加 1900 个 offspring，19 轮 PDDR，且每个 offspring 恰好一次 FE；父代、PDDR 分数、history 不额外消耗 FE。测试覆盖未评估候选拒绝、candidate/history 映射、offspring-first 稳定 tie、author score，报告为 6/6 通过。

因此 P6.1.1 证明的是工程时序和评估记账不变量：

```text
save evaluated parents/history
→ Qg
→ CFVF offspring
→ each offspring exactly once
→ settle Qg reward
→ PDDR stable select
→ offspring-first merge
→ replace population/history
```

该报告没有当前 `ShiftMode=NONE` 的正式性能实验，也没有 Qp、双 Q、O10–O13 或 CA-TA 主线证据。它在时间和语义上早于 v3.5 冻结；报告本身未给出 `LEFT_RIGHT` 或 `fatigue-shift-v2-common-gap`，故不把 P6.1.1 归类为旧 Shift-on，但也不能把它当作当前冻结语义的定量证据。

## 4. P9.1：明确的旧 Shift 边界

主要文件：

- `docs/evidence/P9-cmax-audit-20k-20260812/Cmax_AUDIT_REPORT.md`
- `docs/evidence/P9-cmax-audit-20k-20260812/mechanism-summary.txt`
- `docs/evidence/P9-cmax-audit-20k-20260812/verification.txt`
- `docs/evidence/P9-cmax-audit-20k-teacher-use-20260812/TEACHER_USE_REPORT.md`

P9.1 的明确语义字段为：

```text
FM3 + LEFT_RIGHT
fatigue-shift-v2-common-gap
algorithmSemantics=p8-ablation-v5-shift
sourceP8Label=FULL
formalBaselineRuntimeEnabled=false
```

它是 20,000 FE、`20_2_3_1`、seed `20260808` 的旧 Shift 运行。该运行记录了 11 条严格的新 Cmax 记录（1 条初始、10 条搜索）；10 条搜索记录都进入 candidate set、被 PDDR 保留并进入 personal archive，9 条进入 global archive；最后一条记录在 FE6750，`Cmax=201.278740141651`，至 FE20k 没有更低生成值。教师追踪还表明 201.279 从未成为 G1 社会教师，而 205.902 曾作为社会教师使用 680 次。

这些数据可用于解释旧 Shift 运行的历史机制，但不能回答当前 `ShiftMode=NONE` 下的 PDDR 是否丢失 Cmax，也不能与 FC5/FC6/Stage2 的当前主线数值合并。P9.1 的结论边界必须在后续报告中保持显式。

## 5. FC5：当前主线的具体知识损失信号

主要文件：

- `docs/evidence/V35-P26/fc5-cmax-audit/FC5_CMAX_GIR_AUDIT_REPORT.md`
- `docs/evidence/V35-P26/fc5-cmax-audit/FC5_1_CMAX_BESTEVER_TEACHER_EXPOSURE_REPORT.md`
- `docs/evidence/V35-P26/fc5-cmax-audit/FC5_2_SEED22_LIFECYCLE_REPORT.md`

FC5 使用 `20_2_3_1`、500k FE、A4-Pacing、当前无 Shift 主线。FC5.1 显示“archive/teacher 暴露不足”不能单独解释问题：archive-best 暴露次数很高，但 fully evaluated 的最好 Cmax 仍未进入最终 archive/front：

| seed | fully evaluated best Cmax | FE | archive best | 最终 front 是否存在 |
|---|---:|---:|---:|---|
| 22 | 174.4367 | 288,564 | 188.39 | 否 |
| 23 | 169.6270 | 219,476 | 175.70 | 否 |
| 24 | 191.2079 | 496,557 | 195.70 | 否 |

FC5.2 对 seed22 做了候选生命周期观察，且审计开关 ON/OFF 保持 front hash、FE 和结果一致。追踪的 1024 条候选中：local accepted 86、merge pool 38、进入 next population/PDDR 18；1008 条没有被 archive 观察；`archive.add` 有 217 次观测（10 accept、91 dominated、116 duplicate），未发现 archive clear/rebuild/replace 异常。174.4367 的实际链路为：

```text
local ACCEPT → merge → PDDR REJECT(score=1, FE291213)
→ never archive → absent from final result
```

FC5.2 因而支持“准入/环境选择造成的知识损失”这一工程假设，而不是 archive 实现错误。限制是：该审计只覆盖已接入 `V35EvaluationSourceContext` 的来源，未必覆盖全部 `vnd()/factorySearch()` 评估点；它是 1 个 seed 的深审计，不足以单独批准修改选择器。

## 6. FC6：从假设到最终否决

### 6.1 FC6A1：全局 score 的组成拥挤

文件：`docs/evidence/V35-P26/FC52-FC6-COMPLETE-20260819/00-REPORTS/FC6A1_PDDR_COMPOSITION_AUDIT.md`

12 条 500k 运行的纯观察把 `GLOBAL_ORIGINAL` score 分成 `N_<1`（中心非支配解）、`N_=1`（边界 `q=0,p=0`）和 `N_>1`。BASE100 中位 `N_<1=102`、`P(N_<1>100)=0.532`；BASE20 中位 108、概率 0.855。当 `N_<1≥100` 时，69 轮中边界 score=1 解全部被拒。这证明了边界拥挤的结构性来源，但没有证明该多目标选择规则在论文意义上必然错误。

### 6.2 FC6A2：区域反事实成立，但仅是反事实

文件：`docs/evidence/V35-P26/FC52-FC6-COMPLETE-20260819/00-REPORTS/FC6A2_REGION_PDDR_AUDIT.md`

12 条运行、474 轮、2772 个被全局 PDDR 淘汰的非支配解中，按 15/55/15/15 区域容量模型计算，可吸收比例为：QGS100 84.4%、BASE100 72.2%、QGS20 82.0%、BASE20 74.4%；`rejG2=0`。174.44 反事实探针中，BASE 全局存活 9/62（14.5%），区域机制 62/62（100%，6.89×）；QGS 为 4/18（22.2%）对 18/18（100%，4.50×）。

这证明 Region 假设具有机制解释力，并不证明真实 `REGION_AWARE` 实现已经通过质量门。该报告中的 `STRONG GO` 仅是反事实审计结论，必须由后续直接比较更新。

### 6.3 FC6A3：实现审计通过

文件：`docs/evidence/V35-P26/FC6A3-implementation-audit/IMPLEMENTATION_AUDIT.md`

FC6A3 证明候选账本、来源标记、评估状态、每父槽至多一个最终 local carrier、PDDR 输入构成和审计开关的工程不变量。2k/6k FE 的 ON/OFF 比较保持初始种群、FE、front、P6/PDDR/Qg/Qp 事件流及 hash 一致。`GLOBAL_ORIGINAL` 仅作为 FC6A4 正式主线选择，`BP_RESERVED_LEGACY` 仅历史保留，`REGION_AWARE` 仅 FC6B 试验使用。

### 6.4 FC6A4：局部搜索顺序不改

文件：`docs/evidence/V35-P26/FC6A4-local-search-order/ORDER_DECISION.md`

在当前 `ShiftMode=NONE`、A4-Pacing、`GLOBAL_ORIGINAL` 条件下，反转 local-search order 虽使 min Cmax 中位改善 6.870539%，但 HV 下降 0.867938%、IGD 上升 11.678867%（超过 +10% 门槛），故维持 `CA-TA-Lite → inherited inter-factory/O1-O9`。该结论与 PDDR 选择器修改无关，但构成当前主线冻结的一部分。

### 6.5 FC6B：最终否决 `REGION_AWARE`

文件：`docs/evidence/V35-P26/FC6B-region-aware/FC6B_RESULT.md`

FC6B 在当前无 Shift、相同初始种群、500k FE、20/100-job、3 seeds 下直接比较 `GLOBAL_ORIGINAL` 与 `REGION_AWARE`，12 条运行完成。`REGION_AWARE` 使用 G1/G4/G2/G3 = 15/55/15/15 容量。

| 实例 | median min Cmax | median HV | median IGD | 裁决 |
|---|---:|---:|---:|---|
| 20-job | +1.6710% | −3.9689% | +67.8729% | 拒绝 |
| 100-job | −1.5743% | −22.7133% | +371.7009% | 100-job veto |

因此 FC6B 是对 FC6A2 反事实假设的最终行为裁决：区域分配在当前实现下不能替代 `GLOBAL_ORIGINAL`。失败提交中的物理容量和缺输入问题已隔离，不与 12 条有效结果混合；有效结果仍然否决该分支。

## 7. Stage2 12 组/60 条先导：有信号，但字段不足

主要文件：

- `docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/PILOT_DECISION.md`
- `docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/results/PILOT_REPORT.md`
- `docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/results/pddr-cmax-lifecycle.csv`
- `docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/PAUSED_BY_USER.properties`

先导为 12 组 × A0–A4 五臂 = 60 条完整运行，`20_2_3_1`、500k FE、seed `20260808–20260819`。A4 相对 A0 的聚合信号为 HV +25.24%、IGD +19.02%、Cmax +3.95%，HV/IGD 为 11/12 胜；A2→A3 出现明显退化，需单独审查 Qp/personal archive。

PDDR/Cmax 聚合信号：A4 有 6/12 条运行曾生成优于最终 global 的 Cmax；损失组中位差约 0.55%，最差 seed20260808 为 `720.291 → 738.804`（约 2.51%）。各臂中位记录保留率约为 A0 11.62%、A1 9.90%、A2 7.19%、A3 9.58%、A4 13.03%。

### EVIDENCE_FIELD_LIMITATION

当前 `pddr-cmax-lifecycle.csv` 只有聚合字段：`finalFrontMinCmax`、`auditBestGlobalCmax`、`auditBestGeneratedCmax`、`generatedBetterThanGlobal`、`generatedToGlobalGap`、`recordCount`、`pddrRetained`、`globalArchive`、`personalArchive`。它不包含：

- 候选来源、候选 FE/cycle 和完整 lineage；
- 严格三目标 `q/p`、PDDR score、rank、selected/rejected reason；
- local admission、merge pool、archive add 的 dominated/duplicate/remover 状态；
- 最终 archive/front 是否实际包含该候选。

所以 60 条先导只能支持“需要候选级解释”的信号，不能单凭记录保留率或 Cmax gap 认定 PDDR 缺陷。`PAUSED_BY_USER.properties` 也明确记录：`acceptedFairnessGroups=12`、`acceptedRuns=60`、`formalMatrixMayResumeWithoutNewDecision=false`、`formalStatisticsComputed=false`、`finalReferenceConstructed=false`。

## 8. 当前证据边界与未来重开条件

当前 PDDR 专项到此收口，不重复运行 PDDR 实验，也不把 Stage2 字段不足误写成“PDDR 尚未审计”。若未来出现新的、可重放的候选级证据，重开 PDDR 课题至少需要：

1. 对 60 条先导逐候选重建：评估来源/FE → local admission → merge pool → 严格 `[0,1,6]` 支配关系 → PDDR score/rank → selected/rejected → archive add/duplicate/dominated → next population → final archive/front。
2. 将“生成最佳但最终丢失”分为：严格 Pareto 被支配、`score=1` 被中心解挤出、极值但多目标较差、未被 archive 观察、后续被移除。
3. 核对所有评估来源，而不是只依赖 FC5.2 已接入的 source context；同时验证最终 `getResult()` 的 provenance。
4. 把 A2→A3 的 Qp/personal archive 问题单独审计，不能混入 PDDR 结论。
5. 如果原始事件日志无法取得，结论应保持 `EVIDENCE_FIELD_LIMITATION`；不得用汇总 CSV 推断候选级 bug。

只有新证据稳定证明“当前 PDDR 选择链路造成不可接受且无质量补偿的知识损失”，并完成新的预注册决策后，才可考虑小规模单变量对照。当前不启动该实验、不恢复远端任务；Stage2 缺失字段统一标记为 `EVIDENCE_FIELD_LIMITATION`。

## 9. 最终状态标签

```text
PDDR_STATUS = CURRENT_EVIDENCE_CLOSED
PDDR_DECISION = KEEP_GLOBAL_ORIGINAL
REGION_AWARE = VETOED_BY_FC6B
OLD_SHIFT_P9_1 = HISTORICAL_ONLY
STAGE2 = PAUSED_AFTER_12_GROUPS_60_RUNS
EVIDENCE_FIELD_LIMITATION = ACTIVE
PDDR_MUST_CHANGE = NOT_ESTABLISHED
```
