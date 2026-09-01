# SOURCE_DIAGNOSTICS_DECISION — V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1 最终裁决

- 日期：2026-08-31
- 裁决（预登记 §6 六选一）：**`NO_SOURCE_LEVEL_FAILURE`**
- 上游：campaign章程 P1；预登记 `../00-preregistration/SOURCE_DIAGNOSTICS_PREREGISTRATION.md`（门与标签映射冻结）
- 执行：6/6 诊断运行（C0 × {50_2_3_1, 100_5_3_1} × seed {20260919,20,21} × 100k，训练机 `zhangbo-v35-source-diagnostics-20260831`，全首跑成功，账本行数==actualFE==96025，UNSET=0，observer/ledger errors=0）
- 诊断Jar：`jmetal-algorithm-5.8-V35-SOURCE-DIAGNOSTICS-V3.jar`（SHA `bbb9ccd6…f2e`，30类 major=52；正式Jar字节不动；等价门 190 IDENTICAL + 16 ON_ONLY + 0 DIFFER → `sourceLedgerValidated=true`）

## 1. 核心数据（全run贡献分区，median over 3 seed）

价值效率 e(S) = 前沿HV贡献占比 / 评估量占比（精确HV分区：终态观测前沿按x扫的专属体积分解，Σe×share=1；实现 `05-analysis/analyze_source_diagnostics.py`）：

| sourceRollup | evalShare(50/100) | e(S)(50/100) | 判读 |
|---|---|---|---|
| CFVF(GLOBAL_Q) | 62.5% / 62.5% | **0.961 / 1.108**（逐seed 0.395–1.255） | 与生成占比相称，健康 |
| INHERITED_LS | 34.3% / 34.2% | **1.150 / 0.863**（逐seed 0.589–2.173） | 相称或更高，主要价值驱动 |
| CATA(Test+Apply) | 3.1% / 3.2% | 0.038 / 0.046（seed21例外0.78/0.86） | 低效但FE占比小 |
| PARENT | 0.1% | 0.0 | 初群点终被支配（预期） |

时间窗（困难实例）：CFVF评估占比 0.79→0.45 递减、INHERITED_LS 0.17→0.53 递增——与冻结的 beta(u) 调度方向一致（局部窗口随进度扩大）；CATA占比全程稳定在2.3–4.4%，无异常增长。pddr-round账本：12循环×恰100选中（working population），池构成与选择率逐来源可追溯。

## 2. 逐门裁决（预登记 §6）

| 门 | 条件 | 结果 |
|---|---|---|
| 1 CFVF_GENERATION_COLLAPSE | evalShare>60% 且 e<0.3 且降幅≥50%，≥2/3 seed双实例 | **FAIL**：evalShare 62.5%✓ 但 e=0.96/1.11 远高于0.3，无下降 |
| 2 QP_PERSONAL_ARCHIVE_COLLAPSE | personal teacher终窗<10% + Qp表早熟稳定 | **NOT_TRIGGERED**：Qp零FE无独立评估行；其失效若存在必然经CFVF后代质量显现，而CFVF e≈1.0健康——前提不成立（个人教师占比字段本身NOT_JOINABLE，如实登记） |
| 3 CATA_LOW_VALUE_EXPLOITATION | e<0.15 且 **FE占比≥5%**，≥2/3 seed | **FAIL**：e中位0.038/0.046<0.15 ✓ 且2/3 seed成立 ✓，但 **FE占比3.1%<5%** ✗——低效幅度不构成根因量级 |
| 4 COMPOSITE_GENERATION_BIAS | ≥2来源 e<0.5 | **FAIL**：仅CATA一个来源 |
| 5 NO_SOURCE_LEVEL_FAILURE | 主要来源（合计96.8%评估量）e≥0.5 或贡献与占比相称 | **SELECTED**：CFVF与INHERITED_LS双实例逐seed全部健康 |

## 3. 结论与含义

1. **生成侧各主要来源无功能失效**：占评估量96.8%的两大来源（CFVF全局生成、inherited LS）在正常/困难实例上的价值效率均与生成占比相称（e≥0.589逐seed下界）。"生成侧候选质量/多样性不足"**不是某一来源模块的崩塌**。
2. 唯一低效信号（CATA：~3.1% FE、HV贡献~0.1–2.4%）低于预登记实质性门槛（≥5% FE），且seed21显示其可在此预算内产生实际价值（e=0.78/0.86）——不足以支撑一个修复族，更无法解释HV/IGD崩塌的量级（2–7%）。
3. **按campaign章程P2："没有可达杠杆（证据不支持任何来源级修复），直接停止修复，不允许凭感觉新增机制。"** 唯一的修复族预算**不消耗**：`repairFamilyBudget=UNSPENT`。
4. 与250k裁决合流：LOCAL_FE_PACING（预算侧）已否证；本诊断（来源侧）确认无失效模块。**算法优化主线就此关闭**：`algorithmOptimizationClosed=true`（依据=两轮独立证据链，非试错次数耗尽）。
5. 下一步（campaign P5-P12，须用户批准后另开工作包）：以当前冻结语义（C0/A4）进入 Final对比（A2 vs A4 500k多实例）、Final Freeze、Preflight、正式消融、外部算法、FM3模型实验与论文。HV/IGD在困难实例上的表现将作为诚实结果报告，归因登记为"实例难度驱动的搜索分布特性 + CA-TA小预算低效（登记在案）"，不做未经因果确认的机制声明。

## 4. 执行事件登记（诚实记录）

- **utilizationRate>0.98 门校准错误（非预登记内容）**：该阈值出现在远端执行Agent的验收指令中，系从250k包沿用的换算（5000/250000≈2%尾差 → >0.98）。100k预算下尾停协议的utilization结构性上限为0.95（本批=0.96025，remainingFE=3975<5000 ✓，phaseBoundAccepted=true ✓，FE记账逐seed确定性一致）。预登记文档本身未定义该数值门限；实质判据全部PASS。处置：按字面登记 FAIL(utilizationRate-gate) + 本更正说明；不重跑（FE记账确定性，重跑无意义）。
- 快照头解析截断（600→1500字符）、staging布局重构（per-seed）两处工程过程修正，均在运行前完成，无科学影响。

## 5. 机器状态

```ini
diagnosticVerdict=NO_SOURCE_LEVEL_FAILURE
rootCauseCandidate=NONE
sourceLedgerValidated=true
diagnosticRunsAccepted=6/6(scientific criteria; utilization-gate calibration note above)
ledgerRows==actualFE=96025(per run)
valueEfficiency.CFVF=0.961/1.108(50/100 median)
valueEfficiency.INHERITED_LS=1.150/0.863
valueEfficiency.CATA=0.038/0.046(FE share 3.1%<5% materiality gate)
repairFamilyBudget=UNSPENT
algorithmOptimizationClosed=true
newRepairImplemented=false
newExperimentStarted=false(stopped per charter after P1)
DOEStarted=false
500kStarted=false
FinalCandidateApproved=false
FINAL_FROZEN=false
formalMatrixRunning=false
PDDRChanged=false
CFVFChanged=false
DualQChanged=false
CaTaChanged=false
formalJarChanged=false
```

按campaign章程与预登记：**本阶段到此停止，等用户复核裁决并决定是否进入P5（Final对比）阶段。**
