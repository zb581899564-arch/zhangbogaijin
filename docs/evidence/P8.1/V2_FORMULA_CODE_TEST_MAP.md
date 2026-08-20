# 总体v2公式—源码—测试映射

| 方案机制 | 当前实现 | 验收证据 |
|---|---|---|
| `PT0=ST/(MS×WE)` | `ZhangBoFatigueEvaluator`加工分量 | `ZhangBoCanonicalFormulaTest`手算，容差`1e-9` |
| `SET0=SUT/WE` | 实例绑定SUT扩展与设置分量 | 严格manifest加载、黄金桥与公式测试 |
| `AT0=PT0+SET0` | 同一OperationRecord的三项基础时长 | 分量求和断言 |
| `AT=AT0[1+r/ln2×ln(1+Fstart)]` | `ZhangBoFatigueModel`和生产Evaluator | 倍率、最大30%增幅及delta等价断言 |
| 自然恢复 | `Fstart=Flast×exp(-mu×idle)` | FM1/FM2差异、恢复公式测试 |
| 疲劳累积 | `Fend=Fstart+(1-Fstart)(1-exp(-lambda×AT))` | 累积公式及边界测试 |
| 四向量身份 | JS逆映射后读取FA/MA/WA | 非canonical JS、非零MA/WA生产测试 |
| 后续疲劳ECT选工 | FM3枚举合法工人并稳定破平 | synthetic ECT/破平测试 |
| CFVF | 按工件身份生成FMW/MW/M/W动作 | `ZhangBoCfvfUpdaterTest` |
| 个人档案与Qp | 容量6谱系档案、16状态×4动作 | Archive、Lineage、Qp测试 |
| 原Qg与双Q | 2×3 Qg；10%预热；五代P/G冻结 | Qg、Qp、DualQ测试 |
| CA-TA-VNS | 六瓶颈、Need、等预算Test/Apply、O1–O13 | CA-TA组件/集成、Neighborhood测试 |

说明：生产载体为作者兼容七目标槽，主目标固定读取`[0,1,6]`；这是一种接口兼容映射，不将七槽数组描述成论文原生三槽接口。
