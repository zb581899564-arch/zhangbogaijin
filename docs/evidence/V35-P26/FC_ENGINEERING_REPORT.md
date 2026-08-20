# V35-FC-0..FC-5 工程实施报告（不跑实验版）

- 日期：2026-08-17
- 依据：D-082（v3.5-Final Candidate）、`docs/V35_P26_FINAL_CANDIDATE_PLAN.md`
- 范围声明：训练机已关机，本报告只覆盖**代码实施、单元/集成测试与工程验收**；FC-2 的 4 规模 screening、FC-4 的 ρ 校准、FC-5 的审计运行、FC-6..FC-9 全部待服务器恢复后按方案执行。`formal_matrix_started=false` 不变。

## 1. FC-0 A4-PREFINAL 存档 — completed

- 新测试 `V35Fc0PrefinalArchiveTest`：物化 `docs/evidence/V35-P26/00_prefinal-archive/`（FREEZE_MANIFEST.txt、source-sha256.csv、environment.txt、FC0_REPORT.md）。
- 存档语义 = 正式 A4：DSCR+CFVF+Qp+Qg+CA-TA-Lite、教师池关、BAL 全开放、dualQ blockFrozen(0.10,5,5)、Table-9 LS=30；显式登记 `localFeBudgetScheduler=absent`、`softFreezeRho=absent`、`gBlockLength=5（gb10/15/20 永久关闭）`。
- 重放门：`20_2_3_1`/seed 20260808/20000 FE × 3 次连续重放，front SHA-256 三次逐位一致。
- 幂等契约：manifest body 与磁盘比对，漂移即失败；source 清单**只写一次**（后续 FC 包的源码漂移用与该基线的 diff 隔离，不被覆盖）。
- formalConfigurationHash=`116393b4e074c1918e1f0983adf32c9312ba439e9a8f99a7436ebf30d79b6e76`（FC-2 测试以硬编码钉住：null 预算下该哈希不变）。

## 2. FC-1 FM3 一致关键结构 — completed（工程部分）

- `ZhangBoCriticalDagAnalyzer` 及 `Analysis`/字段公开（原 package-private），供 v35 线复用，语义零变化。
- `V35MacroCandidateGateway`：
  - 新 `prepareWithEvaluation(action, parent, instance, factory, bottleneck, evaluation)` 重载；`Prepared` 新增 `structureSource`（`FM3_ACTUAL`/`PT0_PROXY`）。
  - N3 FM3 路由：SET 瓶颈按第一阶段 `actualSetupDuration` 选 setup-edge 作业；SEQ/BAL 用 `ZhangBoCriticalDagAnalyzer`（容差 1e-9，与 O10 一致）在父解真实轨迹上取零松弛关键集中第一阶段 `actualDuration` 最大的作业。
  - N4 FM3 路由：WOR=actualDuration+fatigueAtStart、FAT=fatigueAfter、MAC=actualDuration、SET=actualSetupDuration、默认=actual+setup。
  - 基因型编辑与旧语义逐字一致（`reassignFrom` 抽取共享）；**只读轨迹，不写 start time，零 Shift**；trace 不可用时逐字节回退 PT0 proxy。
- `ZhangBoMOHPSOQ.runV35CaTaLiteLocalSearch` 主路径传入父解 `ZhangBoFatigueEvaluationResult`；shadow 审计路径保持 PT0 proxy（维持反事实可比性，FC-1 行为差异完全归因主路径）。
- 测试 `V35Fc1Fm3CriticalStructureTest` 5 项 + 既有 Gateway 回归 8 项全绿：null 回退逐字节一致、FM3 选疲劳膨胀关键作业、FAT 路由命中最高疲劳位置、空工厂 trace 回退、N1/N2/N5 不受影响。

## 3. FC-2 Dynamic Local-FE Pacing — completed（工程部分，实验待跑）

