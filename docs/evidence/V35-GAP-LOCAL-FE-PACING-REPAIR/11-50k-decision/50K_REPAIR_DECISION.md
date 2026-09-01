# 50K_REPAIR_DECISION — V35-LOCAL-FE-PACING 50k 剂量分辨与性能筛查 最终裁决

- 日期：2026-08-31
- 裁决（预登记 §11 六选一）：**`ONE_CANDIDATE_ADVANCES_TO_250K`**
- 唯一候选：**C3（betaMax=0.35，betaMin=0.25）**，`250kEligible=true`、`250kPreregistered=false`、`250kStarted=false`（250k 须用户单独批准，本轮不启动）
- 执行域：训练机 `/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-50k-20260831`（16/16 首次成功，每臂 0 重跑，无 `.partial-*` 残留，总时长 3 分 11 秒）
- 冻结工件：正式Jar `8dad8f40…bad8b9`（只读未动）、实验Jar `a0788580…7331c`（major 52）、快照 ×4、binding ×4（SHA 见 `07-50k-preregistration/artifact-binding-50k.csv` 与 `08-remote-50k/evidence-sha256.tsv`）

## 1. 执行与验收（Agent B，独立复核确认）

16 条 = 4 profile × 2 实例 × 2 seed，全部 COMPLETED、failures=NONE、预算合法（actualFE=decoderCalls≤50000、remainingFE<5000）、illegal/duplicate/来源丢失/Shift 均为 0、PDDR=GLOBAL_ORIGINAL、POST_HOC_PARSE_ONLY。4/4 公平组：四臂初群 V35/P8 双 hash 一致、actualFE 极差 4641<5000。**闭合调度预测 16/16 精确命中**（terminationKind/outerCycles/totalLocalFE 三元组逐条相等）。

## 2. 剂量分辨门：**PASSED**（先于性能，按预登记 §7）

| 门 | 结果 | 证据（主Agent独立复算 == Agent C 脚本产出） |
|---|---|---|
| G1 结构 | PASS | 运行时 betaMax 16/16 逐值匹配 0.65>0.55>0.45>0.35，betaMin=0.25 |
| G2 分配 | PASS（三视图全严格） | 累计 [18169,15259,14900,13936] 严格降；每窗口匹配（6 公共窗）逐窗严格（如 w6: 5827>4153>3051>2263）；per-u∈{0.1..0.9} 对齐全严格 |
| G3 消费 | PASS | localFeShare 四组全部 C0(0.3764)>C3(0.2842)；中位 0.3764>0.3364≥0.2980≥0.2842；相邻降幅 4.00/3.84/1.38 pp（≥1pp 计数 3，需 ≥2）；totalLocalFE 梯度严格（exact-stop 恒等式 caveat 未触发并列） |
| G4 行为 | PASS | 4/4 组 outerCycles [6,6,7,7] 与 cfvfOffspring [30000,30000,35000,35000] 随 betaMax 降低非递减 |

失败判据（仅 C0 分开、C1/C2/C3 在 localFeShare 中位数完全并列）**未触发**——与 20k 相反，50k 下四档完全分离，`doseResolutionAt20k=NOT_RESOLVED` 的疑因（exact-stop 总量恒等式）被更多外层循环解除。种子间确定性：同 profile 的 (actualFE, terminationKind, outerCycles, totalLocalFE, localFeShare) 在两 seed 逐值相同（调度确定性），front/指标随 seed 变化（随机搜索本体）。

## 3. 双口径与 F_common（预登记 §9、偏差 D1）

- **F_common = 40000**（四检查点 10000/20000/30000/40000 在 16/16 run 的 cmax-audit-curves.csv 均有行；C1 终态 45359 ≥ 40000；从数据确认，非事后任选）。
- 主开发口径 = 统一实际FE标量检查点（bestCmax/bestTEC/bestTWC @40000，真实评估序列的逐步最优，非插值）；前沿级共同FE比较按 D1 登记 `NOT_EXPORTED_BY_FROZEN_JAR` 未创建。
- 次要口径 = 终态 front（`TERMINAL_PHASE_CONSISTENT_SECONDARY`），actualFE 极差 4641<5000 随行。
- PFref_terminal：50_2_3_1 557 点、100_5_3_1 242 点（ND(8 fronts)，corrected 管线）。

## 4. 性能筛查（预登记 §10；2-seed 开发筛查，不做统计显著性结论）

终态口径 HV/IGD（2-seed 均值；主Agent独立HV实现与 Agent C 的 fc6 管线门级结论一致，IGD 四位小数吻合）：

| instance | C0 | C1 | C2 | C3 |
|---|---|---|---|---|
| 50_2_3_1 HV (IGD) | 0.8419 (0.1219) | 0.8681 (0.0903) | 0.9455 (0.0640) | 0.8959 (0.0852) |
| 100_5_3_1 HV (IGD) | 0.7752 (0.1802) | 0.7440 (0.1700) | 0.8262 (0.1319) | 0.7899 (0.1471) |

