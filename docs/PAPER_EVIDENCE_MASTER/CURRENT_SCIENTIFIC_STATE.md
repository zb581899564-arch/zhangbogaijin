# 当前科学状态与冻结边界

> **2026-08-30 D-110更新**：旧A4冻结物改作`A4_LEGACY`，当前不存在Final。活动治理路线为
> [`../V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md`](../V35_COMPETITIVE_SUPERIORITY_EXECUTION_ROADMAP.md)；
> 当前只允许`V35_GAP_PROBE_P0`的0-FE预登记，不授权运行。

## 1. 正式算法语义

```text
decoderMode              = FM3
shiftMode                = NONE
familyMode               = DEGENERATE_SINGLE_FAMILY
setupMode                = SEQUENCE_INDEPENDENT
objectives               = [0,1,6] = [Cmax,TEC,TWC]
subSwarmMix               = [G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]
                          = [20,40,20,20]
pddrSelectionMode        = GLOBAL_ORIGINAL
localSearchOrder         = CA-TA-Lite -> inherited LS
dualQ                    = warmup 10%, P=5, G=5, rho=0
directionTeacherPool     = disabled
pressureStrictMask       = disabled; BAL opens N1--N5
PF-SDST                  = disabled
MaxFEs semantics         = PHASE_CONSISTENT_BUDGET_TERMINATION
```

正式冻结 Jar：

