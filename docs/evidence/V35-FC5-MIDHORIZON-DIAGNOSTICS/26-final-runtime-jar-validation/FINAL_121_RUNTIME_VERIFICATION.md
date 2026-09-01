# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1：121 运行 Jar 最终复验

复验日期：2026-08-26

## Jar 身份绑定

本次 ON/OFF 使用的是同一个、已封存且实际参与运行的诊断 runtime Jar：

```ini
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
diagnosticBaseJarSha256=723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F
diagnosticRuntimeJarSha256=121FBB4939258BDC94C297D5F6CE9BE0B0BEE0271A6E71B89BAE8E1486394155
```

正式算法 Jar 为冻结目录中的 `jmetal-exec-5.8-jar-with-dependencies.jar`，48,269,638 bytes；
诊断基础 Jar 为历史 V3 诊断 Jar；121 runtime Jar 为当前工作树构建的
`jmetal-algorithm-5.8-jar-with-dependencies.jar`，47,776,437 bytes。三者的实体副本/路径和
哈希见每个运行目录的 `runtime-provenance.properties`；121 与正式算法 Jar 的实体副本也位于
本目录。

两次 JVM 启动前都重新计算 121 的 SHA-256，并把该值传入 driver；运行输出的
`sourceJarSha256` 与 provenance 中的 `diagnosticRuntimeJarSha256` 一致，
`runtimeJarBindingVerified=true`。本次不再使用 E30 运行结果作为最终部署依据。

## 唯一最终复验对

配置为 A4、`100_5_3_1`、seed `20260901`、population 100、requestedMaxFE 50000；ON 与 OFF
各自独立 JVM，均返回 `status=COMPLETED`、退出码 0。运行目录为：

- `A4-50k-ON-s20260901-121FBB49`
- `A4-50k-OFF-s20260901-121FBB49`

两侧均得到 `actualFE=48269`、`remainingFE=1731`、`qPhaseFE=5000`。核心评价、初始种群、
评价轨迹、Qg/Qp、PDDR 和最终前沿哈希完全一致，详见
`A4_50K_121_ON_OFF_EQUIVALENCE.csv`。

ON 侧真实终止快照为 `ACCEPTED`，共 619 行：working population 72、decision archive 262、
observed full front 285。三类前沿均非空、有限、目标三元组唯一且相互可区分；终止元数据
全部为 `actualCheckpointFE=48269`、`checkpointDeltaFE=-1731`、
`PHASE_CONSISTENT_TERMINAL`、`REAL_ATOMIC_RUN_END_SNAPSHOT`。

## CA-TA 与最终状态

121 运行输出直接写入统一口径：

```ini
cataLifecycleSchemaValidated=true
cataLongRunLifecycleValidated=true
cataAllShortGateSourceCoverageValidated=false
cataFullLifecycleValidated=false
```

ON 侧直接输出 `diagnosticToolingValidated=true` 和
`250kReadyForPreregistration=true`；OFF 侧是无观察控制，输出 false。两侧都直接输出
`250kStarted=false`、`formalMatrixRunning=false`、`FC5=INCONCLUSIVE`，不需要事后归一化。

最终验收字段集中见 `FINAL_121_RUNTIME_ACCEPTANCE.properties`。测试日志副本为
`TEST_121_RUNTIME_COMMAND.txt`、`TEST_121_RUNTIME_OUTPUT.log` 和
`TEST_121_RUNTIME_RESULTS.csv`。

## 范围边界

这是最后一次 50k 复验。本轮没有运行 2k、20k、A2、250k 或 formal matrix，也没有上传。121
实体、正式算法实体、两侧运行产物、测试日志和本说明均纳入最终 Manifest；Manifest 反向复核
必须为 0 failures。
