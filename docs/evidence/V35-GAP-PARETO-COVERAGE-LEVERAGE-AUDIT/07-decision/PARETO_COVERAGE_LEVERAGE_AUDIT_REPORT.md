# PARETO_COVERAGE_LEVERAGE_AUDIT_REPORT — V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT-V1

- 日期：2026-08-31
- 审计问题：为什么 V35 在 100-job 困难实例上仍能找到较好的 Cmax 极值，但 HV/IGD 与 Pareto 覆盖质量明显崩塌？是否存在真实、可操作且未被历史实验否决的单变量杠杆？
- **最终裁决：`NO_ACTIONABLE_LEVER`**（无 ROOT_CAUSE_CANDIDATE； observational evidence only，无因果确认）
- 约束遵守：`newFEConsumed=0`、未上传训练机、未实现新算法、冻结语义零改动。

## 1. 证据治理收口（§二）

- `15-250k-preregistration/evidence-sha256.tsv` 5 项内部漂移**确认并收口**（artifact-binding.csv、三份 50_2_3_1 binding、upload-sha256.tsv；根因=setupFileSha256 手工转录 63/64 位截断，用户授权修复）。原清单原样保留并复制为 `evidence-sha256.pre-binding-correction.tsv`（44 项，记录修复前状态、与被覆盖文件不再闭合——如实登记）；新增 `evidence-sha256.post-binding-correction.tsv`（45 项，反向复算 0 缺失 0 不匹配）。
- 首次失败 3 臂的独立日志被重试覆盖：**如实登记** `failedAttemptArmLogsPreserved=false`；失败摘要证据（总日志 exit=1 时间线、执行报告异常转录）保留（`failedAttemptSummaryEvidencePreserved=true`）。
- 科学输入/算法/预登记设计/科学结果零影响（`scientificInputsChanged=false` 等五项全登记）。
- LOCAL-FE-PACING 顶层清单重建：**2353 项，反向复算 0 缺失 0 不匹配** → `evidencePackageFinalSignoff=true`（`19-evidence-governance-correction/`）。

## 2. 审计发现总览

### 2.1 前沿覆盖时序（250k，C0/C2/C3 × 2 实例 × 3 seed × 5 检查点，指纹级）

核心时序表 `02-front-coverage/front-coverage-timeseries.csv`（180 行）与 `observed-decision-gap.csv`（90 行），主Agent独立复算 16/16 组一致（双 HV 实现互验）：

1. **"未被利用的候选池"在目标空间几乎不携带价值**：observed-only 严格非支配点即使全部并入决策前沿，potentialHvRecovery 最大仅 **0.79%**（90 行中 0 行 ≥2% 门线；独立实现复核一致）。指纹级 observedOnlyRatio（中位 0.08–0.23）中约三至六成是"目标三元组已在决策前沿、仅指纹不同"的候选；objective-new 子集的 HV 增量 <0.1%。
2. **该现象是全局性质而非困难实例特异**：82/90 行 ratio≥10%，含正式基线 C0 自身；C0 终态 ratio 困难 0.177 vs 正常 0.229——困难实例反而更低（门条件 3 要求困难高 ≥10pp，实际 **−5.21pp**，方向相反）。
3. **覆盖差距与 HV 差距的时间结构**：困难实例 C0 的决策前沿 HV 全程高于 C2/C3，差距在 100k–150k 最大（0.610 vs 0.578/0.534）、终态略收敛；而 observed-decision 差距从 50k 就普遍存在且三臂同构——**覆盖差距不与利用差距同相**。
4. 直接推论：**HV/IGD 崩塌不是"好候选已生成但被丢弃"**——若存在被压缩掉的价值池，合并它应带来可观 HV 回收，实测没有。崩塌源于**生成的候选分布本身缺乏目标空间多样性**（Cmax 极值好、前沿中后段弱），属于生成侧问题。

### 2.2 PDDR 生命周期与利用链（H1/H2）

- **候选级字段可用性（`01-evidence-registry/field-availability-matrix.csv`，23 字段）**：当前 250k 运行（LOCAL-FE-PACING 与 FC5-250K）均未导出 enteredMergePool/selectedByPddr/enteredWorkingPopulation 的逐候选账本（冻结Jar遥测限制）；完整 PDDR ledger 仅存在于 2k/20k/50k 诊断运行；正常实例 50_2_3_1 的候选级数据全库**零命中**。
- FC5 四方向代表 lifecycle（549 个真实指纹，250k）：A4/100_5_3_1 pool→working 保留 93.8%、A2 88.1%（对照 100_2_4_1 为 96.7%/100%）——有困难实例倾向但幅度 ≤9pp，远低于门要求的 ≥20pp，且**正常实例对照缺失**（条件 4 不可评估）。
- **历史裁决链（`historical-decision-map.csv`，10 条）**：FC5-250K 已以 12/12 正负对照裁决"ND 膨胀→PDDR 压缩→利用断裂"链 NOT_SUPPORTED（pool→next 保留 ≥88%、archive-working gap=0、maxNnd=92 无溢出）；FC6 否决的三种修法（ORDER_SWAP / BP_RESERVED_LEGACY / REGION_AWARE 15/55/15/15）是修法否决、不是机制否决——本轮如实沿用该边界。
- **H1/H2 裁决：`INSUFFICIENT_EVIDENCE`**（候选级字段缺口 + 前级证据方向相反：条件 3 方向相反、条件 4 不可评估、条件 1 的 recovery 支路 0/90）。不得从前沿规模差异推断 PDDR 是根因——本审计亦未这样做。

