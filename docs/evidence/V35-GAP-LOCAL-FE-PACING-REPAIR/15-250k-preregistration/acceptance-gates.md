# acceptance-gates.md — 250k 验收门（冻结，任务书 §六/§七）

## 逐条门（全部满足才 ACCEPTED）

```
0 < actualFE = decoderCalls <= 250000        budget-termination.properties
remainingFE < 5000                           同上
utilizationRate > 0.98                       同上（>0.98 由 remainingFE<5000 蕴含，双验）
phaseBoundAccepted = true                    同上
front finite and nonempty                    front.csv 非空、全有限、无重复点
illegalSolutions = 0                         status.properties
duplicateEvaluations = 0                     status.properties
unexplainedRepairs = 0                       mechanismSummary: cfvfRepairs=0,
                                             directionalPoolRequests=0, directionalPoolFiltered=0,
                                             shadowSamples=0, shadowEvaluations=0
sourceLoss = 0                               passiveObservedCount == fullEvaluations
shiftActivity = 0                            decoder 左右移位计数与纳秒全零
checkpointRows = 4 且 overshootFE 全 0       checkpoints/checkpoint-registry.csv
observerExecutionErrors = 0                  formal-gate.properties
failures = NONE                              formal-gate.properties
betaMax 运行时读回 = profile 值             profile.txt
```

## 公平组门（每 instance×seed 三臂）

```
same snapshotSha256 / initialPopulationHashV35 / initialPopulationHashP8 / instance+SUT+fatigue provenance
max(actualFE) - min(actualFE) < 5000
每检查点组内 checkpointObservedFE 跨度 < 5000（本设计恒为 0）
```

## 等价前提（已完成）

- 20k 门（50_2_3_1, seed 20260907, C0/C2/C3, OFF/ON）PASSED。
- 50k 门（100_5_3_1, seed 20260907, C0/C2/C3, OFF/ON）PASSED。
- V2-OFF 对存储冻结运行（20k/50k 同配置）逐字节忠实（掩码=版本/runId/墙钟/实验Jar哈希派生字段）。
- 证据：`../14-checkpoint-equivalence/behavior-equivalence.csv`（228 IDENTICAL + 12 ON_ONLY + 0 DIFFER）。
