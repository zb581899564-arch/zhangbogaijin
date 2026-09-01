# 远端吞吐 benchmark 未启动

本文件记录旧的 strict-exact 协议阻断，现标记为
`legacy_pre_phase_budget_protocol`。在方案 C 下，`actualFE=15258` 对 20k 请求本身不再
构成失败；只有 `actualFE>MaxFEs`、decoder 不闭合或尾段不小于完整 Q phase 才失败。

新的本地 Gate3 五臂重验已在
`../V35-PHASE-BUDGET-PROTOCOL/03-gate3-preflight/GATE3_PHASE_BOUND_REPORT.md` 通过；
该前置已完成：Java 8 外部工具、冻结 jar SHA 和 phase-bound Gate3 均已复核，并已在训练机
执行 4/8/12/16 JVM 吞吐测试。

本文件名保留为历史兼容；下面的“0”计数只描述 strict-exact 阻断时的旧状态，不能再当作
当前状态。当前验收结果见
[`REMOTE_GATE3_THROUGHPUT_ACCEPTANCE.md`](../V35-PHASE-BUDGET-PROTOCOL/05-remote-throughput/REMOTE_GATE3_THROUGHPUT_ACCEPTANCE.md)：

```text
remote deployment diagnostics = 45 (5 Gate3 + 40 throughput)
4-JVM benchmark runs = 4
8-JVM benchmark runs = 8
12-JVM benchmark runs = 12
16-JVM benchmark runs = 16
formal matrix runs = 0
```

训练机创建了隔离的 Stage2 diagnostic 目录；未接触已有 `fc6-*` tmux 会话或任何非本项目任务。
正式矩阵仍因为 A0--A4 Master launcher 缺口而未启动。
