# P6.2 谱系化个人非支配档案报告

日期：2026-08-09  
语义：`fatigue_improved`生产派生线上的`B3=B2P+谱系个人档案`  
状态：`completed`

## 结论

已为PDDR保留后的每条粒子谱系建立容量6的个人非支配档案。档案当前是影子记忆：不参与pbest选择、Qg、CFVF或评价预算。使用同一显式初始种群并在每次对照运行前重置作者jMetal随机单例后，B2P与B3的主种群结果、最终非支配集、Qg表、CFVF事件、PDDR事件和FE逐项一致。

## 固定契约

```text
capacity=6
normalizationEpsilon=1e-12
epsilonDup=1e-4
kappa=0.05
Risk=0.5*normalized(Fmax)+0.5*normalized(FE)
fatigueTieObjectiveDistance<=1e-4
```

- 条目只保存四向量快照、目标`[0,1,6]`、`Fmax/FE`、来源、代数、评价序号和稳定指纹；
- 每代由主种群与全局非支配集合冻结归一化边界，但全局集合不向个人档案注入解；
- 更新依次执行严格Pareto过滤、近重复连通分量去重、方向锚点、加性epsilon锚点和最远点填充；
- G1/G2/G3/G4方向分别为Cmax、三归一化目标最大值、TEC和TWC；
- 单分支保留沿用ID，多分支保留退休旧ID并稳定分裂，无分支则删除，换子群只更新标签；档案深复制且不共享可变容器。

## 验证

- 14项P6.1.1/P6.2组件测试通过，覆盖严格支配、近重复连通分量、容量1和6、四子群方向锚点、epsilon指标、最远点、低疲劳近似破平、输入顺序稳定，以及继承、分裂、删除、迁移和合成局部后代来源；
- 集成测试6项通过；`20_2_3_1`固定100粒子、2000 FE，活动谱系始终100，档案大小始终1..6，非法解0，CFVF后置repair为0；
- 每条`evolve`事件记录代数、来源、子群、档案大小和插入是否存活；最终日志记录档案大小分布及插入、支配删除、去重、截断、分裂、删除和迁移累计数；
- 本次证据运行最终档案分布为`{2=5,3=14,4=21,5=35,6=25}`，累计`insertions=1900`、`dominatedRemoved=148`、`duplicatesRemoved=1504`、`truncatedRemoved=8`、`splits=655`、`deletions=655`、`migrations=662`；这些是工程烟测诊断，不是正式实验统计；
- 定向回归54项通过，六模块Java 8打包通过；完整旧回归保持651项、0 failures、3个P1既有errors、6 skipped；
- 作者`MOHPSOQ/Builder/EDHHFSPW/Runner`四个原文件SHA-256继续与P4.1冻结值一致。

## 随机性说明

P6新增随机事件仍由可注入生成器控制。消融等价测试还显式重置作者算法沿用的jMetal全局随机单例；普通作者Runner的初始化及遗留随机调用仍不能据此宣称完整固定seed重放。

## 状态边界

`lineage_archive_engineering_validated=true`、`lineage_archive_scheme_aligned=true`。本阶段未实现Q-pbest、双Q冻结、O10–O13、CA-TA-VNS或正式实验；`sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`。
