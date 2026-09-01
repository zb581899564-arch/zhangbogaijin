# OBSERVER_API_CONTRACT — V35-FC5-MIDHORIZON-DIAGNOSTICS-V2（总控冻结，供四Agent遵守）

**DIAGNOSTIC_TOOLING_ONLY=true | 本契约定义四Observer的公开签名与接入点，任何Agent不得偏离**

## 0. 构建与Jar策略
- 工作树 `java-jmetal58` 是开发线。接线后构建**独立诊断Jar**：
  `target/jmetal-exec-5.8-jar-with-dependencies.jar` 复制为
  `docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/08-runtime-integration/jmetal-exec-5.8-MIDHORIZON-V2-diag.jar`
  记录其SHA-256。冻结Jar `8DAD8F40...` 不构建、不覆盖、不引用。
- OFF/ON 等价 = 同一新诊断Jar内 telemetry=false vs true 的对照（4条2k / 8条20k 独立JVM）。
- 所有 Observer 必须：OFF 时不构造日志对象、不消费随机数、不改 FE、不改选择/奖励/PDDR/档案/停止规则。

## 1. 统一协调器（Agent A 拥有）
新增 `V35MidHorizonTelemetry`（`org.uma.jmetal.algorithm.multiobjective.mypso.v35`）：
- `setEnabled(boolean)`：false 时清空全部行、observerErrors=0，且不构造任何 CSV/DTO。
- `onPddrRound(...)` / `onTeacherUse(...)` / `onCaTaCandidate(...)` / `onAtomicPhaseEnd(...)`：分别转发给四Observer；任一抛异常 → observerErrors++ 并继续（never throw out）。
- `long getObserverErrors()` / `int checkpointRows() / pddrLedgerRows() / teacherRows() / cataRows()` / `String csvBundle()`（返回全部CSV文本，供写入）。

## 2. 接入点（ZhangBoMOHPSOQ.java 统一接线，Agent A 拥有，仅3处）
- **Checkpoint（原子阶段边界）**：`runFormalHmopsoQgsBaseline` 内 `formalBaselineOuterCycles++`（834行附近）之后、`fullEvaluationCount==beforeCycle` break 之前：
  `midHorizon.onAtomicPhaseEnd(fullEvaluationCount, generationNumber(), formalBaselineOuterCycles, completedRounds, swarm, decisionArchiveSnapshot, passiveFullFront)`。
  - `swarm`=当前workingPopulation；`decisionArchiveSnapshot`=`ZhangBoIncrementalParetoArchive` 当前非支配快照（需深复制为不可变）；`passiveFullFront`=v35PassiveArchive 快照（缺失则传空表，写 NOT_APPLICABLE）。
- **PDDR轮**：`v35Fc5TransferAudit.recordPddrRound(...)`（9548行附近）同一调用点后：
  `midHorizon.onPddrRound(pddrAll, fc5PddrSources, selected, fullEvaluationCount, cycle)`。
- **teacher/cata**：由 Agent C 在 ZhangBoQgController / ZhangBoQpController / V35CaTaLiteController 内部插入 enabled 短路观察调用（不改变任何决策）。

## 3. 方法签名（Java 8 兼容，禁止使用时间戳进行为hash）
```java
// V35CheckpointFrontObserver
void onAtomicPhaseEnd(long actualFE, int generation, int outerCycle, int qRound,
    List<PermutationSolution<Integer>> workingPopulation,   // 深复制或不可变
    List<PermutationSolution<Integer>> decisionArchiveFront,// 可空→NOT_APPLICABLE
    List<PermutationSolution<Integer>> observedFullFront);  // 可空→NOT_APPLICABLE
String toCsv(); long getObserverErrors(); int getRowCount();

// V35FullPddrLedgerObserver
void onPddrRound(List<PermutationSolution<Integer>> pool,
    List<ZhangBoEvaluatedPddrSelector.Source> sources,
    List<ZhangBoEvaluatedPddrSelector.Candidate> selected, long fe, int cycle);
String ledgerCsv(); String cycleSummaryCsv(); long getObserverErrors(); int getRowCount();

// V35TeacherConcentrationObserver
void onTeacherUse(TeacherUseEvent event);   // DTO字段见V2第六节
String eventsCsv(); String concentrationCsv(); long getObserverErrors(); int getRowCount();

// V35CaTaContributionObserver
void onCaTaCandidate(CaTaCandidateEvent event); // DTO字段见V2第七节
String eventsCsv(); String summaryCsv(); long getObserverErrors(); int getRowCount();
```

## 4. 行为等价hash（Agent D 拥有）
`canonicalFrontHash`：精确去重三目标 → 稳定排序（Cmax,TEC,TWC 字典序）→ SHA-256。
`RNG消费序列hash`：由 `JMetalRandom` 消费记录？主循环现有 `getEvaluationTraceHash`/`getQpEventStreamHash` 已覆盖候选/eval/Q表；新增 `getQpTableHash` 等从 RunRecord 复用。Agent D 必须逐项从 RunRecord 提取并比 OFF/ON。
`QpTableHash`：A2 写 `NOT_APPLICABLE`。

## 5. 每个CSV必带列（Agent 各自遵守）
`generatedByRunId, sourceJarSha256, configurationHash, instanceHash, seed, arm, telemetryMode` + 各自字段。

## 6. 20k开销公式（Agent D 拥有）
`overheadPct=(ON_wall-OFF_wall)/OFF_wall*100%`，每臂 ≤15%。
`projected250kBytes=actual20kBytes/actual20kFE*250000*1.25`，单条≤1GB，12条≤15GB。
日志体积必须来自真实20k文件大小，禁止手填。

## 7. 停止
本工作包完成（或任一真实验收失败）即停止，不自动启动250k。
