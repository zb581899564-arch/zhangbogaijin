# 诊断工具封板令（DIAGNOSTIC TOOLING FREEZE）

- 生效日期：2026-08-29
- 依据：`PFC5_PHASE0` 执行包 §十一；裁决 `STEP0=SATISFIED_AFTER_OFFLINE_RECONSTRUCTION`
- 机器可读状态：同目录 `diagnostic-freeze.properties`

```ini
diagnosticToolingValidated=true
diagnosticToolingFrozen=true
diagnosticRuntimeJarSha256=121FBB4939258BDC94C297D5F6CE9BE0B0BEE0271A6E71B89BAE8E1486394155
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
diagnosticBaseJarSha256=723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F
behavioralEquivalence=true
terminalCheckpointProtocol=PASSED
phaseConsistentTermination=true
cataFullLifecycleValidated=false
observerErrors=0
newStep0Runs=0
```

## 封板效力（自本令生效起）

1. **不得增加观察字段**：遥测 schema 冻结为 121 runtime 所产出的
   7 类 telemetry CSV 字段集。
2. **不得以"分析更详细"为由修改诊断 Jar**：`121FBB49…` 为唯一获准部署实体；
   权威副本位于 `26-final-runtime-jar-validation/` 与
   `V35-FC5-MIDHORIZON-250K/00-preregistration/runtime/`（SHA 一致）。
3. **不得重做 2k/20k/50k 工具实验**：Step 0 以既有证据满足，`newStep0Runs=0`。
4. **升级通道**：若 F2 暴露真正阻断性字段缺失，停止运行并重新申请诊断工具
   版本升级（新预登记 + 用户批准），不得在运行中临时改工具或事后归一化。
5. **工作树纪律**：`java-jmetal58` 工作树在封板后曾被再次构建（jmetal-algorithm
   target 现为 `a0a1e74d…`，即 250k 诊断所用的后继重建）。本封板不追溯该历史事实，
   但自此起：任何新构建产物进入正式或诊断链路前，必须先登记源差异审计并获批准。
6. `cataFullLifecycleValidated=false` 与
   `cataAllShortGateSourceCoverageValidated=false` 为原始运行输出，永久如实保留，
   不得归一化为 true。
