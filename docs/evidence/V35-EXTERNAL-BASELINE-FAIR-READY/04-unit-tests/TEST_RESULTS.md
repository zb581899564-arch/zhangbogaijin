# 外部基线单元测试结果

- 日期：2026-08-30
- 命令：`mvn -pl jmetal-algorithm -am test -Dtest="V35P25EFaithfulEnginesTest,V35ExternalBaseline*" -Djacoco.skip=true`
- 结论：**新增 14/14 PASS（回归 2/2 PASS）**；jmetal-problem 全模块 67/67 PASS

## V35ExternalBaselineRepresentationTest（4/4）

| 测试 | 验证门 |
|---|---|
| crossoverDeepCopiesAndKeepsParentsUnchanged | 交叉深复制：父本基因型指纹前后不变；子代 JS 为合法排列；FA/MA/WA 全合法（工厂界内、机器数界内、工人可任职） |
| mutationKeepsRepresentationLegal | 50 次变异后表示恒合法 |
| operatorsAreBlindToObjectives | 目标盲：同 seed 双算子实例，父本目标值交换后子代基因型逐位一致（交叉）；低/高目标解变异结果逐位一致（变异） |
| budgetRejectsDuplicateEvaluationOfUnchangedCandidate | 同一未变候选二次评价被拒绝且 duplicateEvaluations+1 |

## V35ExternalBaselineIdentityTest（5/5）

| 测试 | 验证门 |
|---|---|
| nsga2RunsOfficialMachineryWithPositiveIdentityEvents | crossover/mutation/tournament 计数全部 >0（950/1900/1900@2k）；FE 精确闭合；身份证据含 binaryTournament=true;ranking=true;crowdingDistance=true;replacement=RankingAndCrowdingSelection；目标全部有限 |
| spea2RunsOfficialMachineryWithPositiveIdentityEvents | 计数全部 >0（1900/1900/3800@2k）；身份证据含 strengthRawFitness=true;archive=true;environmentalSelection=SPEA2 |
| nsga2EnvironmentalSelectionAppliesRankingAndCrowding | 直接执行官方 RankingAndCrowdingSelection(100, DominanceComparator)：40→100 截断；属性注解（rank+crowding）真实写入 |
| spea2EnvironmentalSelectionAppliesStrengthDensityAndTruncation | 直接执行官方 StrengthRawFitness.computeDensityEstimator（每解均有 strength/raw fitness/density 属性）+ EnvironmentalSelection(100)：截断生效 |
| engineRejectsSevenSlotView | 引擎对 AUTHOR_SEVEN_SLOT 抛 IllegalArgumentException（THREE_OBJECTIVE 强制门） |

## V35ExternalBaselineFairnessTest（3/3）

| 测试 | 验证门 |
|---|---|
| bothArmsStartFromByteIdenticalInitialPopulations | 同 instance/seed/population 下双臂 drain 出的初群四向量哈希与冻结种群一致（纯四向量 SHA-256，独立于作者视图 machine 属性） |
| sharedLayerIsFixedForBothArms | FM3；Shift=NONE；THREE_OBJECTIVE 视图；3 目标 0 约束；评价后目标有限 |
| bothArmsCloseExactBudgetWithoutForbiddenMechanisms | 双算法 FE=2000=canonical counter；duplicateEvaluations=0；representationRepairs=0；身份证据不含 CFVF/PDDR/CA-TA/DSCR/Qg/Qp；sourceKind=OFFICIAL_JMETAL_CORE |

## 静态扫描（12.1，工具 `tools/run_forbidden_reference_scan.py`）

7 个目标文件 × 13 类检查 = 91 项：禁止模块引用（P25D 引擎/BaselineUpdater/CFVF/DSCR/
CA-TA/PDDR/教师池/inheritedLS/O1-O13/dualQ/个人档案）全 0；目标槽 2–5 读取 0；
非合法写槽 0；非 NONE Shift 0。**overall=PASS**（明细
`02-adapter-audit/forbidden-reference-scan.csv`；注释/字符串先行剥离，
文档性枚举不计为引用）。

## 重放测试（12.5）

见 `05-2k-smoke/SMOKE_REPORT.md`：双算法各 2 次独立 JVM 同 seed 运行，
canonical front hash 与 FE 全部一致。
