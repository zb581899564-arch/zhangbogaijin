# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1：独立终止快照复核（历史 E30 对）

复核日期：2026-08-26。本文件记录此前 E30 运行对的独立检查；最终部署 Jar 的复核已由
`../26-final-runtime-jar-validation/FINAL_121_RUNTIME_VERIFICATION.md` 完成。

## 复核范围

本复核只读取本轮新生成的证据，不启动新的优化运行。唯一真实验证对为：

- algorithm：`A4`
- instance：`100_5_3_1`
- seed：`20260901`
- requestedMaxFE：`50000`
- arms：独立 JVM 的 `ON` 与 `OFF`
- 实际运行 fat jar：`E30BB9AD914B278C7F0DAB64433CE20D4EDD217C3AB7351EB00481E49F2A38B6`

运行产物位于本目录上一级的
`23-a4-50k-terminal-validation/A4-50k-ON-s20260901` 和
`23-a4-50k-terminal-validation/A4-50k-OFF-s20260901`。两次运行均正常返回；本复核没有重跑
2k、20k、A2、250k、formal matrix，也没有上传。

## 逐项结果

| 检查项 | 结果 | 证据 |
|---|---|---|
| `actualFE` | PASS：48269 | ON/OFF `behavior-summary.properties` |
| `remainingFE < qPhaseFE` | PASS：1731 < 5000 | ON summary；V3.1 合同 |
| `actualFE == lastCompletedAtomicBoundaryFE` | PASS：48269 == 48269 | ON summary |
| 终止分类 | PASS：`ACCEPTED` | ON `telemetry-checkpoint-fronts.csv` |
| 终止行 | PASS：619 | 72 + 262 + 285 |
| 三前沿完整性 | PASS | `TERMINAL_FRONT_VALIDATION.csv` |
| 终止元数据一致 | PASS：坏元数据 0 | 独立逐行复核 |
| 终止目标值有限 | PASS：非有限值 0 | 独立逐行复核 |
| `unobservableCheckpointCount` | PASS：0 | ON summary |
| `observerExecutionErrors` | PASS：0 | ON summary |
| ON/OFF 核心行为 | PASS | `A4_50K_ON_OFF_EQUIVALENCE.csv` |
| 限定测试 | PASS：20/20，0 failures，0 errors | `22-terminal-checkpoint-implementation/test-results.csv` |
| formal frozen Jar | PASS：SHA-256 未变 | `723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F` |

终止行的实际分组为：`workingPopulationND=72`、`decisionArchiveFront=262`、
`observedFullFront=285`。619 行全部带有 `nominalCheckpointFE=50000`、
`actualCheckpointFE=48269`、`checkpointDeltaFE=-1731`、
`atomicBoundary=REAL_ATOMIC_RUN_END_SNAPSHOT` 和
`terminationKind=PHASE_CONSISTENT_BUDGET_TERMINATION`；三组前沿均非空、有限且彼此可区分。

ON/OFF 的 `actualFE`、评价调用数、初始种群、评价轨迹、QG/QP、PDDR、规范前沿等核心字段
完全一致。OFF 不执行诊断观察，也不产生 ON 专属 telemetry RNG/candidate digest，因此这两个
ON-only 字段在等价表中明确标为 `NOT_APPLICABLE_OFF`，没有被伪装成两侧相等。

## 原始运行与最终报告口径

真实运行产物是用上面的 `E30...` fat jar 保存的原始输出，原始文件保持不改写。随后构建的当前
诊断 fat jar 为
`121FBB4939258BDC94C297D5F6CE9BE0B0BEE0271A6E71B89BAE8E1486394155`；该次构建只修正/补充
报告层字段（CA-TA 拆分、readiness、终止门字段和排序前沿别名），没有再次运行实验。

因此，最终验收中的归一化字段以
`25-v31-final-decision/V31_NORMALIZED_ACCEPTANCE.properties` 为准；它由上述已保存原始证据
逐项推导，不覆盖原始运行记录。该文件明确保留以下边界：

```text
cataLifecycleSchemaValidated=true
cataLongRunLifecycleValidated=true
cataAllShortGateSourceCoverageValidated=false
cataFullLifecycleValidated=false
```

这表示 V3.1 终止快照工具链已验证，但不能声称 CA-TA 全生命周期已完成验证。

## 独立结论

该历史 E30 对证明了终止快照协议满足 V3.1 的 phase-consistent 条件；它不承担最终部署 Jar
身份。最终 121 runtime 对同样通过了该协议和行为等价复验，诊断工具可进入 preregistration
准备状态，但这不等于已批准或已启动 250k，也不构成 FC5 根因结论。最终决策见
`../25-v31-final-decision/V31_FINAL_DECISION.md`。
