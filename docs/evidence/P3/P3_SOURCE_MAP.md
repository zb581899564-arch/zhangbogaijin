# P3 来源映射

## 固定来源

| 来源 | SHA-256 | 使用范围 |
|---|---|---|
| `E:\学习\eswa2026-最新李明哲第四.pdf` | `BBBF3051E3B0B4F24A6B7FDC01DBAE7375D774E2467738A4DD8E49EAECACCF9D` | 第7–9页表4、表5、Fig.3、Algorithm 2；Fig.4调度示意 |
| `E:\学习\李明哲-毕业材料\3.毕业论文\104_2022930913_李明哲.pdf` | `D835DCD5B15BF767F432F80835235BE740E7BD5BA0EF9DDB70437FB4EF91EC3A` | 第59–64页交叉复核；Algorithm 4-2 |

P2已经逐字段冻结表4、表5、Fig.3和Algorithm 2。本工作包只消费该冻结契约，不重新解释图5、图6或引入第三章语义。

## 规则到实现

| 规则 | 主来源 | 实现入口 | 可执行证据 |
|---|---|---|---|
| 第一阶段按JS位置访问，FA/MA/WA直接赋值 | ESWA Fig.3、Algorithm 2行3–6 | `OriginalDhhfspDecoder.construct()`、`DhhfspFourVectorSolution.get*AssignmentForJob()` | Fig.3十个第一阶段逐工件断言 |
| `setup=SUT/WE`、`processing=ST/(MS×WE)` | ESWA表4、表5及正文 | `OriginalDhhfspDecoder.construct()` | J6/F2/S1/M2/W2手算断言 |
| 后续阶段ETC顺序、FIFO破平、FAM | ESWA Algorithm 2行19及正文 | `construct()`、`recordEtcTies()`、`firstAvailableMachine()` | 三工件同刻合成夹具 |
| 首批工人随机选择，超出人数后最早可用 | ESWA正文；用户确认随机无放回 | `workerPermutation()`、`selectWorker()` | 双模式重放与三工件合成夹具 |
| 枚举机器—工人共同空闲窗口 | ESWA Algorithm 2行7–18 | `earliestCommonStart()` | Fig.3 J1/F2/S1由44.0220插入到17.8099 |
| 右移降低待机能耗且不改变Cmax | ESWA Algorithm 2行21、Fig.4 | `rightShift()` | 资源顺序、工厂Cmax、TWC、TEC门槛断言 |
| 三目标`Cmax/TEC/TWC` | ESWA目标定义 | `objectives()`、`DhhfspProblem.evaluate()` | 冻结目标分解与评价计数断言 |
| 待机能耗率1.0 | 用户确认的作者兼容口径，非表5 | `UnitStandbyEnergyRateProvider` | provenance固定为`author_actual_compatibility` |

## Fig.3与Fig.4边界

- Fig.3工厂2的工件集合为`J6/J5/J8/J3/J1`。
- Fig.4图例为`J1/J3/J7/J8/J9`。
- 两者不是同一集合，论文没有给出足够证据证明Fig.4来自Fig.3编码。因此Fig.4只冻结可见的`54.9→45.9→45.9`及微调/右移结构，不被用来反向调整Fig.3结果。

