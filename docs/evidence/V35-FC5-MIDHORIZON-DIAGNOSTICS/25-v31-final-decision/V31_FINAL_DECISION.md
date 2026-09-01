# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1：最终判定

判定日期：2026-08-26

## 最终结论

终止快照协议和最终部署诊断 Jar 均已通过唯一一对 121 runtime A4/50k ON/OFF 复验：

```ini
terminalCheckpointProtocol=PASSED
diagnosticImplementation=FUNCTIONALLY_READY
diagnosticDeploymentArtifact=VALIDATED_121_RUNTIME
diagnosticToolingValidated=true
250kReadyForPreregistration=true
250kStarted=false
formalMatrixRunning=false
FC5=INCONCLUSIVE
```

`250kReadyForPreregistration=true` 只表示工具和部署候选已具备进入 preregistration 准备的
条件，不表示 250k 已获批准、已启动或 FC5 根因已经成立。

## Jar 身份

最终身份拆分如下，不能互相替代：

```ini
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
diagnosticBaseJarSha256=723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F
diagnosticRuntimeJarSha256=121FBB4939258BDC94C297D5F6CE9BE0B0BEE0271A6E71B89BAE8E1486394155
runtimeJarBindingVerified=true
```

121 runtime Jar 已复制到 `../26-final-runtime-jar-validation/diagnostic-runtime-121FBB49.jar`，
正式算法 Jar 的实体副本也已封存。E30 只作为历史运行证据保留，不再作为最终部署依据。

## 终止快照门

| 门 | 结果 |
|---|---|
| `phaseConsistentTerminalSnapshotImplemented` | true |
| `terminalSnapshotIsRealAtomicBoundary` | true |
| `actualFEEqualsLastCompletedAtomicBoundary` | true |
| `terminalWorkingPopulationFrontObserved` | true |
| `terminalDecisionArchiveFrontObserved` | true |
| `terminalObservedFullFrontObserved` | true |
| `unobservableCheckpointCount` | 0 |
| `observerExecutionErrors` | 0 |
| `allowTerminalPartialFormalQPhase` | false |
| `onOffBehaviorEquivalent` | true |
| `formalFrozenJarChanged` | false |
| `algorithmDecisionSemanticsChanged` | false |

终止状态为 `actualFE=48269`、`requestedMaxFE=50000`、`remainingFE=1731`、
`qPhaseFE=5000`；48269 等于最后完整原子边界 FE。ON 侧终止快照为 `ACCEPTED`，三类前沿
共 619 行：working population 72、decision archive 262、observed full front 285。三类
前沿全部非空、有限、目标三元组唯一且相互可区分。

## 121 ON/OFF 复验

ON 与 OFF 使用相同的 121 runtime Jar、相同的 A4/`100_5_3_1`/seed `20260901`/50k 配置，
各自独立 JVM，均退出码 0。FE、初始种群、评价轨迹、Qg/Qp 表、PDDR 事件和最终前沿哈希
完全一致；专属 telemetry RNG/candidate digest 只在 ON 侧适用，已在等价表中标明
`NOT_APPLICABLE_OFF`。详见
`../26-final-runtime-jar-validation/A4_50K_121_ON_OFF_EQUIVALENCE.csv`。

两侧原始运行输出已经直接使用统一 CA-TA 字段：

```ini
cataLifecycleSchemaValidated=true
cataLongRunLifecycleValidated=true
cataAllShortGateSourceCoverageValidated=false
cataFullLifecycleValidated=false
```

因此最终判定不依赖事后归一化修正。完整字段见
`V31_NORMALIZED_ACCEPTANCE.properties`，最终运行直接字段见
`../26-final-runtime-jar-validation/FINAL_121_RUNTIME_ACCEPTANCE.properties`。

## 测试与范围边界

限定的 `V35Fc5MidHorizonDiagnosticsV31ContractTest` 与
`V35MidHorizonObserverRealTest` 共 20 个测试全部 PASS，0 failures，0 errors。测试日志及
逐方法结果见 `../26-final-runtime-jar-validation/TEST_121_RUNTIME_*`。

这是最后一次 50k 复验。本轮没有启动 2k、20k、A2、250k、formal matrix，也没有上传；旧的
14–20 目录及此前 V3.1 证据均保留，不被覆盖。最终 Manifest 包含 121 runtime 实体、正式
算法实体、两侧运行产物、测试日志和全部历史/新增证据，并已做反向哈希复核。
