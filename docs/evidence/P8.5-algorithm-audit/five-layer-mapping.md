# 总体v2—细节方案—论文—代码—测试五层映射

| 机制 | 总体v2主语义 | 辅助来源 | 代码落点 | 测试/运行证据 | 判定 |
|---|---|---|---|---|---|
| 四向量与身份映射 | JS/FA/MA/WA位置对齐，跨解按工件身份 | ESWA Fig.3、P2契约 | `DhhfspFourVectorSolution`、`ZhangBoFatigueEvaluator` | 非标准JS、MA/WA响应及I1轨迹测试 | `ALIGNED` |
| FM0–FM3 | 累积、恢复、工时反馈、疲劳ECT选工逐层开启 | 疲劳细节方案仅作解释 | `ZhangBoCanonicalProductionProblem`、`ZhangBoFatigueModel` | 55项problem测试、I1 20工序回归 | `ALIGNED` |
| FCLS/FCRS | 原移位策略的疲劳一致扩展 | P3静态移位仅作oracle | `fatigue.shift`包 | 幂等、1 FE边界、I1真实候选 | `ALIGNED`（I0人工门待完成） |
| HMOPSO-QGS基线 | 原Qg、PDDR、工厂间搜索、O1–O9 | ESWA Table 9、Fig.5/6 | `ZhangBoFormalHmopsoQgsConfiguration`、`ZhangBoBaselineUpdater` | B1 20k：50 Q轮、关键搜索和14770次O1–O9 | `ALIGNED` |
| 严格PDDR | 三目标严格Pareto与稳定破平 | ESWA/学位论文PDDR | `ZhangBoEvaluatedPddrSelector` | 重复目标及稳定顺序测试 | `ALIGNED` |
| CFVF | JS+FMW/MW/M/W、资源惯性和合法探索 | Qp细节方案 | `ZhangBoCfvfUpdater` | 工件身份、冲突、探索、repair测试；FULL 20k | `ALIGNED` |
| 谱系个人档案 | 容量6、本谱系已评价分支、严格Pareto | Qp细节方案 | `ZhangBoLineageArchive*` | 影子模式归一化回归、I1谱系链 | `ALIGNED` |
| Qp/Qg与冻结 | 16×4 Qp、2×3 Qg、10%预热、B=5 | Qp细节方案 | `ZhangBoQpController`、`ZhangBoDualQCoordinator` | Q表/动作/局部FE隔离测试及FULL 20k | `ALIGNED` |
| O10–O13 | 关键块、设置疲劳、机工联合、恢复窗口 | VNS细节方案；O13以v2覆盖旧错配口径 | `ZhangBoNeighborhoodSuite`、`ZhangBoNaturalRecoveryGate` | 固定/CA-TA双路径O13门、邻域测试 | `ALIGNED` |
| CA-TA | 四群×三阶段×停滞×六瓶颈，80/20 Need，等预算Test/Apply | VNS细节方案 | `ZhangBoCaTaController/Statistics` | 状态机、代价信用、I1和20k非零事件 | `ALIGNED` |
| 非目标 | 不加主动休息、多技能、第五染色体、第四目标 | 总体v2优先覆盖旧细节 | 全生产路径静态扫描 | 构建/配置/目标适配测试 | `ALIGNED` |

## 经批准的工程化偏差

- 疲劳参数采用已批准的`lambda/mu[f,w,k]`标准化实例参数化；论文中必须说明是计算抽象，不是真实精确生理参数。
- 为兼容作者框架，生产solution保留7个目标槽；正式比较只读取`[0,1,6]`。
- CA-TA正式代价信用使用真实单调时钟，确定性时钟只用于语义回放。
- 34个论文标签中有7个跨矩阵精确别名；物理配置相同才复用，不建立虚假差异。
