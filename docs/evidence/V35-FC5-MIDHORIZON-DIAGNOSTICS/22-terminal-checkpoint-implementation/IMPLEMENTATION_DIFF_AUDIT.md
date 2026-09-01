# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3.1：实现差异审计

## 结论

V3.1 只增加诊断协议和只读观察接缝，没有修改正式算法的选择、评价、随机流、Q 表、PDDR、
CA-TA、CFVF、FM3、DOE 参数或子群比例。

## 最小实现

- `V35TerminalCheckpointContract.java`：实现纯函数 `classify(...)`，拒绝零/负 FE、超预算、
  `remainingFE >= qPhaseFE`、终止类型/原子边界不符、三前沿不完整、partial phase 和观察器错误。
- `V35CheckpointFrontObserver.java`：保存 `lastCompletedAtomicBoundaryFE` 及边界元数据；在
  终止点对三个前沿执行同一事务式快照，目标有限、非空或三者不可区分时 fail-closed；发布
  `PHASE_CONSISTENT_TERMINAL` 时保留 `nominalCheckpointFE=50000`、`actualCheckpointFE=48269`、
  `checkpointDeltaFE=-1731` 和 `REAL_ATOMIC_RUN_END_SNAPSHOT`。
- `V35MidHorizonTelemetry.java`：增加 `onTerminalRunEnd(...)`，把三个真实状态列表一次性
  传入观察器；OFF 路径在读取列表前返回。
- `ZhangBoMOHPSOQ.java`：算法 `run()` 返回前调用只读终止钩子，读取当前 swarm、决策档案和
  passive evaluation archive；不参与算法决策或 FE 计数。
- `V35MidHorizonDiagnosticDriver.java`：锁定本轮唯一调用形状，输出终止协议字段、三前沿门和
  CA-TA 拆分字段，并生成规范排序前沿别名。

## 运行与构建指纹

此前 V3 诊断对使用的 fat jar（保留为历史证据，不再作为最终部署依据）：

```text
E30BB9AD914B278C7F0DAB64433CE20D4EDD217C3AB7351EB00481E49F2A38B6
```

随后构建的 V3.1 报告层修正版（此前未参与真实 50k 运行）：

```text
121FBB4939258BDC94C297D5F6CE9BE0B0BEE0271A6E71B89BAE8E1486394155
```

后一次构建只包含报告字段补充（CA-TA 三标志、readiness、终止门字段和排序前沿输出）。随后
已用该 121 Jar 完成最终唯一一对 A4 50k ON/OFF 复验，运行产物和 121 实体副本见
`26-final-runtime-jar-validation`。Jar 身份必须拆分记录，正式算法 Jar、诊断基础 Jar 和
诊断 runtime Jar 分别为：

```text
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
diagnosticBaseJarSha256=723D24ED3021A01FACDA0231E3B142238E740FB18D025A4341748F2AF8D22E2F
diagnosticRuntimeJarSha256=121FBB4939258BDC94C297D5F6CE9BE0B0BEE0271A6E71B89BAE8E1486394155
```

正式算法 Jar 保持不变；121 runtime Jar 是本轮最终实际运行并已封存的部署候选。

## 测试

`V35Fc5MidHorizonDiagnosticsV31ContractTest` 与 `V35MidHorizonObserverRealTest` 共 20 个
测试全部 PASS，0 failures，0 errors。命令、Java 版本和逐方法结果见同目录的
`test-command.txt`、`test-output.log`、`test-results.csv`。

2k/20k/A2/250k 和 formal matrix 测试入口没有在本轮执行；既有 2k 等长实验测试被标记为
V3.1 禁止范围，不构成新的运行证据。
