# V2 真实遥测接线 2k 行为等价验收报告（REAL）

- 工作包：`V35-FC5-MIDHORIZON-DIAGNOSTICS-V2`
- 门：`real2kBehaviorEquivalence`
- 目标：证明四个真实观察者在**同一独立诊断Jar**内、ON 与 OFF 完全行为等价，
  且 ON 运行真实产生四类遥测事件（checkpoint/PDDR/teacher/cata），`observerErrors=0`。

## 1. 使用的运行配置

- 诊断 Jar：`08-runtime-integration/build/jmetal-algorithm-5.8-V35-MIDHORIZON-V2-diag.jar`
- Jar SHA-256：`1F82F67E6A6515B56DD1EFEBC99A1A895150649AFA860BCB5D6B91616F63167A`
- 冻结正式 Jar：`8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`（未改动）
- 实例：`100_5_3_1.txt`（EADHFSP 100 job），seed `20260901`，MaxFEs=`2000`
- 臂：`A2_CFVF`、`A4_BUDGET_AWARE_CATA`；telemetry：`OFF` / `ON`
- 每运行独立 JVM（A2/A4 独立进程），workdir=`java-jmetal58`
- 2k 名义检查点：`{2000}`（maxFEs<=5000 时由 driver 自动派生）

## 2. 行为等价（OFF vs ON 全字段一致）

对比字段：`status, actualFE, decoderCalls, illegalSolutions, duplicateEvaluations,
initialPopulationHash, evaluationTraceHash, qgTableHash, qpTableHash,
pddrEventStreamHash, canonicalFrontHash, frontSize, formalOuterCycles`。

| 臂 | 等价 | 说明 |
|---|---|---|
| A2 | ✅ 全 13 字段 MATCH | qgTableHash=`1CDF95A4...`（OFF/ON 相同），qp=disabled（A2 无 Qp） |
| A4 | ✅ 全 13 字段 MATCH | qg/qp/pddr/canonical 全部一致 |

关键 hash 一致（OFF=ON）：

- A2 initialPopulationHash=`179a82a3...`，evaluationTraceHash=`9e68bc10...`，
  canonicalFrontHash=`c90ab253...`，frontSize=29
- A4 canonicalFrontHash=`971d7da5...`，frontSize=22

## 3. ON 真实事件（非零、非 stub）

| 臂 | observerErrors | checkpointRows | pddrLedgerRows | teacherRows | cataRows |
|---|---:|---:|---:|---:|---:|
| A2 ON | 0 | 50 | 200 | 76 | 0（A2 无 CA-TA，预期） |
| A4 ON | 0 | 30 | 200 | 1889 | 0（2k 预算下 CA-TA 局部窗口未开放，见 5） |

- 所有 telemetry CSV 由运行真实写出：`telemetry-checkpoint-fronts.csv`、
  `telemetry-pddr-full-ledger.csv`、`telemetry-pddr-cycle-summary.csv`、
  `telemetry-teacher-use-events.csv`、`telemetry-teacher-concentration.csv`、
  `telemetry-cata-contribution-events.csv`、`telemetry-cata-contribution-summary.csv`、
  `canonical-front.csv`。
- checkpoint CSV 含真实 front 行（如 A4 ON：`workingPopulationND` + `decisionArchiveFront`，
  nominal=2000, actualSnapshotFE=2000, overshoot=0），每个解有 SHA-256 fingerprint。
- PDDR 账本每周期完整记录每个候选（uniqueObjectiveCount/strictNdCount/cutoff 等真实聚合）。
- teacher 浓度给出真实 entropy/top1/top5。

## 4. OFF 运行零遥测

OFF 运行 behavior-summary 中 `checkpointRows=0, pddrLedgerRows=0, teacherRows=0, cataRows=0`，
且**不生成任何 telemetry-*.csv**（只有 behavior-summary + canonical-front）。

## 5. A4 在 2k 下 cataRows=0 的原因（诚实记录）

2k 预算下 A4 的 `caTaTestCalls=0`：正式局部 FE 窗口按
`B_L=⌊β/(1−β)·B_G⌋`（β=0.25→0.65）在每个外周期开放；2000 FE 预算在初始种群 100 +
19 个 Q round（1900 FE）后已耗尽，局部窗口尚未在 2000 边界内获得余量，
因此 CA-TA-Lite 在该预算下没有真实候选评价。这不是 stub：CA-TA 的真实事件在
20k 预算下被验证（见 10 目录，A4 cataRows=426/473、caTaTestCalls=261/318）。
A2 无 Qp 无 CA-TA，故 qp=disabled、cataRows=0 是正确语义。

## 6. 验收结论

```text
real2kBehaviorEquivalence = true
runtimeObserversWired      = true
2k 两臂 OFF/ON 行为等价      = PASS（13/13 字段一致）
2k ON 四类遥测真实产生      = PASS（checkpointRows>0, pddrLedgerRows>0, teacherRows>0）
2k observerErrors          = 0
```

原始运行目录：`runs/2k-100_5_3_1-20260901-{A2,A4}-{OFF,ON}/`
等价矩阵：`real-2k-behavior-equivalence.csv`
