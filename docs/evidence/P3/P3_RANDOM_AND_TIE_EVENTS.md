# P3 随机与破平事件契约

固定seed：`20260808`。

| 事件 | 键 | 行为 |
|---|---|---|
| `SEEDED_WORKER_PERMUTATION` | `factory/stage` | 由seed、工厂、阶段混合得到局部seed，Fisher–Yates生成无放回排列 |
| `WORKER_KEYED_CHOICE` | `factory/stage/jobOrdinal` | 初始、微调和重复解码读取同一序号事件，不依赖遍历快照的随机调用次数 |
| `DETERMINISTIC_WORKER_ORDER` | `factory/stage` | 升序工人编号，不创建Random |
| `ETC_TIE_BREAK` | 上阶段相同完工时刻 | 上阶段调度序号优先，再按工件编号 |
| `FAM_TIE_BREAK` | 相同最早可用时刻 | 较小机器编号 |
| `WORKER_TIE_BREAK` | 相同最早可用时刻 | 较小工人编号 |
| `RIGHT_SHIFT_ACCEPTANCE` | 单个候选 | 约束合法、各工厂Cmax不变、TWC不变且TEC不增加时接受 |

测试对两种生产模式分别执行100次完整解码并比较`DecodeResult.toCanonicalText()`，两组均字节级一致；两种模式彼此保持不同，防止随机论文语义被确定性默认静默覆盖。

