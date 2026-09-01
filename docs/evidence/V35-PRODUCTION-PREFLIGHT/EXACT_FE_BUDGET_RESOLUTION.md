# V35 Stage2 严格 Exact-FE 预算方案裁决（历史隔离）

> 证据状态：`legacy_pre_phase_budget_protocol`。本文件保留 strict-exact
> 政策不可闭合的证据，不再定义当前正式协议。当前批准的方案 C 是
> `PHASE_CONSISTENT_BUDGET_TERMINATION`，只读协议叠加、不改 jar 或搜索语义。

日期：2026-08-23  
结论：`NO_COMMON_STRICT_EXACT_BUDGET_PLAN_WITH_CURRENT_FROZEN_SEMANTICS`

## 决策输入

总控在 Gate3 阻断后选择保留严格契约：

```text
requestedFE = actualFE = decoderCalls
```

同时禁止修改冻结算法、Q_Times、局部 FE Pacing、局部搜索顺序或开启 terminal partial
Q phase。故本裁决只检验冻结 A4 在 Track C 已许可的生产级诊断预算点，不能以改代码或
改变算法时序取得“精确”。

## 冻结 A4 实测映射

| requested FE | actual FE | decoder calls | Gate | 说明 |
|---:|---:|---:|---|---|
| 50,000 | 48,269 | 48,269 | FAIL | 预注册生产预检 |
| 100,000 | 96,025 | 96,025 | FAIL | 协议允许的记录化扩展 |

两次均满足：`COMPLETED`、前沿非空、非法解/重复评价/非有限目标/repair 均为零，且
Qg、DSCR、CFVF、PA_i/Qp、P5/G5、CA-TA-Lite 与 inherited LS 都实际触发。
失败仅来自 exact-FE 等式。

## 根因与结论

冻结语义以 `Q_Times=50` 与 population 100 执行不可拆分的 5,000-FE Q phase，随后允许
共享 dynamic Local-FE 窗口消耗预算；当剩余预算不足完整 Q phase 时，
`allowTerminalPartialFormalQPhase=false` 要求安全停止。局部候选是否可评价还依赖
instance、seed 与搜索轨迹，因此尾段并非一个可以从单一 A4 运行外推到 A0--A4、45 实例和
20 seed 的固定常数。

因此，50k/100k 的双点复验已否定“只选择另一个已许可预算即可让全部 arm 严格闭合”的假设。
在不改变冻结停止语义的条件下，不能诚实地声明一个共同 exact-FE 正式预算方案。

## 允许的后续治理路径

1. 若要保留当前冻结算法，必须改回安全尾停预算口径；
2. 若要坚持 strict exact-FE，则必须单独批准一个新的停止/终端评价语义，完成源码变更、
   新 freeze、A0--A4 生产预检及公平性复验后，才能重新申请正式矩阵。

本文件不批准、也不启动上述任一路径；当前 `formal_matrix_started=false`。
