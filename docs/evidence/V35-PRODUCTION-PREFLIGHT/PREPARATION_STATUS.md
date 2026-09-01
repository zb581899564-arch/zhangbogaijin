# Track C 准备状态

生成时间：2026-08-23（本地准备阶段）

## 已完成

- 外置 `V35ProductionPreflight` 已以 Java 8 字节码编译；它只调用现有
  `V35FinalAblationProfile`、`V35FairRunner` 和规范 FM3 Problem 的公开接口。
- 预检边界已固定为：`population=100`、`20_2_3_1`、诊断 seed `20260828`、
  A0--A4 各 `50000 FE`；仅当机制仍未触发时才允许另立 `100000 FE` 失败/扩展证据。
- 远端 scheduler 已设计为一次 SSH 启动：先串行跑 A0--A4，再以 A4 的 20k
  诊断负载测试 4/8/12/16 个独立 JVM。它不会遍历正式矩阵。
- driver 与 scheduler 均已完成静态语法/Java 8 编译检查。

## 等待条件

Track A 正在产生 final freeze jar。本 Track 不会以当前候选 jar 运行生产预检或
吞吐基准，也不会把候选结果写成 `PREFLIGHT_ACCEPTED` 或 `FORMAL_MAX_PARALLEL`。

## 已登记的语义—门冲突

一次候选 A4/20k 工具链检查已经证明：当前冻结的 Table-9 `Q_Times=50`、population
100、`allowTerminalPartialFormalQPhase=false` 与 A4 的 shared dynamic local-FE 窗口相组合，
可合法地在没有足够 5000-FE 完整 Q phase 时安全停止。该候选实际停于 15258 FE，所有
合法性和机制门均通过，但不满足 `requestedFE=actualFE`。

因此，若 final jar 复现该行为，不能通过改为 partial Q phase 来凑到精确预算；那会改变
正式 Q/LS 时序。必须由总控在两项原有契约之间裁决：保留 `actualFE<=requestedFE` 的安全
尾段语义，或另行选择一个经计算可精确收口的诊断预算。详见
`CANDIDATE_TOOLING_FAILURE_20260823.md`。

执行前必须取得：

1. final jar 的绝对路径与 SHA-256；
2. final freeze manifest 的路径和 SHA-256；
3. 该 jar 已通过 Track A 声明的 source/parameter freeze 复核。

获得后将重新使用该 jar 编译 driver，远端再次核对 jar SHA-256，然后启动唯一的
`v35-stage2-diagnostics` tmux 会话。任何失败运行会保留在该远端专属目录；不会改动
训练机现有 tmux 任务或正式实验目录。

## Final Gate3 更新

冻结 jar `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9`
已在隔离冻结副本上复验 A4/20k。结果精确重现候选行为：`actualFE=decoderCalls=15258`，
而请求为 20,000。故 Gate3 已从“等待 final jar”变更为 `BLOCKED`；根据 Track C 停止规则，
不运行其余臂、远端吞吐或任何正式矩阵。详见 `FINAL_GATE3_EXACT_FE_REPORT.md`。
