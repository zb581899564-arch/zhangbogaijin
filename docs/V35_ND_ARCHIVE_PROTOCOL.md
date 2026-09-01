# V35 非支配档案与双层前沿协议

协议版本：`v35-nd-archive-protocol-v1`  
算法实验配置版本：`v35-archive-experiment-v1`

## 1. 目标与不可变边界

本协议把“搜索用档案”“所有发现”“论文展示”和“基数敏感性”分开。正式A4仍使用完整无界档案，
PDDR仍为`GLOBAL_ORIGINAL`，三目标固定读取七槽中的`[0,1,6]`。

K30不得进入搜索、PFref、HV、IGD或C-metric；K25/K50只用于等基数敏感性。任何报告若未写清
输入是哪一类前沿，均不具备论文引用资格。

## 2. 精确去重与严格支配

目标点身份使用三个`double`的稳定十六进制表示：

```text
Double.toHexString(Cmax)|Double.toHexString(TEC)|Double.toHexString(TWC)
```

精确相同的三目标点只保留稳定指纹最小的解。严格支配定义为三个目标均不差且至少一个严格更好；
完全相同目标互不严格支配，但在代表集前先精确去重。

## 3. 确定性maximin子集

`V35DeterministicObjectiveSubsetter`执行：

1. 输入按三目标与稳定解指纹排序；
2. 精确去重；
3. 分别保留最小Cmax、最小TEC、最小TWC点；
4. 按当前集合min/max归一化，退化范围取`1e-12`；
5. 反复选择到已选集合最小欧氏距离最大的点；
6. 距离并列时按三目标、再按稳定指纹破平；
7. 返回稳定排序的防御性副本。

该算法不生成随机数，输入顺序反转不得改变输出字节。注册容量为25、30、50、100、200。

## 4. 搜索档案协议

### `UNBOUNDED_FULL`

当前正式语义。新候选先执行严格三目标增量Pareto更新；精确重复和被支配候选拒绝，候选严格支配
的旧成员移除。没有容量裁剪。

### `BOUNDED_TEACHER_VIEW`

完整活动档案不变。只有Qg从当前社会候选集合执行动作2时，读取确定性K点视图；Qg动作0/1的
previous/historical缓存仍由DSCR按现有规则清洗。Qp、PDDR、CFVF、CA-TA、FE和完整科学前沿不变。

### `BOUNDED_ACTIVE_ARCHIVE`

先执行与正式档案相同的严格Pareto更新；成员超过K时才执行极值保留与maximin裁剪。裁剪后的
档案用于搜索和`decision-front`，但独立被动观察器继续保存`observed-full-front`。

## 5. 观察账本

观察器至少导出：

```text
FE, generation
beforeDecisionSize, afterDecisionSize, observedFullSize
candidateAdds, dominatedRejects, equalObjectiveRejects, removedDominated
activeArchivePruned
archiveCopyItems, archiveScanCalls, archiveItemsVisited
archiveUpdateNanos, teacherSelectionNanos, peakHeapUsedBytes
Qg action, group, viewSize, selectedFingerprint
bestEligibleDirectionalScore, selectedDirectionalScore, directionalRegret, teacherExposure
```

运行结束时另计算完整决策前沿的归一化最近邻距离和0.01%/0.05%/0.1%近重复率。墙钟与堆内存
只用于诊断，不能参与搜索决策。

## 6. 指标与reference

- 主指标只读取所有已完成、reference-eligible运行的完整`decision-front`；
- 必须等所有纳入运行完成后一次冻结PFref和归一化边界；
- 单个算法不得使用自己的代表集构造reference；
- `observed-full-front`仅用于审计活动档案是否丢失发现；
- K25/K50按相同基数重算敏感性指标时必须单独标注，不能覆盖主指标；
- K30只用于图形抽样。

## 7. Fail-closed规则

以下任一情况立即停止对应实验：

- ND0中精确去重后的decision与observed前沿不相等；
- 观察开关改变初群、动作/评价事件、Q表、FE或最终前沿；
- 活动档案出现被支配成员或超过容量；
- 代表集进入算法、PDDR、教师缓存或reference；
- 正式Runner接受ND1至ND4；
- 配置/源码/输入哈希不一致；
- 未完成全部运行就构造统一reference。

