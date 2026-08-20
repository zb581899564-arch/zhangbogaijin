# P5 已被取代的诊断运行

下列日志保留用于说明验收过程中发现并纠正的运行方式问题，但不作为P5最终测试结论：

| 日志 | 问题 | 最终替代证据 |
|---|---|---|
| `TEST_PROBLEM_ALL.log` | 根reactor在JDK 17下运行旧Mockito测试时未加入所需模块开放参数，并混入既有绝对路径问题。 | `TEST_P2_P3_P5_PROBLEM_REACTOR.log`与`TEST_FULL_REGRESSION_JDK17_COMPAT.log` |
| `TEST_PROBLEM_ALL_FINAL.log` | 单模块运行错误地复用了本地仓库中较旧的`jmetal-core`制品，导致作者扩展worker向量长度不一致。 | `TEST_P2_P3_P5_PROBLEM_REACTOR.log`，其通过reactor同时构建所需模块 |
| `TEST_FULL_REGRESSION.log` | 首次完整回归未给旧Mockito测试传入JDK 17所需的`--add-opens`。 | `TEST_FULL_REGRESSION_JDK17_COMPAT.log` |

最终结论只引用“最终替代证据”列。被取代日志不表示P5源码缺陷，也不计入最终失败数；它们被保留是为了让证据链完整、避免隐藏失败尝试。
