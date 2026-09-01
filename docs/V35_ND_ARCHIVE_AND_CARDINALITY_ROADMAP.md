# V35 非支配档案与前沿基数路线图

状态：`IMPLEMENTED_LOCALLY / REMOTE_EXPERIMENTS_NOT_STARTED`  
日期：`2026-08-24`

## 1. 当前裁决

本路线不修改当前论文主线。正式语义继续冻结为：

```text
FM3
ShiftMode=NONE
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
subswarm mixture=[20,40,20,20]
PDDR=GLOBAL_ORIGINAL
local-search order=CA-TA-Lite -> inherited LS
active archive=UNBOUNDED_FULL
directional teacher pool=OFF
```

当前证据只支持以下谨慎结论：A4最终非支配点较多，但尚无证据证明这些点主要由精确重复、
近重复或错误目标槽造成；当前统一参考集下的HV/IGD改善也不能仅由“输出点多”解释。因此：

- 不修改PDDR；
- 不裁剪正式活动档案；
- 科学评价使用完整前沿；
- 论文绘图另取确定性代表集；
- 会改变搜索的方案仅作为休眠实验臂；
- DOE1冻结的`20/40/20/20`不因本工作包重做。

这不是“档案方案已经更优”的结论，而是“先把测量、展示和搜索语义拆开”的治理决定。

## 2. 为什么A4可能产生更多非支配点

A4同时包含DSCR、CFVF、谱系个人档案/Qp、双Q及预算感知CA-TA-Lite。它会从不同方向产生并
保留更多折中解；严格三目标支配下，只要一个点在Cmax、TEC、TWC间形成真实权衡，就不能仅因
靠得近而删除。前沿点多可能来自以下正常机制：

1. CFVF在四向量空间产生不同资源组合；
2. 四个语义子群分别强化Cmax、TEC、TWC和平衡方向；
3. CA-TA-Lite产生局部但不互相支配的折中点；
4. 无界全局档案保留整个运行期间发现的严格非支配历史；
5. 连续目标值使“很接近”不等于“完全重复”。

需要排除的异常解释则包括：精确重复未去除、目标槽误读、未评价候选入档、同一候选重复评价、
只因某算法输出点多而使自身reference占优，以及PDDR/档案生命周期丢失。上述问题必须通过本
路线的纯观察账本和统一reference敏感性检查回答，而不是直接改算法。

## 3. 四类前沿对象

| 对象 | 定义 | 是否参与搜索 | 是否参与主指标 | 论文用途 |
|---|---|---:|---:|---|
| `decision-front` | 算法`getResult()`返回的活动决策档案 | 是 | 是 | 完整科学结果 |
| `observed-full-front` | 被动观察全部已评价候选所得严格非支配集合 | 否 | 仅审计 | 候选生命周期核验 |
| `representative-front-k30` | 完整前沿的确定性30点子集 | 否 | 否 | 绘图、甘特图选例 |
| `sensitivity-front-k25/k50` | 等基数25/50点子集 | 否 | 仅敏感性 | 检查基数效应 |

控制臂首先执行：

```text
exactDedup(decision-front) = exactDedup(observed-full-front)
```

不相等时必须停止并调查候选生命周期，不能静默把论文指标来源改成被动档案。

## 4. 已实现的休眠臂

| 标签 | 搜索行为 | 当前地位 |
|---|---|---|
| `ND0_FULL_ARCHIVE_CONTROL` | 无界活动档案；仅挂观察账本 | 本地等价控制 |
| `ND1_TEACHER_VIEW_K50` | 活动档案不变；Qg动作2只读取K50视图 | 休眠 |
| `ND2_TEACHER_VIEW_K25` | 活动档案不变；Qg动作2只读取K25视图 | 休眠 |
| `ND3_ACTIVE_ARCHIVE_K200` | 搜索活动档案上限200；被动前沿仍完整 | 休眠 |
| `ND4_ACTIVE_ARCHIVE_K100` | 搜索活动档案上限100；被动前沿仍完整 | 休眠 |

正式Runner不暴露这些模式；只有`V35ArchiveExperimentRunner`可创建它们。默认路径不安装实验
runtime，因此正式行为仍为`UNBOUNDED_FULL`。

## 5. 分阶段路线

### Gate A：纯观察

未来另行批准后才可运行：I1的5000 FE单seed，以及`20_2_3_1`的20000 FE三seed，均为ND0。
下列任一门触发才允许申请Gate B/C：

- 归一化0.1%近重复率中位数大于20%；
- 档案与教师扫描耗时超过总时间25%；
- 教师方向遗憾中位数大于5%或P95大于20%；
- K25/K50敏感性使A4相对A0的HV/IGD结论反转；
- leave-one-run或leave-one-algorithm-out reference导致主要排序反转；
- PDDR最终Cmax损失中位数达到2%，且丢失解不在任何档案、仍严格非支配且无HV/IGD补偿。

全部未触发时，直接维持无界档案，不运行ND1至ND4，也不重做DOE。

### Gate B：教师视图

仅比较ND0、ND1、ND2；固定`20_2_3_1`、三个配对seed、50000 FE。教师视图必须在扫描成本或
方向遗憾上有可测收益，同时HV、IGD、Cmax不越门，才可申请规模验证。

### Gate C：有界活动档案

只有教师视图不能解决已确认问题时才比较ND0、ND3、ND4。先50k，必要时100k；禁止直接500k。
活动档案必须满足质量门、无单seed灾难、被动完整前沿无系统性优势缺口，并实际降低时间或内存。

### Gate D：DOE迁移

任何改变搜索语义的候选要升级，必须在四种配比
`20/40/20/20`、`30/50/10/10`、`25/25/25/25`、`20/40/30/10`上做小型配对交互。
若最佳配比改变且改善超过2%、交互范围超过2个百分点、排序明显反转或触发HV/IGD灾难门，
必须重做完整15-treatment DOE；否则仍需独立held-out确认后才能变更档案语义。

## 6. 当前完成边界

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
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

`archive_remote_experiments_started=false`只描述本档案路线没有启动远端实验；Stage2正式矩阵
曾启动后暂停的事实仍按D-091保留，不能改写。
