# TELEMETRY_CONTRACT — V35-FC5-MIDHORIZON-DIAGNOSTICS-V1

**DIAGNOSTIC_TOOLING_ONLY=true | algorithmChanged=false | searchSemanticsChanged=false**
**冻结日期 2026-08-26 | 总控**

## 1. 原子阶段与FE

- `qPhaseFE = population * Q_Times = 100*50 = 5000` 为最小原子完整评价单位。所有正式外循环、Q轮、评价后PDDR、局部搜索均以此为边界，`remainingFE<5000` 即阶段一致尾停，无补评价。
- FE递增仅由 `ZhangBoMOHPSOQ.evaluateSwarm` → `ZhangBoFatigueEvaluationResult` 完整解码产生，每次完整评价 `decoderCalls++` 且 `fullEvaluations++`，`decoderCalls==fullEvaluations` 必须成立。

## 2. Checkpoint捕获时机

```
nominalCheckpointFE ∈ {1k,2k} for 2k; {5k,10k,15k,20k} for 20k; {25k..250k step 25k} for 250k
capture当且仅当 原子阶段结束 且 actualFE首次>=nominalCheckpointFE
overshootFE = actualSnapshotFE - nominalCheckpointFE, 0<=overshootFE<5000
```

记录 `nominalCheckpointFE, actualSnapshotFE, overshootFE, generation, formalOuterCycle, qRound`。

配对比较容差：`abs(actualSnapshotFE_A2 - actualSnapshotFE_A4)<5000` 否则 `CHECKPOINT_FE_MISMATCH` 不得插值。

## 3. 三类front定义

- `workingPopulationND`：当轮 `workingPopulation` 经三目标[0,1,6]精确去重+严格Pareto过滤+稳定排序后的非支配集（PDDR输入侧）
- `decisionArchiveFront`：`ZhangBoIncrementalParetoArchive` 的当前非支配集（决策档案）
- `observedFullFront`：`V35PassiveEvaluationArchive` 增量旁路（只读，不参与搜索，缺失写 NOT_APPLICABLE）
- 三类禁止混用，禁止用最终front倒推历史。

## 4. 数据质量门

- 主键：`instance|seed|arm|actualSnapshotFE|frontType|solutionFingerprint`
- 去重：目标三元组精确去重，严格支配
- 哈希：`checkpoint-fronts.csv` 按 `frontType` 分组稳定排序后三目标序列哈希

## 5. 开销与故障

- `telemetry wall-clock overhead <=15%`，单条250k未压缩<=1GB，12条<=15GB，否则仅优化日志实现（流式、分文件、fingerprint缓存），不得删字段。
- `observerErrors=0` 必须。

## 6. 冻结语义

FM3/ShiftMode=NONE/single family/sequence-independent/mixture[20,40,20,20]/GLOBAL_ORIGINAL/LS order CA-TA→inherited / teacherPool OFF 保持不变。