| 候选 | 安全门(50) | 改善门(100) | 三目标保护 | 双口径一致 | 结论 |
|---|---|---|---|---|---|
| C1 (0.55) | PASS | **FAIL**（median ΔHV_hard=−0.19%<+2% 且 ΔIGD_hard=+3.77%<+10%，无改善信号） | PASS | PASS | 出局 |
| C2 (0.45) | PASS | PASS（ΔHV_hard=+10.72%，ΔIGD_hard=+25.13%） | PASS | **FAIL = BUDGET_SENSITIVITY_CONFLICT** | 出局 |
| C3 (0.35) | PASS（ΔHV=+6.18%，ΔIGD=+36.50%） | PASS（ΔHV_hard=+4.58%≥+2%，ΔIGD_hard=+17.30%≥+10%） | PASS（common-FE 最差档：Cmax −0.79% / TEC +0.38% / TWC −0.63%，均 ≥−2%） | PASS（两口径方向一致） | **保留** |

**C2 冲突细节（登记）**：50_2_3_1 的 TWC 终态 median Δ=+1.19% 而 common-FE@40000 Δ=−0.11%，符号翻转——C2 的终态 TWC 改善出现在 FE=40000 之后（C2 exact-stop 用满 50000，比 C0 多 1731 FE），属预算差异而非机制差异。C1/C3 两口径方向逐 instance 一致。

## 5. 候选筛选（预登记 §11）

五维严格 Pareto（ΔHV_hard, ΔIGD_hard, ΔCmax_all, ΔTEC_all, ΔTWC_all）：C1 已出局；C2 与 C3 互不支配，但 C2 被双口径一致门先行淘汰。**保留 C3**；破平键未启用（无并列）。C3 五维 = [+4.58%, +17.30%, −0.79%, +0.38%, −0.63%]。

## 6. 主Agent独立复算（`11-50k-decision/MAIN_AGENT_INDEPENDENT_CHECK.py`）

以独立实现的 3-D 精确 HV（f1 切片）、IGD、ND 过滤直接从 `08-remote-50k/sync` 原始文件重算：预算恒等式（exact-stop totalLocal=FE−100−cycles×5000）、剂量聚合、F_common、终态 HV/IGD、五维配对响应、四门判定——与 Agent C 产出全部一致（INDEPENDENT_CHECK=PASSED，exit 0）。HV 数值存在 ±0.2pp 的实现间差异（fc6 管线 vs 本脚本切片法），不影响任何门级结论。

## 7. 最终机器状态

```ini
50KDecision=ONE_CANDIDATE_ADVANCES_TO_250K
selectedCandidates=C3(betaMax=0.35)
250kEligible=true
250kPreregistered=false
250kStarted=false
20kImplementationGate=PASSED
strict20kDoseGate=NOT_RESOLVED
doseResolution20k=NOT_RESOLVED
doseResolution50k=PASSED
50kStarted=true
50kCompleted=true
runsAccepted=16/16
fairGroupsPassed=4/4
scheduleValidation=16/16
budgetSensitivityConflict=true(C2_only)
DOEStarted=false
validationStarted=false
FinalCandidateApproved=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaRemoved=false
formalJarChanged=false
experimentalJarChanged=false
poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR
checkpointFrontExport=NOT_EXPORTED_BY_FROZEN_JAR
allocationAccounting=CLOSED_FORM_SCHEDULE_RECONSTRUCTION
```

## 8. 移交与停止

- 按任务书 §16：本轮到 50k 分析完成即停止；250k 的预登记、矩阵与执行须用户单独批准后另开工作包。
- 若 250k 获批：候选臂 = C3（betaMax=0.35），对照臂 = C0（当前 A4 语义）；建议在 250k 工作包中一并裁决是否以可改造的实验链补齐前沿级共同FE口径（本批 D1 缺口）。
- 历史与失败结果全部保留；本阶段未修改 20k 报告、冻结 Jar、20k 同步数据与任何既有证据文件。

---

## SUPERSESSION NOTE（2026-08-31 追加，原文未改动）

本报告的 §7 最终机器状态与 §4 候选筛选中的"唯一保留候选 C3"结论，已被
`../12-50k-decision-correction/50K_DECISION_CORRECTION.md` **部分取代**：

- **维持**：执行验收（16/16）、剂量分辨门 PASSED、C1 出局（完整门失败）。
- **取代**：C2 的出局理由（双口径 TWC 符号翻转）被认定为 `MINOR_FLUCTUATION`
  （幅度 ≈0.235pp，pooled +0.1293% vs −0.1059%，无 2/3 seed 一致性；且原双口径
  实为"终态完整前沿 HV/IGD vs 共同FE标量极值"的口径不对称比较，淘汰证据不足）。
- **修正后裁决**：`TWO_CANDIDATES_ADVANCE_TO_250K`（C2、C3 均晋级）。
- 本报告 §7 的 `budgetSensitivityConflict=true(C2_only)` 相应降级为
  `RETIRED_AS_MINOR_FLUCTUATION`；其余字段不变。