- 新类 `V35LocalFeBudgetConfiguration`：`beta(u)=betaMin+(betaMax-betaMin)·u²`、`B_L=floor(β/(1-β)·B_G)`、范围校验、canonicalText。
- `V35ProductionConfiguration` 可选 `localFeBudget`（默认 null=旧 LS_Times 语义；null 时 canonicalText/哈希不变，非 null 时进入 canonical 记录）；`ZhangBoGlobalSearchConfiguration.forV35` 传递。
- `ZhangBoMOHPSOQ`：外层循环 Q 阶段后 `beginLocalFeBudgetWindow(B_G)` 开窗；inter-factory swap/insert、O1–O9 pass 循环与 CA-TA-Lite 的全部预算检查改用 `localFeHardLimit()`（null 预算时恒等于 maxIterations，行为逐字不变）；inter-factory LS 与 CA-TA 共享同一硬预算。
- 测试 `V35Fc2LocalFePacingTest` 5 项全绿：β 数值/校验、**PREFINAL 哈希稳定钉子**、20000 FE 集成对照（legacy=null：local 占比 >0.6；pacing=0.25/0.65：占比落入 [0.2,0.7] 且外层循环 ≥2）。
- 待实验（服务器恢复后）：4 规模 × 50k × 3 paired seed screening → 500k×3；100-job 否决线 HV −5%/IGD +10%。

## 4. FC-3 Cheap-Test CA-TA — completed（工程部分）

- 现状确认：Test 轮已是每宏邻域 1 候选各 1 FE（nTest=1）；Apply 胜者已持续（成功不回 N1）。
- `V35CaTaLiteConfiguration` 新增 `top2ProbeEnabled`（默认 false）与 `testFeShareCap`（默认 1.0=不限制，保持存档行为）；新工厂 `cheapTest()`=（probe 开，cap 0.20）。
- `V35CaTaLiteController`：
  - **Top-2 加探**：Test 完成且 mask 前 two 在主信用键（成功数）打平时，返回 `TOP2_PROBE` 决策（actions=[top1,top2]，各 1 FE），每 epoch 至多一次（`probedThisEpoch`，beginTestEpoch 重置）。
  - **Test 份额硬门**：State 累计 testEvaluations/applyEvaluations；`FE_Test > cap×(FE_Test+FE_local)` 时两个 Re-test 触发点（连续失败/配额耗尽）被抑制，返回 `RETEST_SUPPRESSED_TEST_SHARE_CAP` 的当前 winner Apply。
- 测试 `V35Fc3CheapTestTest` 4 项 + 既有 Controller 回归 3 项全绿（standard 永不 probe/抑制、tie 触发单次 probe、cap 抑制 Re-test、配置校验）。

## 5. FC-4 贡献门控软冻结双 Q — completed（工程部分，校准待跑）

- `ZhangBoDualQCoordinationConfiguration` 新增 `softFreezeRho`（默认 0.0=硬冻结；ρ=0 时 toCanonicalText 不变，存档字节稳定）；新工厂 `blockFrozenSoftFreeze(warmup, p, g, rho)`。
- `ZhangBoQgController.settleWithScaledAlpha`：与 settle 相同的 TD 目标、学习率 α×scale；新增 `softTdUpdateCount`。
- `ZhangBoQpController`：`SettlementMode.SOFT_LEARN`；settle 新重载带 `alphaScale` 与 `contributingBranchIds`——只有实际执行过 teacher-derived CFVF 动作的 branch 记 TD（其余保持 frozen observation）；batchUpdate 支持有效学习率 α×scale。
- `ZhangBoMOHPSOQ`：
  - I_contrib 数据源 = 现有 `ZhangBoCfvfDiagnostics.gbestInherited/pbestInherited`（GBEST 与 BOTH 均计入）：P-block 的 Qg 按群门控（该群 ≥1 个后代执行过 gbest-derived 动作）；G-block 的 Qp 按谱系 branch 门控。
  - P6.4 冻结哈希不变式守卫在 ρ>0 时对软侧放宽（ρ=0 严格模式原样保留——硬冻结行为有守卫背书）。
