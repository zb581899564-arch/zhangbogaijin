# V35 档案候选实验指南

状态：候选代码已本地编译；本指南不构成训练机运行授权。

## 1. 入口隔离

正式Runner没有档案实验参数，正式构造器也不暴露ND1至ND4。未来只有专用入口
`V35ArchiveExperimentRunner`可以运行预注册臂；所有臂固定在A4、FM3、Shift NONE、单族、
序列无关SUT、`GLOBAL_ORIGINAL`、当前局部搜索顺序和方向教师池关闭的边界内。

调用结构为：

```text
显式实例与Problem
-> 显式冻结初始四向量种群
-> V35ArchiveExperimentProfile
-> V35ArchiveExperimentRunner.run(...)
-> V35FairRunner.runArchiveExperiment(...)
-> RunRecord + V35ArchiveExperimentArtifacts
```

专用Runner内部使用`V35FinalAblationProfile.A4_BUDGET_AWARE_CATA`构造冻结机制配置；档案臂只能
改变档案模式/容量/教师视图，不能顺带修改PDDR、双Q、CA-TA、局部FE或子群配比。

## 2. 输出契约

`writeRecord`在普通V35证据之外增加：

```text
archive-experiment-configuration.txt
archive-audit-summary.properties
archive-audit-events.csv
representative-front-k30.csv
sensitivity-front-k25.csv
sensitivity-front-k50.csv
```

配置文件同时记录arm、archive mode、容量、目标槽、选择算法和SHA-256。输出目录必须先写临时
目录再原子发布；未来远端Runner还需添加输入、Jar、初群和结果的文件级清单。

## 3. 本地验收（本工作包）

本地只允许：

- maximin单元测试；
- 五个profile配置测试；
- 教师视图不修改完整档案测试；
- K100/K200活动档案容量测试；
- ND0与未挂观察器的2000 FE行为等价测试；
- Java 8构建和既有V35回归。

这些是测试，不是训练实验，不得产出算法优越性结论。

## 4. 将来运行顺序

必须严格按Gate A、B、C、D推进。Gate A未触发预注册问题门时，后续臂不运行；Gate B未证明
教师视图有必要时，Gate C不运行；任何活动档案要转正必须通过DOE迁移和held-out确认。

未来训练机每个物理运行都必须使用独立JVM、独立Problem、同seed同初群，并保存实际FE、
decoder calls、机制事件、完整decision/observed前沿和证据SHA。未经用户再次批准，不得上传、
启动或恢复任何相关任务。

## 5. 论文表述

当前可以写：

> 为避免将解集规模与搜索机制混淆，研究区分完整决策前沿、被动发现前沿和仅用于可视化的
> 固定基数代表前沿；主性能指标始终基于完整决策前沿。

当前不能写：

- 有界档案优于无界档案；
- 教师视图提高算法性能；
- K30是算法返回前沿；
- A4优势来自输出点更多；
- 档案改造无需重新确认DOE。

