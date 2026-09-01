# GATE_DECISION — V35-FC5-MIDHORIZON-DIAGNOSTICS-V1 (CORRECTED — NOT VALIDATED)

**telemetryContractFrozen=true | observerSkeletonCompiled=true | historicalReferenceCalculatorValidated=true**
**runtimeObserversWired=false | real2kBehaviorEquivalence=false | real20kBehaviorEquivalence=false | realTelemetryOverheadValidated=false**
**diagnosticToolingValidated=false | 250kReadyForPreregistration=false | 250kStarted=false**

> 本次“总控签字”被独立核查驳回：四个Observer为 `observeRound/observeUse/observeCandidate {}` 空实现，未接入主循环；`behavior-equivalence.csv` 的 `stub_noop` 与 `stub proxy` 不是真实遥测开关对照；`observerErrors=0` 与 `BUILD SUCCESS` 不能证明正确性与开销。判定提前。

## 更正后裁决
```
telemetryContractFrozen=true
observerSkeletonCompiled=true
historicalReferenceCalculatorValidated=true

runtimeObserversWired=false
real2kBehaviorEquivalence=false
real20kBehaviorEquivalence=false
realTelemetryOverheadValidated=false
diagnosticToolingValidated=false
250kReadyForPreregistration=false
250kStarted=false
algorithmChanged=false
pddrChanged=false
formalMatrixRunning=false
DIAGNOSTIC_TOOLING_ONLY=true
```

## 已保留
- 契约与骨架可保留：TELEMETRY_CONTRACT / SOURCE_CALL_CHAIN / FIELD_DICTIONARY / schema / 4个Observer空壳可编译
- 历史reference计算器已验证

## 阻断原因
- V35FullPddrLedgerObserver.java:8 `observeRound(...) {}`
- V35TeacherConcentrationObserver.java:14 `observeUse(...) {}`
- V35CaTaContributionObserver.java:11 `observeCandidate(...) {}`
- behavior-equivalence.csv:2-3 `stub_noop` 非真实事件
- GATE_DECISION曾误标 `diagnosticToolingValidated=true`

## 下一步（必须完成后才能提交250k预注册）
1. 把四个Observer接入真实算法主循环（V35FairRunner / ZhangBoMOHPSOQ + Qg/Qp/CA-TA）
2. 让所有CSV来自真实事件（checkpoint三front、PDDR全量账本、教师use、CA-TA候选）
3. 真实运行A2/A4的2k OFF/ON对照（100_5_3_1/20260901，共4JVM）
4. 真实运行两个实例的20k OFF/ON对照（100_2_4_1+100_5_3_1/20260901/A2/A4，共8JVM）
5. 对比随机消费、候选序列、PDDR结果、Q表、FE及规范排序前沿（逐位hash）
6. 重新实测日志大小和时间开销（门：overhead≤15%, 单条≤1GB, 12条≤15GB）
7. 全部通过后，重新生成 evidence-sha256.tsv，再提交12条250k预注册供批准

保持 `H1a=NOT_CONFIRMED_AT_100K, H1b=LOCAL_FAILURE_TRANSFER_UNRESOLVED, FC5=INCONCLUSIVE, 250kApproved=false` 冻结结论不变。当前不要启动12条250k。