```text
SHA-256 = 8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

## 2. Stage2 当前状态

```text
formalMatrixStartedHistorically = true
formalMatrixRunning             = false
formalMatrixPaused              = true
plannedPhysicalRuns             = 4500
acceptedFairnessGroups          = 12
acceptedPairedRuns              = 60
excludedUnpairedCompleted       = 8
excludedPartialAttempts         = 7
formalPFrefGenerated            = false
formalStatisticsGenerated       = false
nextAlgorithmGate               = V35_GAP_PROBE_P0
nextAlgorithmRuns               = 0 (zero-FE preregistration only; no run authorized)
candidateDecision               = A4_NOT_PROMOTED; A2_NOT_PROMOTED
currentPrimaryCandidate         = NONE (no V35 Final candidate promoted)
sampledReproductionAccepted     = false
fullReproductionAccepted        = false
```

训练机暂停标记：

```text
/home/inspur/aicomp/zhangbo-v35-stage2-master-v2-20260823/formal/PAUSED_BY_USER.properties
```

## 3. 当前先导结果

有效数据范围：

```text
instance = 100_2_3_1
seeds    = 20260808..20260819
arms     = A0..A4
MaxFEs   = 500000
runs     = 12 groups × 5 arms = 60
```

统一先导参考前沿为60条最终前沿的raw-objective去重和严格Pareto过滤，共1979点。

| 比较 | median ΔHV | median ΔIGD | median ΔCmax | 解释 |
|---|---:|---:|---:|---|
| A0→A1 | -1.56% | +7.58% | +2.04% | DSCR方向有价值，但seed稳定性不足 |
| A1→A2 | +29.90% | +6.88% | -1.32% | CFVF改善前沿，但牺牲部分Cmax/TWC |
| A2→A3 | -16.24% | -24.93% | +1.25% | Qp/个人档案组合必须复核 |
| A3→A4 | +22.82% | +37.85% | +1.31% | CA-TA-Lite对A3有稳定恢复作用 |
| A0→A4 | +25.24% | +19.02% | +3.95% | 单实例先导信号积极，非论文正式结论 |

A4相对A0的TEC中位改善约5.40%，TWC中位退化约1.45%。

## 4. PDDR状态

- A4有6/12次生成过优于最终global的Cmax记录。
- 全12次gap中位数0.08%；发生记录丢失的6次中位数0.55%，最坏2.51%。
- A0--A3同样存在记录丢失，不能把现象直接归因于CA-TA。
- FC5/FC6及Stage2候选生命周期证据已裁决当前保持`KEEP_GLOBAL_ORIGINAL`；BP与
  Region-aware均不得回主线。方向极值可能被综合型选择取舍仍是已记录的结构性限制，
  不是“已确认PDDR程序错误”或立即改造的授权。

## 4.1 A2→A3最小因果拆分（2026-08-24）

新增的诊断性四臂D0--D3在`20_2_3_1`、seed `20260822..20260824`、同初群、50k FE下完成12/12条：

- D0→D1（档案+确定性方向个人领导）在2/3 seed触发稳定退化，median ΔHV=-9.02%、
  median ΔIGD=+102.56%；D1有30,000次可用档案领导选择且fallback=0，不能写为档案空或退回历史。
- D1→D2（再加同步Qp四动作及未裁剪奖励）在3/3 seed触发稳定退化，median ΔHV=-4.32%、
  median ΔIGD=+30.11%。
- D2→D3（再加10%预热/P5-G5冻结）未通过稳定退化门，故冻结时序不是已观察退化的唯一根因。

随后Q0观察学习与Q1冷启动tie-break诊断均已完成：Q0证明Qp动作策略本身与TD学习的影响仍不可
加性拆开；Q1未达到预注册改善门，不能晋升为修复。因此A2→A3的微观问题不再阻断整体候选验证。
当前唯一允许的新算法门是A2/A4多实例整体确认；详情见
`docs/V35_A2_A4_MULTISCALE_CONFIRMATION_PROTOCOL.md`。诊断历史仍见
`docs/evidence/V35-A2-A3-DECOMPOSITION/05-analysis/ACCEPTANCE_REPORT.md`、
`docs/evidence/V35-A3-D2-QP-SETTLEMENT/`和`docs/evidence/V35-A3-D3-QP-COLD-START-TIE/`。

2026-08-25的D-103确认已完成：60/60运行、30/30配对均通过完整性和公平性验收；总体中位
`ΔHV=+1.50%`、`ΔIGD=+7.24%`、`ΔCmax=+1.72%`，但100-job pooled HV/IGD为负，且
`100_5_3_1`触发单实例否决门。因此A4不能晋升Final候选。随后独立A0/A2确认也已完成：60/60运行、
30/30配对有效，但总体`ΔCmax=-0.7410%`、仅3/6实例HV/IGD同时非负，且`100_8_3_1`触发否决门，
故A2同样不能晋升Final候选。详见
`docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/`。
后续A2证据入口为`docs/evidence/V35-A2-FINAL-CANDIDATE-CONFIRMATION/`。

## 5. 允许与禁止的结论

允许：

- 规范FM3编解码、三目标重构和疲劳公式已获工程与示例验证。
- DSCR、CFVF、Qp/Qg、谱系档案和CA-TA-Lite均已接入正式代码路径。
- DOE1未找到可稳定替代`20/40/20/20`的子群比例，因此保留原比例。
- 当前单个100-job实例的五臂先导显示A4相对A0有积极信号，直接A2→A4也为正向先导；但机制贡献非单调。
- A3不能写成独立正贡献；A4与A2均已在各自独立的预注册六实例、五seed确认中未通过，不能继续靠
  Qp、CFVF、PDDR或单实例信号挽救。当前不存在已晋升的Final候选。

禁止：

- “A4已经在45实例上显著优于全部算法”。
- “A3/Qp已经证明有效”。
- “PDDR已经确认有bug或必须修改”。
- 恢复4500矩阵、把A4称作最终算法，或在确认数据上调Qp/PDDR/CA-TA以挽救A4。
- 使用P25D、Shift-on或不完整Stage2运行构建论文reference。
- 把先导reference称为论文理论真值或最终PFref。

## 6. 非支配档案与展示前沿（2026-08-24）

- 当前活动档案仍为`UNBOUNDED_FULL`，PDDR仍为`GLOBAL_ORIGINAL`；没有转正任何裁剪方案。
- A4前沿点较多暂不判为缺陷；完整`decision-front`继续用于科学指标。
- K30代表集只用于论文图形，K25/K50只用于等基数敏感性，均不进入主reference。
- ND1至ND4候选已完成本地代码预注册，但没有上传或运行，状态统一为`DORMANT_NOT_RUN`。
- 4500正式矩阵继续暂停。档案路线与证据边界见ROADMAP D-094至D-096和
  `docs/V35_ND_ARCHIVE_AND_CARDINALITY_ROADMAP.md`。

## 7. V35-FC5-MIDHORIZON-DIAGNOSTICS-V2 遥测诊断门（2026-08-26 — 2026-08-27 整改中，2026-08-28 新 Jar 4544）

```text
telemetryContractFrozen           = true
observerSkeletonCompiled          = true
historicalReferenceCalculatorValidated = true
runtimeObserversWired             = true
real2kBehaviorEquivalence         = true
real20kBehaviorEquivalence        = true
realTelemetryOverheadValidated    = true
diagnosticToolingValidated        = false
250kReadyForPreregistration       = false
250kStarted                       = false
algorithmChanged                  = false
searchSemanticsChanged            = false
pddrChanged                       = false
formalMatrixRunning               = false
FC5                               = INCONCLUSIVE
250kApproved                      = false
```

- 四个观察者已真实接入冻结主循环（OFF=null 短路、5 处守卫），OFF/ON 15/15 行为哈希逐位一致。
- B1 Checkpoint：`actual-nominal<5000` 时捕获，否则 `CHECKPOINT_UNOBSERVABLE` 并计 `observerErrors`（A2 20k=3、A4 20k=1，2k=0）。
- B2 三 front：`attachPassiveArchive=true`，`observedFullFront` 为真实 ND 前沿（此前 `NOT_APPLICABLE`），`decision/observed/full` 三前沿真实落地。
- B3 PDDR：32 列对齐（`candidateId/stableFingerprint` 双写、`[0,1,6]` 归一化、`stableLineageId`、`generation` 真实、`parentId` 非 -1、`semanticRoleBefore` 真实角色 `G1_CMAX` 等），`hdr==data==32`。
- B4 教师：24 列对齐（补 `cacheType`），浓度按指纹列聚合，新增 `qAction/.../offspringImproved` 7 列占位（当前 `NOT_APPLICABLE`，聚合逻辑已正）。
- B5 CA-TA：事件扩展 6 生命周期列，`enteredMergePool` 按 `accepted` 真实、`selectedByPddr` 经 PDDR 指纹回填真实（N1 1/11、N3 52/83 等），摘要 `pddrSurvived` 不再复用 `accepted`；`enteredArchive/survivedNextGeneration/teacherUsedLater/improvedOffspringLater` 仍 `NOT_OBSERVED` 待谱系归档与下一代钩子。
- B6 预算：诊断用 `allowTerminalPartialFormalQPhase=true` 变体以命中 2k/20k 精确预算（与冻结 500k `tail<5000` 差异已显式说明），OFF/ON 在同一变体内等价。
- B7 行为哈希：已补 `rngConsumptionSequenceHash/generatedCandidateSequenceHash` 并纳入等价（15/15）。
- B8 证据：`evidence-sha256.tsv` 97 行/96 项（纠正误写 93），历史 reference `A42D6F15.../C34782CC...` 未动。
- 新测（Jar `4544B467CE9279011BE7A55D3872848265E2DA8E041B7874B9BA5ADC39016FCA`）：2k 78/51 行、20k 191/423/201/638 行（含 9/3 不可观测），A4 cata 426/473 行；OFF/ON 15/15 一致；开销 4/4 PASS（-0.26%/1.95%/1.14%/6.22% 均 ≤15%）。
- 冻结正式 Jar `8DAD8F40...` 未改动。`diagnosticToolingValidated` 仍 `false` 直至 CA-TA 剩余 4 生命周期与 `physicalSlotBefore` 完全物理化；`250k` 仍不启动。

---

## V35-GAP-PROBE-V2（2026-08-30，D-110 主线第一阶段完成）

```ini
gapProbeStarted                   = true
gapProbe500kCompleted             = true
gapProbeVerdict                   = GAP_GT_15
gapProbeRed                       = false
runs500kAccepted                  = 16/16
fairnessGroupsValid               = 4/4
shakedown20k                      = SHAKEDOWN_PASSED (4/4, mechanism gates real)
strongestExternal                 = SPEA2-F (frozen rule: mean rank HV+IGD over 2x2)
algorithmChanged                  = false
PDDRChanged                       = false
CFVFChanged                       = false
DualQChanged                      = false
CaTaChanged                       = false
formalMatrixRunning               = false
validationStarted                 = false
FinalCandidateApproved            = false
FINAL_FROZEN                      = false
```

- 四算法 = A4-Pacing（冻结 Jar 8DAD8F40…）/A0(HMOPSO-QGS-F)/SPEA2-F/NSGA-II-F
  （外部两臂 = OFFICIAL_JMETAL_CORE，比较 Jar 966DA3D2…）；
  实例 = 50_2_3_1（DEVELOPMENT 50-job）+ 100_5_3_1（registered hard）；
  seeds = 20260827/20260906（均 performance-unexposed DEVELOPMENT 用途）。
- 执行域：训练机 `/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830`
  （23 输入/Jar/计划文件逐字节核验后运行；16×500k + 4×20k 全部独立 JVM）。
- 逐实例独立 reference（8 条 ACCEPTED raw front 严格 ND 并集，顺序无关自检通过），
  禁止复用任何历史 reference；HV/IGD/三极值统一重算。
- Gap 裁决：50_2_3_1 = GAP_WITHIN_5×3（A4 对 A0 gapHV −50.5%、对 NSGA-II-F −16.0%、
  对 SPEA2-F HV 差距仅 4.0% 且 IGD 领先 38.5%）；100_5_3_1 = GAP_5_TO_15（vs A0）、
  GAP_GT_15×2（vs SPEA2-F HV +63.5%/IGD +260.7%；vs NSGA-II-F +67.8%/+311.3%）。
  RED=false（种子级稳定性条件不成立）。
- 科学含义：冻结 A4（A4_LEGACY）在 50-job 全面领先；困难 100-job 上 A4/A0 相对
  官方经典核的 Pareto 覆盖质量大幅落后（极值方向未崩塌，minCmax 接近全场最优），
  与 250k"覆盖收缩"机制候选一致——这是 leverage audit 与单一 repair family
  的量化输入。
- 后续最强 external = SPEA2-F（均秩 2.25 < A0 2.50 < NSGA-II-F 2.75，冻结规则
  选定，仅开发对照用途）。
- 下一步 = 0-FE leverage audit + 单一 repair family 决策，需用户批准；
  不得自动进入 Validation/Final/正式矩阵。
- 证据：`docs/evidence/V35-PFC5-GAP-PROBE/{02..06}-v2-*/`（分目录台账逐条反算
  ALL_MATCH：03=64、04=250、05=8、06=2）。

### V35-GAP-LEVERAGE-AUDIT-V1（2026-08-30，纯离线 0-FE）

```ini
leverageAuditCompleted            = true
selectedRepairFamily              = CATA_BUDGET（单旋钮 betaMax：C0=0.65 当前/C1=0.55/C2=0.45/C3=0.35）
repairImplemented                 = false
repairExperimentStarted           = false
DOEStarted                        = false
```

- 核心证据：A4 机制调度实例盲（结构计数 50/100 实例逐值相同）；局部预算 37.98%
  恒定且 62.3% 落在困难实例 bestCmax 停止改善（129k–252k）之后；A0 反事实
  （无 DYNAMIC_BETA 时 LS 吞 70–84%）；betaMax 参数化注入点与 CLI 通路实证。
- H_CFVF_QP_GUIDANCE：9 注入点逐判无 ≥10% 非禁区可达旋钮（排除，假设未否证）；
  H_CREDIT_TIMING：离线不可测（排除）。
- 最小实验仅预登记（20k 门 8 条 → 50k 门 24 条 → 250k 16 条条件性；新 seed
  20260907/20260914），未实现未运行；另登记 EXTERNAL_ADAPTER_MEMORY_DEBT
  （外部适配器 100-job 500k 需 100g 堆；修复仅限观测/输出保留且须 OFF/ON 等价）。
- 证据：`docs/evidence/V35-GAP-LEVERAGE-AUDIT/`（台账 10 文件反算 ALL_MATCH）。

## 追加（2026-08-31）：V35-GAP-LOCAL-FE-PACING-REPAIR-V1 状态

- Gap Probe V2（GAP_GT_15）后的单一 repair family LOCAL_FE_PACING（singleKnob=betaMax，
  原名 SELECT_CATA_BUDGET_REPAIR 已更正，CaTaHarmfulNotClaimed=true、rootCauseProven=false）。
- 独立实验Jar 0788580… + 薄Runner + 快照绑定 20k 机制门 **10/10 PASSED**：
  C0==REF_A4 逐位等价；FE回流成立（outerCycles 2→3、cfvf 10000→15000）；
  CFVF/Dual-Q/CA-TA 全触发；PDDR=GLOBAL_ORIGINAL 未动；池级PDDR归因
  NOT_EXPORTED_BY_FROZEN_JAR（冻结Jar遥测限制，如实登记）。
- 50k 预登记草案 16 条（更正旧24条误计），50kStarted=false 待用户批准。
- 证据：docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/；训练机
  /home/inspur/aicomp/zhangbo-v35-local-fe-pacing-repair-20260831。
- 正式矩阵继续暂停；最终候选未批准。

### V35-GAP-LOCAL-FE-PACING 50K（2026-08-31，完成：剂量分辨 PASSED，唯一候选 C3）

```ini
20kImplementationGate              = PASSED
strict20kDoseGate                  = NOT_FULLY_PASSED
doseResolutionAt20k                = NOT_RESOLVED
50kPurpose                         = DOSE_RESOLUTION_AND_PERFORMANCE_SCREEN
50kStarted                         = true
50kCompleted                       = true
runsAccepted50k                    = 16/16
fairGroupsPassed50k                = 4/4
scheduleValidation50k              = 16/16
doseResolution50k                  = PASSED
localFeShare50k                    = C0 0.3764 > C1 0.3364 > C2 0.2980 > C3 0.2842
F_common                           = 40000
selectedCandidates                 = C3(betaMax=0.35)
budgetSensitivityConflict          = true(C2 only)
C2ExitReason                       = 终态与common-FE在50实例TWC符号翻转(+1.19% vs -0.11%)
C1ExitReason                       = 困难实例无改善信号(ΔHV_hard=-0.19%, ΔIGD_hard=+3.77%)
250kEligible                       = true
250kPreregistered                  = false
250kStarted                        = false
DOEStarted                         = false
validationStarted                  = false
FinalCandidateApproved             = false
formalMatrixRunning                = false
PDDRChanged=false; CFVFChanged=false; DualQChanged=false; CaTaRemoved=false
formalJarChanged=false; experimentalJarChanged=false
poolLevelAttribution               = NOT_EXPORTED_BY_FROZEN_JAR
checkpointFrontExport              = NOT_EXPORTED_BY_FROZEN_JAR
allocationAccounting               = CLOSED_FORM_SCHEDULE_RECONSTRUCTION
```

- 20k 结论范围更正（append-only）：`06-20k-scope-correction/`；聚合剂量门入 `build_gate.py`
  （20k 重跑 NOT_RESOLVED，exit 2）。
- 50k 冻结预登记：`07-50k-preregistration/50K_PREREGISTRATION.md`（偏差 D1 检查点front不可导出、
  D2 分配上限闭合重建、D3 runId 前缀，均运行前登记）。
- 远端执行与验收：`08-remote-50k/`（16/16，训练机 zhangbo-v35-local-fe-pacing-50k-20260831）。
- 剂量分辨：`09-dose-resolution/`；性能筛查：`10-performance-screen/`；
  最终裁决：`11-50k-decision/50K_REPAIR_DECISION.md`（ONE_CANDIDATE_ADVANCES_TO_250K，候选 C3）。
- 主Agent独立复算：`11-50k-decision/MAIN_AGENT_INDEPENDENT_CHECK.py`（PASSED）。
- 全树 SHA-256：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/evidence-sha256.tsv`
  （1255 条目，反向复算 missing=0 mismatch=0）。
