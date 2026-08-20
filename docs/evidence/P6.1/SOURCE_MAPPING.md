# P6.1来源映射

| 实现项 | 方案口径 | 作者生产语义 | 落点 |
|---|---|---|---|
| JS交换速度 | 综合v2/P6计划 | 作者交换序列更新 | `applyJobSequenceChannel()` |
| 资源差分 | `FMW/MW/M/W`且按工件身份 | 四向量按位置存储 | `resourceDifference()`+JS逆映射 |
| 资源惯性 | `omegaR=0.5` | solution attribute随副本传递 | `ZhangBoResourceVelocity` |
| 认知/社会抽样 | `c1R=c2R=Rand_k=0.4` | 原pbest+P6.0 Qg | `sampleDifference()` |
| 冲突 | 粒度优先、同粒度按eta、双零50/50 | 不使用固定执行顺序覆盖 | `resolveLeadership()` |
| 探索 | `pExplore=0.05`，每后代最多一个 | 当前实例第一阶段合法域 | `maybeAddExploration()` |
| repair | 异常安全网、最小合法编号 | 不修改默认作者路径 | `repairForSafety()` |
| 预算 | 每后代一次完整P5评价 | `evaluateSwarm()`统一计数 | `fullEvaluationCount` |

工程默认只用于P6验收；正式敏感性和论文实验仍属于P9。

