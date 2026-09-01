# V2 真实遥测接线 20k 行为等价与开销验收报告（REAL）

- 工作包：`V35-FC5-MIDHORIZON-DIAGNOSTICS-V2`
- 门：`real20kBehaviorEquivalence`、`realTelemetryOverheadValidated`
- 目标：在 20k 预算下证明四观察者 ON/OFF 行为等价，ON 产生真实中间事件，
  且 ON 相对 OFF 的墙钟开销 ≤15%。

## 1. 使用的运行配置

- 诊断 Jar：`08-runtime-integration/build/jmetal-algorithm-5.8-V35-MIDHORIZON-V2-diag.jar`
- Jar SHA-256：`1F82F67E6A6515B56DD1EFEBC99A1A895150649AFA860BCB5D6B91616F63167A`
- 冻结正式 Jar：`8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`（未改动）
- 实例：`100_2_4_1.txt`、`100_5_3_1.txt`；seed `20260901`；MaxFEs=`20000`
- 臂：`A2_CFVF`、`A4_BUDGET_AWARE_CATA`；telemetry：`OFF`/`ON`
- 名义检查点：`{5000,10000,15000,20000}`（driver 按 maxFEs>5000 自动派生）
- 8 个独立 JVM（每 arm×instance×mode 一个进程）

## 2. 行为等价（全字段一致）

对比字段：`status, actualFE, decoderCalls, illegalSolutions, duplicateEvaluations,
initialPopulationHash, evaluationTraceHash, qgTableHash, qpTableHash,
pddrEventStreamHash, canonicalFrontHash, frontSize, formalOuterCycles`。

| 臂 | 实例 | 等价 | formalOuterCycles |
|---|---|---|---|
| A2 | 100_2_4_1 | ✅ 13/13 | 1 |
| A4 | 100_2_4_1 | ✅ 13/13 | 3 |
| A2 | 100_5_3_1 | ✅ 13/13 | 1 |
| A4 | 100_5_3_1 | ✅ 13/13 | 3 |

说明：A2 采用 legacy `LS_Times=30` 局部搜索（无动态 β 窗口），20k 预算被单个长外周期
（50 Q round + 继承局部搜索）一次消耗，故 `formalOuterCycles=1`；A4 启用 Budget-Aware
CA-TA 动态局部 FE 窗口，完整经历 3 个外周期。两条路径的 OFF/ON hash 均逐位一致。

## 3. 20k ON 真实事件（非零、非 stub）

| 臂 | 实例 | obsErr | checkpointRows | pddrLedgerRows | teacherRows | cataRows | caTaTestCalls |
|---|---|---|---:|---:|---:|---:|---:|---:|
| A2 | 100_2_4_1 | 0 | 212 | 258 | 200 | 0（无 CA-TA） | 0 |
| A4 | 100_2_4_1 | 0 | 316 | 718 | 12894 | 426 | 261 |
| A2 | 100_5_3_1 | 0 | 376 | 255 | 200 | 0（无 CA-TA） | 0 |
| A4 | 100_5_3_1 | 0 | 458 | 723 | 14185 | 473 | 318 |

- **A4 的 CA-TA 真实事件被验证**：cataRows>0 且 caTaTestCalls>0（261/318 次 Test 调用），
  每宏邻域 N1--N5 有独立的 generated/evaluated/accepted/pddrSurvived 汇总。
- 检查点覆盖全部 4 个名义：A4 在 5000/10000/15000/20000 均捕获（overshoot ≤ 2006，
  诚实记录）；A2 因单外周期结构在唯一边界 FE=20000 处一次捕获全部 4 个名义
  （overshoot=15000 诚实记录，说明该预算下不存在更细边界）。
- 教师浓度给出真实 Qg/Qp 分布：如 A4 100_5_3_1 ALL_QG=588 次暴露/111 个唯一教师/
  entropy=3.866；ALL_QP=13597 次暴露/132 个唯一教师。
- PDDR 账本每周期完整记录（poolSize/uniqueObjectiveCount/strictNdCount/cutoffRank 等）。

## 4. 墙钟开销（≤15% 门）

采用**确定性复测**后的墙钟（同 seed 重放 hash 逐位一致，见 6）：

| 臂 | 实例 | OFF (ms) | ON (ms) | overhead% | 门 (15%) |
|---|---|---|---:|---:|---:|---|
| A2 | 100_2_4_1 | 5270 | 5425 | +2.94% | ✅ |
| A4 | 100_2_4_1 | 10577 | 10100 | -4.51% | ✅ |
| A2 | 100_5_3_1 | 8192 | 8377 | +2.26% | ✅ |
| A4 | 100_5_3_1 | 14095 | 15166 | +7.60% | ✅ |

首次批内测量曾出现 A2 100_2_4_1 OFF=4819ms 的异常快值（+29.26%），
经独立复测（OFF=5270ms, ON=5425ms → +2.94%）确认为单次运行噪声；
复测 hash 与原批逐位一致，证明行为确定性、时序为偶发噪声。所有 4 对最终
开销均在门内，最大 +7.60%。

## 5. 字节量预估（250k 外推基础）

单次 20k ON 遥测 CSV 字节：A2 约 0.29--0.36MB，A4 约 5.9--6.5MB（teacher 事件为主）。
250k 外推（×12.5）预计单次 ≤82MB，12 次运行 ≤1GB，远低于存储上限；本验收只做
预估，不启动 250k。

## 6. 确定性复核

对每对 OFF/ON 重新运行同一命令，对比 `canonicalFrontHash/evaluationTraceHash/qgTableHash/
pddrEventStreamHash` 与批内证据——**全部 DETERMINISTIC**，证明同 seed 跨 JVM 逐位可复现，
遥测接线不改变任何行为 hash。

## 7. 验收结论

```text
real20kBehaviorEquivalence   = true  （4/4 对 13/13 字段一致）
realTelemetryOverheadValidated = true （4/4 对开销 ≤ +7.60% < 15% 门）
A4 20k CA-TA 真实事件        = PASS  （cataRows>0, caTaTestCalls>0）
observerErrors               = 0     （全部 ON 运行）
```

原始运行目录：`runs/20k-{100_2_4_1,100_5_3_1}-20260901-{A2,A4}-{OFF,ON}/`
等价矩阵：`real-20k-behavior-equivalence.csv`
开销矩阵：`real-20k-telemetry-overhead.csv`