### 2.3 Teacher 集中（H3）

`04-teacher-analysis/teacher-concentration-analysis.csv`（18 run × 5 FE 窗口，与 mechanismSummary 18/18 核对一致）：集中度随预算上升是**双实例共同趋势**（C0 top1Share 0.05→0.21–0.23，归一化熵 0.82→0.63–0.67）；困难 vs 正常终窗差 **+1.75pp / −0.04**，远低于门（≥20pp / ≥0.20）。方向遗憾与后代有效率无困难特异恶化。**H3 裁决：NOT_CONFIRMED**（观察性、事件级、与 mechanismSummary 交叉验证）。

### 2.4 CFVF/Qp 来源贡献（H4）

`05-cfvf-qp-analysis/source-contribution-analysis.csv`（126 行）：来源生成计数可用（C0：cfvfOffspring=155000、qpActions=135100 等），但**来源级严格 ND/HV 贡献与 per-source offspringImproved 冻结Jar未导出**（`poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR`；Qg/Qp 零 tracked 候选；ca-ta 事件流 4094 行环形截断）。"大量生成但贡献下降"无法评估。**H4 裁决：INSUFFICIENT_EVIDENCE**。

## 3. 假设-裁决矩阵与最终裁决

| 假设 | 裁决 | 关键缺口/反证 |
|---|---|---|
| H1+H2 PDDR_WORKING_POPULATION_COMPRESSION | INSUFFICIENT_EVIDENCE | 候选级 250k 遥测 NOT_EXPORTED；front 级反证：recovery≤0.79%、困难≤正常 |
| H3 TEACHER_EXPOSURE_CONCENTRATION | NOT_CONFIRMED | 困难特异差 1.75pp ≪ 20pp 门 |
| H4 CFVF_QP_GENERATION_BIAS | INSUFFICIENT_EVIDENCE | 来源级 ND/HV 归因 NOT_EXPORTED |

无假设通过其预登记门 → **无 ROOT_CAUSE_CANDIDATE**。杠杆清单逐项核对：ORDER_SWAP（FC6 ✗）、BP_RESERVED_LEGACY（FC6 ✗）、REGION_AWARE 15/55/15/15（FC6 ✗）、Teacher lambda 锦标赛惩罚（覆盖率关闭 ✗）、betaMax/LOCAL_FE_PACING（250k 确认否证 ✗）；PDDR 选择环节在本轮证据下不满足开工条件（§十前置未达成）；教师门控/CFVF/Qp/奖励/动作集/P-G 时序均在冻结保护内。**→ `NO_ACTIONABLE_LEVER`**：找不到真实、可达、未被否决的单变量杠杆；不为继续实验创造新机制。

## 4. 审计实际回答了什么

对审计问题的观察性回答（非因果确认）：

1. **Cmax 极值好但覆盖崩塌的机制定位**：现有证据一致指向**生成侧多样性不足**（前沿中后段候选稀缺），而非保留侧/PDDR 压缩——被"丢弃"的候选在目标空间近乎冗余（recovery≤0.79%），教师集中度无困难特异性，历史利用断裂链已在 250k 被正负对照否证。
2. **证据缺口（未来轮次的前提条件）**：要在候选级闭环"生成→PDDR→working population→教师→后代"全链归因，需要 250k 预算上两实例（含正常实例对照）的候选级 PDDR/来源归因遥测（FC5 诊断Jar能力 + 正常实例对照运行）。这是**诊断能力缺口登记**，不是本轮可执行的杠杆，也不是实验启动。
3. 本轮不修改 PDDR、不提出新机制、不启动任何实验；下一修复族的批准权在用户。

## 5. 独立复核清单（主Agent）

- 旧顶层清单（2346 项）与修复后新顶层清单（2353 项）：反向复算全部闭合（`06-independent-verification/`）；
- 18 条 250k 运行状态与 6 个公平组：run-acceptance 18/18 PASS、fairness 6/6 PASS 独立确认；
- C2/C3 原始裁决（NO_REPAIR_CANDIDATE）：与 `18-250k-decision/` 一致，本轮未改变；
- observed/decision 前沿差：双 HV 实现互验 16/16 组一致（recovery 差 ≤0.08pp 绝对值）；
- 最终假设门：H3 阈值、H1/H2 六条件逐条复核（`MAIN_AGENT_INDEPENDENT_CHECK.py`，PASSED）。

## 6. 机器状态

见 `FINAL_DECISION.properties`。治理四文件（AGENTS.md / ROADMAP.md / CURRENT_SCIENTIFIC_STATE.md / CLAIM_EVIDENCE_MATRIX.md）已按 §十三 append-only 更新。
