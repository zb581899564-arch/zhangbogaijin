# P6.0来源映射

| 实现项 | 生产来源 | 验证来源 | 落点 |
|---|---|---|---|
| 2状态、3动作Q表 | 作者`MOHPSOQ`的`actionset/Q`相关代码 | P4 `QGbestController`语义oracle | `ZhangBoQgController` |
| 四子群方向 | 作者活动分群：Cmax/PDDR/TEC/TWC | ESWA第四章、P4四子群测试 | `ZhangBoSubSwarm` |
| 领导接线时间点 | 作者主循环结构 | 用户批准P6.0计划 | `ZhangBoMOHPSOQ.prepareOriginalQg()` |
| 奖励结算时间点 | 原Q奖励含义 | 用户批准“评价后、历史/局搜前”硬门 | `settleOriginalQg()`位于`updateLeaders()`入口 |
| Builder/Runner开关 | P4.1直接派生接口 | 用户批准保留旧签名 | 新setter、重载Runner入口 |

作者Q代码存在但活动`perturbation()`为空的事实登记为D-020。P6.0不声称改变作者Q论文定义，也不把CFVF效果混入Qg恢复。

