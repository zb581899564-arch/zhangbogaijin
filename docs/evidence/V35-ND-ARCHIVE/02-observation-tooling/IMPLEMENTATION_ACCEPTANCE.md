# V35 非支配档案工作包本地实现验收

## 1. 验收结论

本工作包已完成文档、纯观察审计工具、确定性代表集和五个预注册候选臂的本地实现。
当前正式算法语义没有改变；ND1--ND4没有上传训练机、没有运行、没有得到质量结论。

```text
nd_archive_audit_tooling_implemented=true
representative_front_protocol_implemented=true
archive_candidate_arms_compiled=true
archive_control_behavior_preserved=true
current_archive_semantics_unchanged=true
pddr_semantics_unchanged=true
doe1_frozen_mixture_unchanged=true
archive_remote_experiments_started=false
archive_candidate_promoted=false
formal_matrix_started_historically=true
formal_matrix_running=false
formal_matrix_paused=true
```

## 2. 已实现边界

- ND0继续使用完整无界活动档案；精确去重后的`decision-front`与`observed-full-front`不一致时立即失败。
- ND1/ND2仅缩小Qg的CURRENT候选视图，previous/historical缓存、Qp、PDDR、CFVF与CA-TA保持原语义。
- ND3/ND4只在专用实验Runner中裁剪活动档案；被动`observed-full-front`继续保存全部已评价候选的严格非支配集合。
- K30只可用于展示；K25/K50只可用于敏感性。`V35FrontKind`和`V35ArchiveMetricInputGate`禁止它们进入PFref或主指标。
- 审计账本记录档案更新、拒绝原因、裁剪、教师来源/缓存、方向遗憾、FE检查点近重复率、扫描/复制/构造/选择耗时和有界滚动哈希。
- 正式`V35ProductionConfiguration`和正式Runner不暴露archive mode；候选只能由`V35ArchiveExperimentRunner`创建。

## 3. 行为与构建证据

| 门 | 结果 |
|---|---|
| 档案专项测试 | 15/15通过 |
| 当前V35主线定向回归 | 84/84通过 |
| Problem定向回归 | 67/67通过 |
| jMetal core完整旧回归 | 651项中648通过，3项为P1已登记旧错误 |
| 多模块`package -DskipTests` | 通过 |
| `V35ArchiveExperimentRunner.class` | major version 52 |
| 冻结正式Jar | SHA-256仍为`8DAD8F...8B9`，未重建、未覆盖 |

ND0 2000 FE等价测试比较初始种群哈希、实际FE、最终前沿以及机制核心中的P6、PDDR、Qg/Qp、
CA-TA和Q表事件哈希；观察开关没有改变这些搜索结果。ND0还验证decision/observed精确去重集合相等。

## 4. 历史回归债务隔离

全量历史算法回归抽查到247项、5项失败，均不是本工作包新增：

1. `V35Fc0PrefinalArchiveTest`与`V35Fc2LocalFePacingTest`仍钉住pressure-era配置哈希；当前主线已是FC6语义。
2. `V35P101TeacherPoolVerificationTest`的历史冻结前沿比当前重放多一个点，不能通过排序或改证据伪造一致。
3. `V35P241FreezeRevisionTest`与`V35P24FreezeCaptureTest`仍要求旧pressure语义，而磁盘冻结物已经记录FC6语义。

这些是旧证据重放债务，未改写其期望值。本工作包新增/修改的档案测试失败数为0。

## 5. 当前科学结论

当前只能说：完整前沿继续作为科学结果；K30绘图协议和休眠候选实现已准备好。不能说有界档案、
教师视图或K30提高了算法性能。未来只有在用户单独批准Gate A后，才允许生成首批远端数据。