- 下一步须用户批准：250k（候选臂 C3 + 对照臂 C0）；本轮按任务书在此停止。

### 50k 候选裁决勘误（2026-08-31，append-only）

```ini
50kExecutionAccepted              = true
doseResolution50k                 = PASSED
C1Rejected                        = true
C2EligibleFor250k                 = true
C3EligibleFor250k                 = true
50KDecision                       = TWO_CANDIDATES_ADVANCE_TO_250K
original50KDecision               = ONE_CANDIDATE_ADVANCES_TO_250K(superseded)
c2BudgetSensitivityConflict       = RETIRED_AS_MINOR_FLUCTUATION
250kArms                          = C0,C2,C3
250kStarted                       = false
```

- 勘误依据：双口径实为前沿 HV/IGD vs 标量极值的口径不对称（D1）；C2 的 TWC 翻转
  仅 ≈0.235pp 且无 2/3 seed 一致性；C2 终态 HV/IGD 双实例均优于 C3。
- 证据：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/12-50k-decision-correction/`。

### V35-GAP-LOCAL-FE-PACING 250K 确认（2026-08-31，完成：NO_REPAIR_CANDIDATE）

```ini
50kDecision                         = TWO_CANDIDATES_ADVANCE_TO_250K
checkpointObserverValidated         = true
250kRunsAccepted                    = 18/18
fairGroupsPassed                    = 6/6
250kDecision                        = NO_REPAIR_CANDIDATE
C2Pass                              = false(normalSafety/hardImprovement/singleSeedDisaster FAIL)
C3Pass                              = false(checkpointConsistency CONFLICT; 4 gates PASS)
C3ConflictEvidence                  = 50_2_3_1 ck100000 dHV=-6.87% / ck150000=-5.15%, 3/3 seeds, terminal +0.19%
repairFamily_LOCAL_FE_PACING        = REJECTED_AT_250K_CONFIRMATION
PROVISIONAL_250K_REPAIR_CANDIDATE   = false
500kStarted=false; DOEStarted=false; validationStarted=false
FinalCandidateApproved=false; FINAL_FROZEN=false; formalMatrixRunning=false
PDDRChanged=false; CFVFChanged=false; DualQChanged=false; CaTaChanged=false; formalJarChanged=false
```

- 检查点观察器（V2 实验Jar，正式Jar零改动）：OFF/ON 等价 + 冻结忠实性全部 PASSED。
- 50k 开发筛查正信号（C2 ΔHV_hard +10.72%、C3 ΔIGD_hard +25.13%）未在 250k 复现；
  C3 终态平价仅末段出现（检查点门为该轮预登记的实质性阈值）。
- 证据：`docs/evidence/V35-GAP-LOCAL-FE-PACING-REPAIR/{12..18}-*/`；裁决
  `18-250k-decision/250K_REPAIR_DECISION.md`。

### V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT-V1（2026-08-31，只读审计：NO_ACTIONABLE_LEVER）

```ini
auditScope                        = READ_ONLY (newFEConsumed=0)
evidenceGovernanceClosure         = COMPLETE (19-evidence-governance-correction, signoff=true)
H1_H2_PDDR_COMPRESSION            = INSUFFICIENT_EVIDENCE (front-level counter-evidence + 250k telemetry gap)
H3_TEACHER_CONCENTRATION          = NOT_CONFIRMED (hard-vs-normal gap +1.75pp << 20pp)
H4_CFVF_QP_BIAS                   = INSUFFICIENT_EVIDENCE (source-level ND/HV attribution NOT_EXPORTED)
finalDecision                     = NO_ACTIONABLE_LEVER
rootCauseCandidate                = NONE
observationalFinding              = coverage collapse points to generation-side diversity deficit,
                                    not retention-side compression (potentialHvRecovery <= 0.79%, 0/90 >= 2%)
