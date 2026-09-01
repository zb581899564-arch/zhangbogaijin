# V35-FC5-MIDHORIZON-DIAGNOSTICS-V3：Checkpoint / budget 最终验收

先登记、后运行的 complete-phase 预算记录见 [PRE_REGISTERED_COMPLETE_PHASE_BUDGETS.md](PRE_REGISTERED_COMPLETE_PHASE_BUDGETS.md)。

## 预算语义

所有最终 ON/OFF 运行均使用：

- formalBudgetSemantics=PHASE_CONSISTENT_BUDGET_TERMINATION
- allowTerminalPartialFormalQPhase=false
- 不接受 partial formal Q phase；
- 不生成 synthetic checkpoint；
- checkpoint 统计中的 unobservable boundary 与 observer execution error 分开计数。

2k 的登记有效 cap 为 5100=100 initial FE+5000 complete Q phase；A4 20k 的登记有效 cap 为 20258=15258 previous complete boundary+5000 complete Q phase。这些 fallback 只改变诊断停止上限，不改变算法参数、决策或正式冻结 Jar。

## 最终 checkpoint 结果

| 运行组 | checkpoint schedule | 观察结果 | 最大 observed overshoot | unobservable FE boundary | 结论 |
|---|---|---|---:|---:|---|
| A2/A4 2k effective-5100 | 2000 | 2000→2000 | 0 | 0 | checkpoint PASS；短门 source coverage/CATA 仍不足 |
| A2 20k（两实例） | 5000,10000,15000,20000 | 四个名义点均到达 | 266 / 104 | 0 | PASS |
| A4 20k effective-20258（两实例） | 5000,10000,15000,20000 | 四个名义点均到达 | 200 / 99 | 0 | PASS |
| A2 50k | 10000,20000,30000,40000,50000 | 五个名义点均到达 | 266 | 0 | PASS |
| A4 50k | 10000,20000,30000,40000,50000 | 10000–40000 到达；50000 未到达 | 251（observed rows） | 1 FE boundary（3 个 front-type rows） | FAIL-CLOSED |

A4-50k 的实际停止为 actualFE=48269，距名义 50000 还剩 1731 FE；因为剩余预算小于一个完整 5000-FE formal Q phase，phase-consistent 分支正确停止，没有强行补跑 partial phase。最终 checkpoint 的三个 front-type 行均写入 CHECKPOINT_NOT_REACHED 与 RUN_END_NO_ATOMIC_SNAPSHOT。同一运行的 observerErrors=0、observerExecutionErrors=0，这不是 observer error。

因此，2k/20k 的登记 fallback 规则已执行；50k 没有未经登记的 fallback，A4-50k 的一个未观测末端 boundary 必须使总诊断 gate 保持失败。
