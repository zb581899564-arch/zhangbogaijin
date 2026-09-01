# V35-FC5-T：100-job 候选膨胀与利用断裂迁移审计报告

日期：2026-08-25  
状态：`IN_PROGRESS — existing evidence insufficient for H1 verdict`  
论文资格：`ROOT_CAUSE_DIAGNOSTIC_ONLY`  

## 已经可以确认的事实

1. FC-5 已在历史运行中发现“已生成/已进入合并池的强候选未被后续 working population 有效利用”的
   现象。这是利用断裂假设，不是 PDDR 程序错误裁决。
2. FC-6 的 `ORDER_SWAP`、`BP_RESERVED_LEGACY` 和 `REGION_AWARE` 失败，只否决了三种具体修法；
   它们没有否定候选膨胀或代表性损失是否存在。
3. A0↔A2 与 A2↔A4 的独立多实例确认都在100-job出现明确的负例：
   `100_8_3_1`（A2相对A0）与 `100_5_3_1`（A4相对A2）。对应正例为
   `100_2_5_1` 与 `100_2_4_1`。完整背景母表见
   `../01-existing-100job-background/100job-background-summary.csv` 和
   `../04-positive-negative-contrast/positive-negative-contrast-matrix.csv`。
4. 现有运行有最终指标和 Cmax 生命周期；字段可用性矩阵证实，它们缺少每轮合并池的严格ND数、四方向
   代表的PDDR/物理槽位/教师/后代链。因此不具备 H1 的四项预注册判据，不能把旧现象升级为100-job根因。

## 本轮实现的观察能力

- `V35Fc5TransferAudit`：同一真实合并池的 `Nmerge/Nunique/Nnd/Roverflow`，稳定四方向代表、实际
  PDDR选中身份/槽位、Qg/Qp教师暴露、教师派生后代的方向改善、archive-working三目标差距；
- `ZhangBoV35Fc5TransferRunner`：仅允许已预注册的 A0/A2/A4 对照实例、seed、预算，要求原始四向量
  snapshot，并以原子目录与文件级SHA-256写证据；
- 观察开/关等价测试：初群、FE、decoder调用、评价轨迹、Qp事件流和规范前沿一致；
- 审计器单元测试：严格精确ND去重、四方向代表、PDDR结果、教师利用、archive-working gap，以及相同
  基因型子代的 FIFO 观察结算均通过。

这些新增代码只在独立诊断构建物中使用；冻结Jar、历史臂语义、`GLOBAL_ORIGINAL`、DOE参数和正式矩阵
均未改变。

本地启动前验证已通过：`V35Fc5TransferAuditTest`、`V35Fc5TransferTelemetryEquivalenceTest`、
`V35FinalAblationProfileTest`、`V35FairRunnerTest` 和 `ZhangBoEvaluatedPddrSelectorTest` 共13项通过；
`jmetal-algorithm` 与 `jmetal-exec` 源码编译通过，两项新增 class 的 bytecode major version 均为`52`。
完整 Maven package 仍需要在具备完整 JDK
`javadoc` 工具的环境执行；当前桌面环境的 `JAVA_HOME` 未设置且只暴露运行/编译包装器，故未将 package
未执行误报为通过。

## 当前裁决

```text
FC5_TRANSFER_STATUS = INSUFFICIENT_EVIDENCE_FROM_HISTORICAL_LOGS
FC5_TRANSFER_REPLAY = FIRST_TIER_50K_ACCEPTED_PENDING_ANALYSIS
PDDR_CURRENT_DECISION = KEEP_GLOBAL_ORIGINAL
NEXT_ALLOWED_ACTION = analyze accepted first-tier telemetry; do not auto-escalate
```

本报告不构成 CFVF、双Q、CA-TA-Lite 或 FM3 的负面结论，更不授权删改它们。只有在 FC5-T 未能解释100-job
退化后，才按预登记顺序进入其它模块审查。

## 启动前恢复门

首档24条重放已于2026-08-25 18:41（Asia/Shanghai）启动并完成。原确认实验的12份四向量snapshot已直接从
两个权威训练机campaign恢复至新的隔离目录；每份物理SHA-256均与原receipt逐一一致，没有按seed重新
随机生成。诊断Jar、实例、SUT和疲劳参数在启动前完成绑定，2k只作为加载/输出结构探针，不进入结果。
首档固定为24条50k、12个独立JVM并行；24/24运行及12/12配对组通过验收，逐运行证据清单已反向
复算。A0/A2实际FE均为50000，A4按冻结阶段一致预算实际FE为48269。完成脚本已经停止，没有自动
升级到100k或250k。H1仍需对50k遥测作正式窗口和正负对照分析后才能裁决。
