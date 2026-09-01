# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1：终止快照契约

状态：已收口，适用于本轮唯一的 A4 / `100_5_3_1` / `20260901` / `50000` 验收。

## 合法终止

终止分类器只接受以下条件同时成立的状态：

```text
0 < actualFE <= requestedMaxFE
remainingFE = requestedMaxFE - actualFE
0 <= remainingFE < qPhaseFE
allowTerminalPartialFormalQPhase = false
terminationKind = PHASE_CONSISTENT_BUDGET_TERMINATION
actualFE = lastCompletedAtomicBoundaryFE
```

本轮 `qPhaseFE=5000`。因此 `actualFE=48269`、`remainingFE=1731` 是合法的
phase-consistent 终止；它不是伪造的 `50000` FE 快照，也不是 partial Q phase。

## 终止快照

只有真实完整原子边界才能提交一次性三前沿快照：

```text
workingPopulationND
decisionArchiveFront
observedFullFront
```

三个前沿必须来自同一 `actualFE` 状态，分别非空、目标有限、可序列化且三者可区分。
观察器只复制并整理数据，不写回算法状态，不消耗 FE/随机数，不改变 Q 表、PDDR、档案或决策。
重复回调不能替换已经保存的终止状态。

phase-consistent 终止行使用：

```text
checkpointKind=PHASE_CONSISTENT_TERMINAL
nominalCheckpointFE=50000
actualCheckpointFE=48269
checkpointDeltaFE=-1731
atomicBoundary=REAL_ATOMIC_RUN_END_SNAPSHOT
terminationKind=PHASE_CONSISTENT_BUDGET_TERMINATION
```

名义检查点未精确到达只增加 `nominalCheckpointNotExactlyReachedCount=1`，不增加
`unobservableCheckpointCount`、`observerErrors` 或 `observerExecutionErrors`。无法证明真实
原子边界、三前沿缺失/为空/非有限/不可区分、预算或终止类型不合法时，必须 fail-closed。

## 生产接缝与测试

`V35TerminalCheckpointContract.classify(...)` 是纯、确定性的 `ACCEPTED/REJECTED` 分类器。
`V35CheckpointFrontObserver` 保存最后完整原子边界，并通过
`V35MidHorizonTelemetry.onTerminalRunEnd(...)` 接收算法结束时的真实状态。算法的终止钩子只
在 telemetry 开启时读取状态。

V3.1 合同测试覆盖 exact `50000`、合法 `48269`、`remainingFE>=5000`、超预算、FE/原子边界
不一致、三前沿缺失/为空/非有限、partial phase、观察器异常、OFF 短路及重复回调。测试结果见
`../22-terminal-checkpoint-implementation/test-results.csv`。

本契约不等同于 FC5 根因成立，不授权 250k 或 formal matrix；它只定义诊断工具对合法终止
快照的可接受边界。