diagnosticGapRegistered           = 250k candidate-level PDDR/source-attribution telemetry with normal-instance control
localFePacingRepairFamily         = PILOT_REJECTED
betaMax=0.65; PDDR=GLOBAL_ORIGINAL
newRepairImplemented=false; newExperimentStarted=false; DOEStarted=false
500kStarted=false; FinalCandidateApproved=false; formalMatrixRunning=false
```

- 证据：`docs/evidence/V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT/`（00-07 +
  `07-decision/PARETO_COVERAGE_LEVERAGE_AUDIT_REPORT.md` + FINAL_DECISION.properties）。

### Campaign P1 来源贡献诊断（2026-08-31，完成：NO_SOURCE_LEVEL_FAILURE）

```ini
campaign                           = V35-FINAL-COMPETITIVE-RECOVERY-CAMPAIGN(charter frozen)
sourceLedgerValidated              = true(V3 diagnostic jar bbb9ccd6..., OFF/ON 0 DIFFER)
diagnosticRuns                     = 6/6 (C0 x 2 instances x 3 seeds x 100k, ledgerRows==actualFE)
valueEfficiency.CFVF               = 0.961/1.108 (50/100-job median; per-seed >= 0.395)
valueEfficiency.INHERITED_LS       = 1.150/0.863 (per-seed >= 0.589)
valueEfficiency.CATA               = 0.038/0.046 (FE share 3.1% < 5% materiality gate)
diagnosticVerdict                  = NO_SOURCE_LEVEL_FAILURE
algorithmOptimizationClosed        = true
repairFamilyBudget                 = UNSPENT
500kStarted=false; DOEStarted=false; FinalCandidateApproved=false
formalMatrixRunning=false; FINAL_FROZEN=false
PDDRChanged=false; CFVFChanged=false; DualQChanged=false; CaTaChanged=false; formalJarChanged=false
```

- 含义：占评估量96.8%的两大来源价值效率与生成占比相称——生成侧无失效模块；
  唯一低效（CATA）低于实质性门槛。HV/IGD困难实例表现为实例难度驱动的搜索分布特性，
  不做未经因果确认的机制声明。
- 下一步（须用户批准）：P5 Final对比（A2 vs A4，500k多实例）或直接冻结当前语义。
- 证据：`docs/evidence/V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1/`；
  章程：`docs/evidence/V35-FINAL-COMPETITIVE-RECOVERY-CAMPAIGN/`。

### SOURCE-ATTRIBUTION-500K Phase A0（2026-08-31，完成：PHASE_A0_PREREGISTRATION_PASSED）

```ini
phaseA0Decision = PHASE_A0_PREREGISTRATION_PASSED
NORMAL = 100_2_3_1 (CONTAMINATED_DEVELOPMENT; 12 accepted A4 500k; dual-positive; seed 20260901 free)
HARD = 100_5_3_1 / 20260901 / A4 / 500k (CASE_SELECTED_DIAGNOSTIC_ONLY, binding closed)
references = HARD Failure-Replay contract (pfref 757pts) + NORMAL 12 raw fronts (cold archive, 12/12 hash match)
sourceTaxonomy = GLOBAL_CFVF / CATA / INHERITED_LS / PARENT_CARRYOVER (4 classes, frozen)
thresholds = fallback frozen (WHVG 2.0pp / ExclusiveND 10.0pp / 2 windows); t_div fallback (HV 1.0pp AND IGD 10pp, 2 ckpts)
wallClockInfluencesSearch = false (A4/A2 arms; 7-site static audit)
observerImplemented = false; newFEConsumed = 0; remoteExperimentUploaded = false
sourceAttribution500kStarted = false
```

- 未来执行顺序冻结：20k OFF/ON工程门→Observer冻结→SA-HARD→复现门→SA-NORMAL→(仅G1)SA-A2
  →G1-G4→强制停止。禁止第三实例/第二诊断seed/其他arm。
- 证据：`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration/`（20文件，0/0闭合）。

### Phase A0 修正（2026-09-01，PHASEA0-CORRECTION-V1）

```ini
phaseA0Decision = PHASE_A0_PREREGISTRATION_PASSED (corrected; initial version returned by independent acceptance)
corrections = counterfactual producerSet attribution (multi-source duplicate triples) + decomposed memory model (x25 formula deprecated)
verification = T1-T8 dual-path PASS (developer selftest + main-agent independent with contract-given expectations)
normalControlResolved = true (NORMAL=100_2_3_1, unchanged)
observerImplemented = false; newFEConsumed = 0; remoteExperimentUploaded = false
sourceAttribution500kStarted = false; memoryPreflightExecuted = false; memoryGatePassed = false
```

- 证据：`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/00-preregistration/`（含
  PHASE_A0_CORRECTION_NOTICE.md、metric-counterfactual-tests.csv、
  memory-model-correction.md、phase-a0-correction-verification.md、pre-correction快照）。
- 证据重包装（2026-09-01）：Phase A0清单闭合并跨目录绑定独立复核链（30行0/0；
  phaseA0Decision=PHASE_A0_PREREGISTRATION_PASSED，evidenceRepackComplete=true）。

### SOURCE-ATTRIBUTION Observer 工程门（2026-09-01，完成：全门通过，Observer冻结）

```ini
observerImplemented = true (V4 jar 43 classes major=52)
observerBehavioralEquivalent = true (20k OFF/ON 14/14 artifacts byte-identical)
observerCompleteness = ledgerRows==actualFE==15258, UNSET=0, errors=0, dropped=0
memoryGatePassed = true (estimated500kPeak ratio=0.326 < 0.60)
observerSchemaFrozen = true; observerJarFrozen = true
sourceAttribution500kEligible = true; sourceAttribution500kStarted = false
formalJarChanged = false; PDDRChanged = false (all frozen semantics untouched)
```

- 证据：`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/{01..06}/`。
- 下一阶段须用户批准：SA-HARD 500k → 复现门 → SA-NORMAL → G1/G3 → (仅G1)A2条件臂 → G1-G4裁决。
- Observer内存流式修正（2026-09-01）：初版flushedEventLedger内存驻留问题已修正为真流式
  （磁盘临时文件）；行为等价更正为12字节一致+2掩码等价+1测量；内存门重算ratio=0.2891<0.60
  → PASS；冻结文档含完整Jar SHA `78bf4d30…46565`。500k待用户批准。

### Source Observer V5合同纠正（2026-09-01，工程门通过）

```ini
v4FailureReplayAccepted = true
v4SourceAttributionSchemaCompliant = false
v5ObserverJarSha256 = 1A73E3CF025F7CFDB47BDE38A7B34E8F8B0810958F61323A5D3CBC35272C8C9E
v5ObserverSchemaCompliant = true
v5ObserverBehavioralEquivalent = true
v5MemoryGatePassed = true (estimated ratio 0.3221)
sourceAttribution500kEligible = true
correctedSaHard500kStarted = false
saNormalStarted = false
sourceAttributionRootCauseEstablished = false
```

- V4的500k失败轨迹与前沿复现有效，但其来源账本不符合Phase A0合同，因此不允许写来源根因。
- V5补齐真实生命周期、25k nominal窗口、轮次上下文、严格B0、parent lineage和Qp action。
- 本地2k及训练机20k OFF/ON均通过；正式Jar仍为`8DAD8F40…BAD8B9`，算法/PDDR/CFVF/Dual-Q/CA-TA均未变。
- 下一步如获批准，只重跑一条V5 SA-HARD 500k；通过失败复现与来源证据验收后才允许SA-NORMAL。

### V5 SA-HARD 500k（2026-09-01，完成：运行验收+失败类复现通过，来源证据仅HARD侧）

```ini
V5_SA_HARD_500K_STARTED=true
V5_SA_HARD_500K_COMPLETED=true
RUN_ACCEPTANCE=PASSED(61/61)
FAILURE_CLASS_REPRODUCTION=PASSED
frontSha256Raw=f3755d83a2acb4280ff8dd566025340c8b64edc71050e05bbd6a3ff4b1239bdd（=历史A4，规范排序亦一致）
hv=0.5545772540415207  igd=0.15898065502479636  deltaHV=-0.3155  deltaIGD=-1.7503
actualFE=decoderCalls=500000  remainingFE=0  utilizationRate=1.0  EXACT_MAX_FE
sourceLedgerRows=500000  lifecycleRows=2430744(十类)  b0=11/11逐点一致  checkpointOvershoot=0
HARD_WINDOW_EVIDENCE=COMPUTED(20/20)
HARD_NORMAL_DEFICIT=NOT_COMPUTABLE  G1/G3=UNDECIDED_NEEDS_SA_NORMAL  t_div=NOT_COMPUTABLE
SOURCE_LEVER_CANDIDATE=NONE  SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
SA_NORMAL_STARTED=false  formalMatrixRunning=false  formalJarChanged=false
```

- 含义：V5为纯观察重跑，确定性复现冻结A4失败类，来源账本首次满足Phase A0合同（nominal窗口/轮次上下文/真实生命周期/严格B0）。
  但**只有HARD一条轨迹**，hard–normal差值门未建立，因此不得宣布任何来源级根因或修复杠杆。
- 禁止：把"GLOBAL_CFVF占62%评价量"或任何窗口份额写为根因；用单侧轨迹自配对构造t_div代理值；据本包启动SA-NORMAL。
- 证据：`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/09-v5-sa-hard-500k/`（110项包级清单0缺失0不匹配；
  两文件≥100MB已G盘冷归档+SHA登记）。决策：`07-decision/SA_HARD_V5_DECISION.{md,properties}`。

### V5 SA-NORMAL 500k与Phase A G4收口（2026-09-02，完成：HARD–NORMAL分析，G4出口）

```ini
SA_NORMAL_V5_STARTED=true
SA_NORMAL_V5_COMPLETED=true
RUN_ACCEPTANCE=PASSED(56/56)
actualFE=decoderCalls=500000  remainingFE=0  utilizationRate=1.0  EXACT_MAX_FE
sourceLedgerRows=500000  lifecycleRows=2488377(十类)  b0=5/5逐点一致  checkpointOvershoot=0
snapshotSha256=ea19f691…  snapshotSource=MATERIALIZED_ZERO_FE(生成器同源性证明)
initialPopulationHashV35=1fdf0820…  heapPeak=2.98GB(<4GB,no OOM)
t_div=NOT_REACHED
G1_GLOBAL_CFVF=INSUFFICIENT(窗口deficit信号存在但时序前提t_div不满足)
G3_CATA=NOT_TRIGGERED(无持续deficit+FE占比<5%)
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
SOURCE_LEVER_CANDIDATE=NONE
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
SA_A2_CONDITIONAL_ELIGIBLE=false  SA_A2_CONDITIONAL_STARTED=false
formalMatrixRunning=false  formalJarChanged=false
```

- 含义：HARD与NORMAL两条500k V5轨迹在冻结Phase A0合同下完成逐窗口来源归因比较。GLOBAL_CFVF在窗口层确有
  WHVGShare/ExclusiveNDShare deficit（NORMAL的CFVF份额更高），但**没有coverage divergence锚点**（t_div=NOT_REACHED：
  HARD未在decision-front HV/IGD上相对NORMAL持续落后）→G1的时序前提不成立→INSUFFICIENT。G3无信号。
  **G4_NO_ACTIONABLE_LEVER**：Phase A结束，OLD_A4_DIAGNOSTIC_CLOSED=true，永久停止追PDDR/pacing/teacher/source扩大诊断。
- 初始种群快照：`100_2_3_1×20260901`此前无已执行记录，使用项目规范零-FE物化器确定性物化；生成器同源性证明
  （同一生成器复现HARD快照逐字节一致）。attempt1漏传binding秒退（0FE），补齐后attempt2成功。
- 禁止：把CFVF 62%预算占比或任何窗口份额写为根因；FIRST_ADMISSION归因；新调阈值或重建reference。
- 限制：B0退化基线（HV_0=0）使i=1 hvProgress数值不稳定但不影响lag判定；PA/QP利用层约53%事件无法按来源归属
  （NOT_ATTRIBUTABLE_BY_FINGERPRINT_JOIN，如实登记未猜测）。
- 证据：`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/10-v5-sa-normal-500k/`（121项清单0缺0不匹配；
  两文件≥100MB G盘冷归档+SHA登记）。决策：`06-decision/SA_NORMAL_V5_DECISION.{md,properties}`。
  分析：`04-hard-normal-analysis/`（4份CSV+报告+decision.properties）。

### V35-QP-V2-SINGLE-AXIS Phase B1：语义欠定义停止（2026-09-02，D-116）

```ini
QP_V2_SEMANTICS_UNDERDEFINED=true
QP_V2_SEMANTICS_FROZEN=false
QP_V2_IMPLEMENTED=false
QP_V2_EXPERIMENT_STARTED=false
K1_BEHAVIOR_EQUIVALENT=false (NOT_OBSERVABLE, 未实现未运行)
K2_K4_MECHANISM_TRIGGERED=false
PHASE_B1_ENGINEERING_GATE=BLOCKED (K语义欠定义)
QP_V2_250K_ELIGIBLE=false
QP_V2_250K_PREREGISTERED=false
QP_V2_250K_STARTED=false
DOE_AUTHORIZED=false  VALIDATION_AUTHORIZED=false  FORMAL_AUTHORIZED=false
formalMatrixRunning=false  newFEConsumed=0
PDDRChanged=false  CFVFChanged=false  DualQActionRewardChanged=false  CaTaChanged=false
formalJarChanged=false (Jar 8DAD8F40…BAD8B9 前后实测一致)
remoteOriginMain=051877aa (IN_SYNC, 此前推送阻塞已解除)
```

- 含义：用户授权的 Qp-v2 单轴 K（K=1,2,3,4）工作包在第一硬门（语义来源核查）处 fail-closed。
  获批材料对 K 仅有轴取值、K=1≡A4 要求、同时调整禁令与证明协议；计数对象、作用候选集合、
  K>1 选择/破平规则、候选不足 fallback、RNG 契约、K=1 还原机制全部缺失。当前 A4 的 Qp 是
  "动作→唯一候选"确定性映射（`ZhangBoQpCandidateSelector`），不存在现成多元素候选集，
  任何 Top-K 实现都是自行发明算法。未实现、未编译、未运行、未消耗 FE。
- Phase A G4 结论不受影响：OLD_A4_DIAGNOSTIC_CLOSED=true 继续有效；Phase B 是假设驱动的
  新候选路线，工程门 BLOCKED 不涉及任何性能结论。250k、DOE、Validation、Final Freeze、
  正式矩阵均未启动，须用户另行批准。
- 证据：`docs/evidence/V35-QP-V2-SINGLE-AXIS/`（00-governance / 01-semantic-source-audit /
  07-decision；清单 4+1 项反向复算闭合）。重启须用户先冻结含 8 项缺失定义的 K 语义预注册。
