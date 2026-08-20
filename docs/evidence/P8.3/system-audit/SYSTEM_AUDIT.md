# P8.3系统专项复核

日期：2026-08-10

| 检查项 | 结论 | 证据/说明 |
|---|---|---|
| CA-TA新候选计FE | 通过 | Test/Apply候选均经唯一评价网关，每个新候选最多1 FE |
| Test完整性 | 通过 | 当前合法掩码中的全部邻域执行相同`nTest`次数 |
| Apply跨调用状态机 | 通过 | 每次父粒子调用最多1个Apply候选，总次数严格为`K×nTest×multiplier` |
| 掩码变化/连续失败重启 | 通过 | 掩码哈希变化或连续失败3次开启新Test epoch |
| 随机键 | 通过 | 包含master seed、generation、parent slot、lineage、epoch、call ordinal和neighborhood |
| 双Q区块 | 通过 | P/G区块按已完成外层代数推进，局部FE不推动五代切换 |
| Q信用隔离 | 通过 | Qg/Qp奖励在局部搜索前结算，只读取全局后代 |
| O13恢复门 | 通过 | 固定VNS和CA-TA共享同一自然恢复增益门 |
| PDDR重复偏差 | 通过 | 同一父粒子Apply不再重复相同候选；局部来源和预评价标记保留 |
| 谱系/父槽位/来源 | 通过 | global/local/parent映射和lineage事件测试通过 |
| 四向量与可变共享 | 通过当前定向门 | canonical解对象深复制、档案/速度/谱系定向回归通过；未恢复作者缺陷Solution |
| FM0–FM3与MA/WA身份 | 通过 | jmetal-problem全回归46项通过；I1图1–6和人工核算哈希不变 |
| 七槽三目标适配 | 通过 | 正式指标只读取`objective[0]/[1]/[6]` |
| HV/IGD reference | 边界已锁定 | 正式论文必须用同实例全部算法、全部run联合非支配集冻结reference、归一化边界和HV reference point；旧单seed指标隔离 |
| 无界日志/档案 | 通过工程门 | 事件有界4096并流式哈希；个人档案容量6；全局Pareto改为指纹去重的增量严格支配更新 |
| GC增长 | 可接受但需继续观察 | 100k JFR有71次Young GC；深复制31.79%仍是首要剩余热点 |

## 测试与构建

- `jmetal-problem`：46项，0失败/错误；
- CA-TA/双Q/日志/Pareto/烟测定向：20项，0失败/错误；
- P8定向：10项，0失败/错误；
- Runner/P9定向：7项，0失败/错误；
- 另有较早核心定向30项和Runner 15项继续保持通过；
- 六模块`mvn package`成功；
- `ZhangBoMOHPSOQ`和`ZhangBoP83PerformanceSuiteRunner`字节码major version均为52（Java 8）。

完整旧核心回归在JDK 17下加入`--add-opens=java.base/java.lang=ALL-UNNAMED`后为651项、0失败、3错误、6跳过。3个错误仍是P1登记的作者遗留签名：`PMXCrossoverTest`、`PermutationSwapMutationTest`、`DefaultIntegerPermutationSolutionTest`因旧默认构造器从模块工作目录寻找`EADHFSP/150_8_5_1.txt`失败；正式canonical生产路径不调用该构造器。

## 指标证据边界

旧P9单seed报告以当前FULL和BASE的并集构造reference，存在自贡献偏差，不能作为当前正式指标。既有六seed统一reference审计仍显示正向信号，但那是`legacy_pre_cata_apply_fix`结果。P8.3没有运行正式统计矩阵，也没有生成新的HV/IGD论文结论。