- 辅助控制器冻结期间本就纯 greedy（P6.4 语义），方案要求已满足，无需改动。
- 测试 `V35Fc4SoftFreezeTest` 3 项全绿：ρ=0 canonical 字节一致+校验、缩放 TD 数学（0.3×硬更新值）、20000 FE 集成（ρ=0.3 的冻结侧 Qg TD 严格多于硬冻结，双臂 FE 闭合）。
- 待实验：FC-2 稳定后单变量 `A4+Pacing` vs `A4+Pacing+SoftFreeze`，先 20/100 后 10/50。

## 6. FC-5 GIR 审计 + RecordContribution — completed（工程部分，审计运行待跑）

- `ZhangBoCfvfDiagnostics` 新增 Kind×Source 交叉计数 `crossCounts`（updater 在应用动作处同步累计，输出进 canonicalText `cross.KIND:SOURCE=n`）。
- 新观察器 `V35CfvfGirAudit`（v35 包）：按 (group × 向量 × 教师来源) 聚合 GIR——FMW→FA/MA/WA、MW→MA/WA、M→MA、W→WA；JS 维度经聚合继承计数如实登记（粒度限制写进类注释）；维护 per-branch 最近修改指纹（容量 4096 的 LRU）供 RecordContribution 按 branch/FE 对齐；`summaryText()` 输出规范汇总。
- `ZhangBoMOHPSOQ` 常挂观察（`updateCfvfGroup` 处 observe，lineage/FE/代号为键）；`v35CfvfGirAuditSummary()`/`getV35CfvfGirAudit()` 供 Runner 与报告输出。
- 测试 `V35Fc5CfvfGirAuditTest` 2 项全绿：交叉计数→GIR 展开/JS 限制/fingerprint；20000 FE 真实运行重放 front 逐位一致且 `observations == cfvfOffspringCount`（纯观察证明）。
- 待运行：20k/100k 审计母表 + 与 cmaxAudit 曲线对齐的 RecordContribution 分析（FC-6 分支选择的证据输入）。

## 7. 行为影响总结

| 配置 | 行为 |
|---|---|
| 全默认（A4-PREFINAL 语义） | 与存档一致：无 localFeBudget、ρ=0、standard CA-TA、PT0 proxy 仅在 evaluation 缺失时（正式主路径已传 FM3 trace——**这是唯一默认路径行为变化**：N3/N4 候选生成读真实关键结构，见 FC-1） |
| localFeBudget 非 null | FC-2 预算调度生效 |
| dualQ.softFreezeRho>0 | FC-4 软冻结生效 |
| cheapTest() CA-TA 配置 | FC-3 probe+cap 生效 |

默认配置的配置哈希不变（FC-2 钉子验证）；行为层唯一有意变化是 FC-1（主路径 N3/N4 用 FM3 真实关键结构），该变化即方案 FC-1 的目的，且有 null 回退门。

## 8. 回归

jmetal-algorithm 全量回归（`--add-opens` 命令）：结果见本目录 `REGRESSION.md`（全量运行在收尾时执行并登记，既有失败分类不变：V35P101/D-076 快照不兼容、NSGAIIIT 上游、Mockito JDK17）。

## 9. 下一步（服务器恢复后，须用户批准）

1. FC-1 语义审计运行（I1+20_2_3_1，N3 候选命中关键结构比例）
2. FC-2 screening：10/20/50/100 × 50k × 3 paired seed → 有效则 500k×3（100-job 否决线）
3. FC-3 A/B：20_2_3_1 50k×3（Test FE 降幅 + 性能不退）
4. FC-5 审计母表 + FC-6 分支选择（四选一）
5. FC-4 校准（FC-2 稳定后）：ρ∈{0,0.1,0.2,0.3} 先 20/100
